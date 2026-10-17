package com.DeviceManage.deviceManage.model;

import jakarta.persistence.*;

/**
 * Физическа или логическа връзка между два мрежови възела.
 * Интерфейсите са по избор: ако са зададени, състоянието и натоварването
 * на връзката се определят от тях.
 */
@Entity
@Table(name = "links")
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "device_a_id")
    private Device deviceA;

    private Integer ifIndexA;       // интерфейс от страната на A (по избор)

    @ManyToOne(optional = false)
    @JoinColumn(name = "device_b_id")
    private Device deviceB;

    private Integer ifIndexB;       // интерфейс от страната на B (по избор)

    private Double capacityMbps;    // капацитет на връзката, за % натоварване (по избор)

    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Device getDeviceA() { return deviceA; }
    public void setDeviceA(Device deviceA) { this.deviceA = deviceA; }

    public Integer getIfIndexA() { return ifIndexA; }
    public void setIfIndexA(Integer ifIndexA) { this.ifIndexA = ifIndexA; }

    public Device getDeviceB() { return deviceB; }
    public void setDeviceB(Device deviceB) { this.deviceB = deviceB; }

    public Integer getIfIndexB() { return ifIndexB; }
    public void setIfIndexB(Integer ifIndexB) { this.ifIndexB = ifIndexB; }

    public Double getCapacityMbps() { return capacityMbps; }
    public void setCapacityMbps(Double capacityMbps) { this.capacityMbps = capacityMbps; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
