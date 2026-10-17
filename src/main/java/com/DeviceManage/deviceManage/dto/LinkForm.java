package com.DeviceManage.deviceManage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class LinkForm {

    @NotNull(message = "Изберете устройство")
    private Long deviceAId;

    @Min(value = 1, message = "Номерът на интерфейса е положително число")
    private Integer ifIndexA;

    @NotNull(message = "Изберете устройство")
    private Long deviceBId;

    @Min(value = 1, message = "Номерът на интерфейса е положително число")
    private Integer ifIndexB;

    @Positive(message = "Капацитетът трябва да е положително число")
    private Double capacityMbps;

    @Size(max = 255)
    private String description;

    public Long getDeviceAId() { return deviceAId; }
    public void setDeviceAId(Long deviceAId) { this.deviceAId = deviceAId; }

    public Integer getIfIndexA() { return ifIndexA; }
    public void setIfIndexA(Integer ifIndexA) { this.ifIndexA = ifIndexA; }

    public Long getDeviceBId() { return deviceBId; }
    public void setDeviceBId(Long deviceBId) { this.deviceBId = deviceBId; }

    public Integer getIfIndexB() { return ifIndexB; }
    public void setIfIndexB(Integer ifIndexB) { this.ifIndexB = ifIndexB; }

    public Double getCapacityMbps() { return capacityMbps; }
    public void setCapacityMbps(Double capacityMbps) { this.capacityMbps = capacityMbps; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
