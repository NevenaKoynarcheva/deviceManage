package com.DeviceManage.deviceManage.dto;

/** Връзка с изчислено текущо състояние и натоварване - за таблицата и картата. */
public record LinkView(Long id,
                       Long deviceAId, String deviceAName, String interfaceA,
                       Long deviceBId, String deviceBName, String interfaceB,
                       String description, String status,
                       Double trafficMbps, Double capacityMbps, Double loadPercent) {
}
