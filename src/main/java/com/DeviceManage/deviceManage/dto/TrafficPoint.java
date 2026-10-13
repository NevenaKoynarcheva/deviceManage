package com.DeviceManage.deviceManage.dto;

import com.DeviceManage.deviceManage.model.InterfaceSample;
import java.time.LocalDateTime;

public record TrafficPoint(LocalDateTime time, boolean operUp, Double inBps, Double outBps) {

    public static TrafficPoint from(InterfaceSample s) {
        return new TrafficPoint(s.getCollectedAt(), s.isOperUp(), s.getInBps(), s.getOutBps());
    }
}
