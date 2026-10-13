package com.DeviceManage.deviceManage.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Състояние и трафик на един мрежов интерфейс в даден момент (IF-MIB).
 */
@Entity
@Table(name = "interface_samples",
        indexes = @Index(name = "idx_if_device_time", columnList = "device_id, collected_at"))
public class InterfaceSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(nullable = false)
    private LocalDateTime collectedAt;

    private int ifIndex;        // номер на интерфейса в устройството
    private String name;        // ifDescr, например "GigabitEthernet0/1"
    private boolean operUp;     // ifOperStatus = up(1)
    private Long inOctets;      // брояч на входящите байтове
    private Long outOctets;     // брояч на изходящите байтове
    private Double inBps;       // входящ трафик в бит/с (от разликата с предишното измерване)
    private Double outBps;      // изходящ трафик в бит/с

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }

    public int getIfIndex() { return ifIndex; }
    public void setIfIndex(int ifIndex) { this.ifIndex = ifIndex; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isOperUp() { return operUp; }
    public void setOperUp(boolean operUp) { this.operUp = operUp; }

    public Long getInOctets() { return inOctets; }
    public void setInOctets(Long inOctets) { this.inOctets = inOctets; }

    public Long getOutOctets() { return outOctets; }
    public void setOutOctets(Long outOctets) { this.outOctets = outOctets; }

    public Double getInBps() { return inBps; }
    public void setInBps(Double inBps) { this.inBps = inBps; }

    public Double getOutBps() { return outBps; }
    public void setOutBps(Double outBps) { this.outBps = outBps; }
}
