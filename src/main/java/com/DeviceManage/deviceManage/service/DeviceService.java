package com.DeviceManage.deviceManage.service;

import com.DeviceManage.deviceManage.dto.DeviceForm;
import com.DeviceManage.deviceManage.dto.MetricPoint;
import com.DeviceManage.deviceManage.dto.TrafficPoint;
import com.DeviceManage.deviceManage.model.*;
import com.DeviceManage.deviceManage.repository.*;
import com.DeviceManage.deviceManage.snmp.PingClient;
import com.DeviceManage.deviceManage.snmp.SnmpClient;
import com.DeviceManage.deviceManage.snmp.profile.VendorProfile;
import com.DeviceManage.deviceManage.snmp.profile.VendorProfileResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);
    private static final String OID_SYS_UPTIME = "1.3.6.1.2.1.1.3.0";
    private static final String OID_SYS_OBJECT_ID = "1.3.6.1.2.1.1.2.0";

    // IF-MIB: интерфейси и броячи на трафика
    private static final String OID_IF_DESCR = "1.3.6.1.2.1.2.2.1.2";
    private static final String OID_IF_OPER_STATUS = "1.3.6.1.2.1.2.2.1.8";
    private static final String OID_IF_HC_IN_OCTETS = "1.3.6.1.2.1.31.1.1.1.6";   // 64-битови броячи
    private static final String OID_IF_HC_OUT_OCTETS = "1.3.6.1.2.1.31.1.1.1.10";
    private static final String OID_IF_IN_OCTETS = "1.3.6.1.2.1.2.2.1.10";        // 32-битови (резервен вариант)
    private static final String OID_IF_OUT_OCTETS = "1.3.6.1.2.1.2.2.1.16";

    private final DeviceRepository deviceRepository;
    private final MetricSampleRepository metricSampleRepository;
    private final InterfaceSampleRepository interfaceSampleRepository;
    private final ChangeLogRepository changeLogRepository;
    private final MetricHourlyRepository metricHourlyRepository;
    private final SnmpClient snmpClient;

    private final PingClient pingClient;
    private final VendorProfileResolver vendorProfiles;

    public DeviceService(DeviceRepository deviceRepository,
                         MetricSampleRepository metricSampleRepository,
                         InterfaceSampleRepository interfaceSampleRepository,
                         ChangeLogRepository changeLogRepository,
                         MetricHourlyRepository metricHourlyRepository,
                         SnmpClient snmpClient,
                         PingClient pingClient,
                         VendorProfileResolver vendorProfiles) {
        this.deviceRepository = deviceRepository;
        this.metricSampleRepository = metricSampleRepository;
        this.interfaceSampleRepository = interfaceSampleRepository;
        this.changeLogRepository = changeLogRepository;
        this.metricHourlyRepository = metricHourlyRepository;
        this.snmpClient = snmpClient;
        this.pingClient = pingClient;
        this.vendorProfiles = vendorProfiles;
    }


    @Scheduled(fixedDelayString = "${nms.polling.interval-ms:30000}")
    public void pollAllDevices() {
        for (Device device : deviceRepository.findAll()) {
            try {
                pollDevice(device);
            } catch (Exception e) {
                // Грешка при едно устройство не спира останалите
                log.warn("Грешка при проверка на {}: {}", device.getIpAddress(), e.getMessage());
            }
        }
    }
    public Device findById(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Устройството не е намерено"));
    }

    public Optional<MetricSample> findLatestSample(Long deviceId) {
        return metricSampleRepository.findTopByDeviceIdOrderByCollectedAtDesc(deviceId);
    }

    public List<MetricPoint> getMetrics(Long deviceId, int hours) {
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hours);
        // Старата част на периода идва от часовите обобщения, новата - от подробните измервания.
        // Двете не се застъпват, защото подробните редове се изтриват след агрегирането.
        List<MetricPoint> points = new ArrayList<>();
        metricHourlyRepository.findByDeviceIdAndHourStartBetweenOrderByHourStartAsc(deviceId, from, to)
                .forEach(h -> points.add(MetricPoint.from(h)));
        metricSampleRepository.findByDeviceIdAndCollectedAtBetweenOrderByCollectedAtAsc(deviceId, from, to)
                .forEach(s -> points.add(MetricPoint.from(s)));
        return points;
    }
    public void pollDevice(Device device) throws IOException {
        MetricSample sample = new MetricSample();
        sample.setDevice(device);
        sample.setCollectedAt(LocalDateTime.now());

        // 1) ICMP ping - работи за всяко устройство
        PingClient.PingResult ping = pingClient.ping(device.getIpAddress(), 4, 1000);
        sample.setPingLatencyMs(ping.avgLatencyMs());
        sample.setPacketLossPercent(ping.packetLossPercent());

        // 2) SNMP - само ако устройството има read community
        boolean snmpOk = false;
        if (device.getReadCommunity() != null && !device.getReadCommunity().isBlank()) {
            long start = System.currentTimeMillis();
            Variable uptime = snmpClient.getVariable(device.getIpAddress(), device.getSnmpPort(),
                    device.getReadCommunity(), OID_SYS_UPTIME);
            if (uptime != null) {
                snmpOk = true;
                sample.setResponseTimeMs(System.currentTimeMillis() - start);
                sample.setUptimeSeconds(uptime.toLong() / 100);
            }
        }

        // 3) CPU и памет - според профила на производителя
        if (snmpOk) {
            collectResources(device, sample);
        }

        // 4) Интерфейси и трафик
        if (snmpOk) {
            try {
                collectInterfaces(device, sample.getCollectedAt());
            } catch (Exception e) {
                log.warn("{}: интерфейсите не са прочетени: {}", device.getName(), e.getMessage());
            }
        }

        boolean up = ping.received() > 0 || snmpOk;
        sample.setReachable(up);
        device.setStatus(up ? DeviceStatus.UP : DeviceStatus.DOWN);
        if (up) {
            device.setLastSeen(sample.getCollectedAt());
        }

        metricSampleRepository.save(sample);
        deviceRepository.save(device);
        log.info("{} ({}) -> {} | ping {} ms, загуба {}% | SNMP {} | CPU {}%, памет {}%",
                device.getName(), device.getIpAddress(), device.getStatus(),
                ping.avgLatencyMs(), ping.packetLossPercent(), snmpOk ? "OK" : "-",
                sample.getCpuLoad(), sample.getMemoryUsage());
    }

    /** Интерфейсите от последната проверка: първо активните, после по номер. */
    public List<InterfaceSample> findLatestInterfaces(Long deviceId) {
        return interfaceSampleRepository.findTopByDeviceIdOrderByCollectedAtDesc(deviceId)
                .map(last -> interfaceSampleRepository.findByDeviceIdAndCollectedAt(deviceId, last.getCollectedAt()))
                .orElse(List.of())
                .stream()
                .sorted(Comparator.comparing(InterfaceSample::isOperUp).reversed()
                        .thenComparingInt(InterfaceSample::getIfIndex))
                .toList();
    }

    public List<TrafficPoint> getTraffic(Long deviceId, int ifIndex, int hours) {
        LocalDateTime to = LocalDateTime.now();
        return interfaceSampleRepository
                .findByDeviceIdAndIfIndexAndCollectedAtBetweenOrderByCollectedAtAsc(deviceId, ifIndex, to.minusHours(hours), to)
                .stream().map(TrafficPoint::from).toList();
    }

    /**
     * Чете всички интерфейси на устройството (IF-MIB) и записва по един ред за всеки.
     * SNMP дава само броячи на байтовете, затова скоростта се смята като
     * разлика с предишното измерване: (байтове сега - байтове преди) * 8 / секунди.
     */
    private void collectInterfaces(Device device, LocalDateTime now) throws IOException {
        String ip = device.getIpAddress();
        int port = device.getSnmpPort();
        String community = device.getReadCommunity();

        Map<String, Variable> statuses = snmpClient.walk(ip, port, community, OID_IF_OPER_STATUS);
        if (statuses.isEmpty()) {
            return; // устройството не дава информация за интерфейси
        }
        Map<String, Variable> names = snmpClient.walk(ip, port, community, OID_IF_DESCR);
        Map<String, Variable> in = snmpClient.walk(ip, port, community, OID_IF_HC_IN_OCTETS);
        Map<String, Variable> out = snmpClient.walk(ip, port, community, OID_IF_HC_OUT_OCTETS);
        if (in.isEmpty()) {
            in = snmpClient.walk(ip, port, community, OID_IF_IN_OCTETS);
            out = snmpClient.walk(ip, port, community, OID_IF_OUT_OCTETS);
        }

        // Предишните стойности по номер на интерфейс
        Map<Integer, InterfaceSample> previous = new HashMap<>();
        for (InterfaceSample p : findLatestInterfaces(device.getId())) {
            previous.put(p.getIfIndex(), p);
        }

        List<InterfaceSample> samples = new ArrayList<>();
        for (Map.Entry<String, Variable> entry : statuses.entrySet()) {
            String key = entry.getKey();
            InterfaceSample s = new InterfaceSample();
            s.setDevice(device);
            s.setCollectedAt(now);
            s.setIfIndex(Integer.parseInt(key));
            s.setName(cleanName(names.get(key), key));
            s.setOperUp(entry.getValue().toInt() == 1); // 1 = up, 2 = down
            s.setInOctets(in.containsKey(key) ? in.get(key).toLong() : null);
            s.setOutOctets(out.containsKey(key) ? out.get(key).toLong() : null);

            InterfaceSample prev = previous.get(s.getIfIndex());
            if (prev != null) {
                double seconds = Duration.between(prev.getCollectedAt(), now).toMillis() / 1000.0;
                s.setInBps(rate(prev.getInOctets(), s.getInOctets(), seconds));
                s.setOutBps(rate(prev.getOutOctets(), s.getOutOctets(), seconds));
            }
            samples.add(s);
        }
        interfaceSampleRepository.saveAll(samples);
    }

    private static Double rate(Long before, Long now, double seconds) {
        if (before == null || now == null || seconds <= 0 || now < before) {
            return null; // няма предишна стойност или броячът е нулиран
        }
        return Math.round((now - before) * 8 / seconds * 10) / 10.0;
    }

    /** Някои устройства връщат името с нулеви байтове накрая - чистим ги. */
    private static String cleanName(Variable v, String ifIndex) {
        if (v == null) {
            return "Интерфейс " + ifIndex;
        }
        String text = v instanceof OctetString os
                ? new String(os.getValue(), StandardCharsets.UTF_8)
                : v.toString();
        text = text.replace("\0", "").trim();
        return text.isEmpty() ? "Интерфейс " + ifIndex : text;
    }

    /**
     * Чете CPU и памет чрез профила на производителя.
     * При първата успешна SNMP проверка производителят се разпознава автоматично
     * по sysObjectID и се записва в устройството, за да не се пита всеки път.
     */
    private void collectResources(Device device, MetricSample sample) {
        try {
            if (device.getVendor() == null || device.getVendor().isBlank()) {
                String sysObjectId = snmpClient.get(device.getIpAddress(), device.getSnmpPort(),
                        device.getReadCommunity(), OID_SYS_OBJECT_ID);
                VendorProfile detected = vendorProfiles.detect(sysObjectId);
                device.setVendor(detected.name());
                log.info("{}: sysObjectID {} -> профил {}", device.getName(), sysObjectId, detected.name());
            }
        } catch (Exception e) {
            log.warn("{}: неуспешно разпознаване на производителя: {}", device.getName(), e.getMessage());
        }

        VendorProfile profile = vendorProfiles.byName(device.getVendor());
        // CPU и паметта се четат поотделно - ако едното липсва, другото пак се записва
        try {
            sample.setCpuLoad(profile.readCpuLoad(snmpClient, device));
        } catch (Exception e) {
            log.debug("{}: CPU не е прочетен: {}", device.getName(), e.getMessage());
        }
        try {
            sample.setMemoryUsage(profile.readMemoryUsage(snmpClient, device));
        } catch (Exception e) {
            log.debug("{}: паметта не е прочетена: {}", device.getName(), e.getMessage());
        }
    }

    public List<Device> findAll() {
        return deviceRepository.findAll(Sort.by("name"));
    }

    public Device addDevice(DeviceForm form) {
        if (deviceRepository.existsByIpAddress(form.getIpAddress())) {
            throw new IllegalArgumentException("Устройство с този IP адрес вече съществува");
        }
        Device d = new Device();
        applyForm(form, d);
        d.setWriteCommunity(blankToNull(form.getWriteCommunity()));
        Device saved = deviceRepository.save(d);

        // Първа проверка веднага, за да не чакаме 30 секунди за статуса
        try {
            pollDevice(saved);
        } catch (Exception e) {
            log.warn("Първата проверка на {} неуспешна: {}", saved.getIpAddress(), e.getMessage());
        }
        return saved;
    }

    public Device updateDevice(Long id, DeviceForm form) {
        Device d = findById(id);
        String ip = form.getIpAddress().trim();
        // IP адресът трябва да остане уникален (без да броим самото устройство)
        deviceRepository.findByIpAddress(ip)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException("Устройство с този IP адрес вече съществува");
                });
        applyForm(form, d);
        // Празно поле = запазва се старата write community
        if (form.getWriteCommunity() != null && !form.getWriteCommunity().isBlank()) {
            d.setWriteCommunity(form.getWriteCommunity());
        }
        d.setVendor(null); // производителят ще се разпознае наново при следващата проверка
        Device saved = deviceRepository.save(d);
        try {
            pollDevice(saved);
        } catch (Exception e) {
            log.warn("Проверката на {} след редакция неуспешна: {}", saved.getIpAddress(), e.getMessage());
        }
        return saved;
    }

    /** Изтрива устройството заедно с цялата му история. */
    @Transactional
    public void deleteDevice(Long id) {
        Device d = findById(id);
        changeLogRepository.deleteAllByDeviceId(id);
        metricHourlyRepository.deleteAllByDeviceId(id);
        interfaceSampleRepository.deleteAllByDeviceId(id);
        metricSampleRepository.deleteAllByDeviceId(id);
        deviceRepository.delete(d);
        log.info("Изтрито устройство {} ({})", d.getName(), d.getIpAddress());
    }

    private void applyForm(DeviceForm form, Device d) {
        d.setName(form.getName().trim());
        d.setIpAddress(form.getIpAddress().trim());
        d.setType(form.getType());
        d.setSnmpVersion(form.getSnmpVersion());
        d.setSnmpPort(form.getSnmpPort());
        d.setReadCommunity(blankToNull(form.getReadCommunity()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
