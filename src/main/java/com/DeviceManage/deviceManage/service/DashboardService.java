package com.DeviceManage.deviceManage.service;

import com.DeviceManage.deviceManage.dto.DeviceOverview;
import com.DeviceManage.deviceManage.model.Device;
import com.DeviceManage.deviceManage.model.DeviceStatus;
import com.DeviceManage.deviceManage.model.MetricSample;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Обобщено състояние на мрежата за началния екран.
 * Проблем = устройството е недостъпно или някой показател е над прага.
 * Праговете се задават в application.properties.
 */
@Service
public class DashboardService {

    private final DeviceService deviceService;

    @Value("${nms.threshold.cpu:80}")
    private double cpuThreshold;

    @Value("${nms.threshold.memory:90}")
    private double memoryThreshold;

    @Value("${nms.threshold.latency-ms:100}")
    private double latencyThreshold;

    @Value("${nms.threshold.packet-loss:0}")
    private double packetLossThreshold;

    public DashboardService(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public List<DeviceOverview> overview() {
        List<DeviceOverview> result = new ArrayList<>();
        for (Device d : deviceService.findAll()) {
            MetricSample latest = deviceService.findLatestSample(d.getId()).orElse(null);
            result.add(new DeviceOverview(d, latest, findProblems(d, latest)));
        }
        return result;
    }

    private List<String> findProblems(Device d, MetricSample s) {
        List<String> problems = new ArrayList<>();
        if (d.getStatus() == DeviceStatus.DOWN) {
            problems.add("Устройството не отговаря");
            return problems; // останалите показатели нямат смисъл, щом е недостъпно
        }
        if (s == null) {
            return problems;
        }
        if (s.getPacketLossPercent() != null && s.getPacketLossPercent() > packetLossThreshold) {
            problems.add(String.format("Загуба на пакети %.0f %%", s.getPacketLossPercent()));
        }
        if (s.getPingLatencyMs() != null && s.getPingLatencyMs() > latencyThreshold) {
            problems.add(String.format("Високо закъснение %.0f ms", s.getPingLatencyMs()));
        }
        if (s.getCpuLoad() != null && s.getCpuLoad() > cpuThreshold) {
            problems.add(String.format("Натоварен процесор %.0f %%", s.getCpuLoad()));
        }
        if (s.getMemoryUsage() != null && s.getMemoryUsage() > memoryThreshold) {
            problems.add(String.format("Заета памет %.0f %%", s.getMemoryUsage()));
        }
        return problems;
    }
}
