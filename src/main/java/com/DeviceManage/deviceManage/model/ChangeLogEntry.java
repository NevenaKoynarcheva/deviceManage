package com.DeviceManage.deviceManage.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Журнал на управляващите операции: кой, какво и кога е променил
 * и дали промяната е успяла. Записва се и при неуспех.
 */
@Entity
@Table(name = "change_log",
        indexes = @Index(name = "idx_log_device_time", columnList = "device_id, performed_at"))
public class ChangeLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    private String username;      // кой е направил промяната (след добавяне на вход)
    private String operation;     // напр. "Смяна на sysLocation"
    private String oid;           // кой OID е променен
    private String oldValue;
    private String newValue;
    private boolean success;
    private String message;       // съобщение за грешка при неуспех

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getOid() { return oid; }
    public void setOid(String oid) { this.oid = oid; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
