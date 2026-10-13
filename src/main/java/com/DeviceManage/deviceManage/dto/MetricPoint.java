package com.DeviceManage.deviceManage.dto;

import com.DeviceManage.deviceManage.model.MetricSample;
import java.time.LocalDateTime;

public record MetricPoint(LocalDateTime time, boolean reachable, Double pingLatencyMs,
                          Double packetLossPercent, Long responseTimeMs, Long uptimeSeconds) {

    public static MetricPoint from(MetricSample s) {
        return new MetricPoint(s.getCollectedAt(), s.isReachable(), s.getPingLatencyMs(),
                s.getPacketLossPercent(), s.getResponseTimeMs(), s.getUptimeSeconds());
    }
}