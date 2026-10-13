package com.DeviceManage.deviceManage.service;

import com.DeviceManage.deviceManage.dto.DeviceForm;
import com.DeviceManage.deviceManage.dto.MetricPoint;
import com.DeviceManage.deviceManage.model.*;
import com.DeviceManage.deviceManage.repository.*;
import com.DeviceManage.deviceManage.snmp.PingClient;
import com.DeviceManage.deviceManage.snmp.SnmpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.Variable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);
    private static final String OID_SYS_UPTIME = "1.3.6.1.2.1.1.3.0";

    private final DeviceRepository deviceRepository;
    private final MetricSampleRepository metricSampleRepository;
    private final SnmpClient snmpClient;

    private final PingClient pingClient;

    public DeviceService(DeviceRepository deviceRepository,
                         MetricSampleRepository metricSampleRepository,
                         SnmpClient snmpClient,
                         PingClient pingClient) {
        this.deviceRepository = deviceRepository;
        this.metricSampleRepository = metricSampleRepository;
        this.snmpClient = snmpClient;
        this.pingClient = pingClient;
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
        return metricSampleRepository
                .findByDeviceIdAndCollectedAtBetweenOrderByCollectedAtAsc(deviceId, from, to)
                .stream().map(MetricPoint::from).toList();
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

        boolean up = ping.received() > 0 || snmpOk;
        sample.setReachable(up);
        device.setStatus(up ? DeviceStatus.UP : DeviceStatus.DOWN);
        if (up) {
            device.setLastSeen(sample.getCollectedAt());
        }

        metricSampleRepository.save(sample);
        deviceRepository.save(device);
        log.info("{} ({}) -> {} | ping {} ms, загуба {}% | SNMP {}",
                device.getName(), device.getIpAddress(), device.getStatus(),
                ping.avgLatencyMs(), ping.packetLossPercent(), snmpOk ? "OK" : "-");
    }

    public List<Device> findAll() {
        return deviceRepository.findAll(Sort.by("name"));
    }

    public Device addDevice(DeviceForm form) {
        if (deviceRepository.existsByIpAddress(form.getIpAddress())) {
            throw new IllegalArgumentException("Устройство с този IP адрес вече съществува");
        }
        Device d = new Device();
        d.setName(form.getName().trim());
        d.setIpAddress(form.getIpAddress().trim());
        d.setType(form.getType());
        d.setSnmpVersion(form.getSnmpVersion());
        d.setSnmpPort(form.getSnmpPort());
        d.setReadCommunity(form.getReadCommunity() == null || form.getReadCommunity().isBlank()
                ? null : form.getReadCommunity());
        d.setWriteCommunity(form.getWriteCommunity() == null || form.getWriteCommunity().isBlank()
                ? null : form.getWriteCommunity());
        Device saved = deviceRepository.save(d);

        // Първа проверка веднага, за да не чакаме 30 секунди за статуса
        try {
            pollDevice(saved);
        } catch (Exception e) {
            log.warn("Първата проверка на {} неуспешна: {}", saved.getIpAddress(), e.getMessage());
        }
        return saved;
    }
}
