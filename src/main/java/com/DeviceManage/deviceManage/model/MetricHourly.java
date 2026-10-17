package com.DeviceManage.deviceManage.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Агрегирани (обобщени) измервания за един час.
 * Подробните измервания се пазят само няколко дни; по-старите се обобщават
 * в средни стойности по часове и се изтриват, за да не расте базата безкрайно.
 */
@Entity
@Table(name = "metric_hourly",
        uniqueConstraints = @UniqueConstraint(name = "uk_hourly_device_hour", columnNames = {"device_id", "hour_start"}))
public class MetricHourly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "hour_start", nullable = false)
    private LocalDateTime hourStart;            // началото на часа, напр. 14:00

    @Column(name = "samples")
    private Integer samples;                    // от колко измервания е обобщението

    @Column(name = "availability_percent")
    private Double availabilityPercent;         // % от проверките, в които е отговорило

    @Column(name = "avg_ping_ms")
    private Double avgPingMs;

    @Column(name = "avg_packet_loss")
    private Double avgPacketLoss;

    @Column(name = "avg_cpu")
    private Double avgCpu;

    @Column(name = "avg_memory")
    private Double avgMemory;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public LocalDateTime getHourStart() { return hourStart; }
    public void setHourStart(LocalDateTime hourStart) { this.hourStart = hourStart; }

    public Integer getSamples() { return samples; }
    public void setSamples(Integer samples) { this.samples = samples; }

    public Double getAvailabilityPercent() { return availabilityPercent; }
    public void setAvailabilityPercent(Double availabilityPercent) { this.availabilityPercent = availabilityPercent; }

    public Double getAvgPingMs() { return avgPingMs; }
    public void setAvgPingMs(Double avgPingMs) { this.avgPingMs = avgPingMs; }

    public Double getAvgPacketLoss() { return avgPacketLoss; }
    public void setAvgPacketLoss(Double avgPacketLoss) { this.avgPacketLoss = avgPacketLoss; }

    public Double getAvgCpu() { return avgCpu; }
    public void setAvgCpu(Double avgCpu) { this.avgCpu = avgCpu; }

    public Double getAvgMemory() { return avgMemory; }
    public void setAvgMemory(Double avgMemory) { this.avgMemory = avgMemory; }
}
