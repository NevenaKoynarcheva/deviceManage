package com.DeviceManage.deviceManage.dto;

import com.DeviceManage.deviceManage.model.MetricHourly;
import com.DeviceManage.deviceManage.model.MetricSample;
import java.time.LocalDateTime;

public record MetricPoint(LocalDateTime time, boolean reachable, Double pingLatencyMs,
                          Double packetLossPercent, Long responseTimeMs, Long uptimeSeconds,
                          Double cpuLoad, Double memoryUsage) {

    public static MetricPoint from(MetricSample s) {
        return new MetricPoint(s.getCollectedAt(), s.isReachable(), s.getPingLatencyMs(),
                s.getPacketLossPercent(), s.getResponseTimeMs(), s.getUptimeSeconds(),
                s.getCpuLoad(), s.getMemoryUsage());
    }

    /** Точка от обобщените часови данни (за периоди, по-стари от няколко дни). */
    public static MetricPoint from(MetricHourly h) {
        boolean reachable = h.getAvailabilityPercent() != null && h.getAvailabilityPercent() > 0;
        return new MetricPoint(h.getHourStart(), reachable, h.getAvgPingMs(),
                h.getAvgPacketLoss(), null, null, h.getAvgCpu(), h.getAvgMemory());
    }
}