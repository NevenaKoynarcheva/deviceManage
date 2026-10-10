package com.DeviceManage.deviceManage.service;

import com.DeviceManage.deviceManage.model.*;
import com.DeviceManage.deviceManage.repository.*;
import com.DeviceManage.deviceManage.snmp.SnmpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.Variable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);
    private static final String OID_SYS_UPTIME = "1.3.6.1.2.1.1.3.0";

    private final DeviceRepository deviceRepository;
    private final MetricSampleRepository metricSampleRepository;
    private final SnmpClient snmpClient;

    public DeviceService(DeviceRepository deviceRepository,
                         MetricSampleRepository metricSampleRepository,
                         SnmpClient snmpClient) {
        this.deviceRepository = deviceRepository;
        this.metricSampleRepository = metricSampleRepository;
        this.snmpClient = snmpClient;
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

    public void pollDevice(Device device) throws IOException {
        MetricSample sample = new MetricSample();
        sample.setDevice(device);
        sample.setCollectedAt(LocalDateTime.now());

        long start = System.currentTimeMillis();
        Variable uptime = snmpClient.getVariable(device.getIpAddress(), device.getSnmpPort(),
                device.getReadCommunity(), OID_SYS_UPTIME);
        long elapsed = System.currentTimeMillis() - start;

        if (uptime != null) {
            sample.setReachable(true);
            sample.setResponseTimeMs(elapsed);
            sample.setUptimeSeconds(uptime.toLong() / 100); // TimeTicks са в стотни от секундата
            device.setStatus(DeviceStatus.UP);
            device.setLastSeen(sample.getCollectedAt());
        } else {
            sample.setReachable(false);
            device.setStatus(DeviceStatus.DOWN);
        }

        metricSampleRepository.save(sample);
        deviceRepository.save(device);
        log.info("{} ({}) -> {} за {} ms", device.getName(), device.getIpAddress(),
                device.getStatus(), sample.getResponseTimeMs());
    }
}
