package com.DeviceManage.deviceManage.dto;

import com.DeviceManage.deviceManage.model.Device;
import com.DeviceManage.deviceManage.model.MetricSample;

import java.util.List;

/** Устройство, последното му измерване и откритите проблеми - за началния екран. */
public record DeviceOverview(Device device, MetricSample latest, List<String> problems) {

    public boolean hasProblems() {
        return !problems.isEmpty();
    }
}
