package com.DeviceManage.deviceManage.dto;

import com.DeviceManage.deviceManage.model.Device;
import com.DeviceManage.deviceManage.model.DeviceType;
import com.DeviceManage.deviceManage.model.SnmpVersion;
import jakarta.validation.constraints.*;

public class DeviceForm {

    @NotBlank(message = "Въведете име")
    private String name;

    @NotBlank(message = "Въведете IP адрес")
    @Pattern(regexp = "^((25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1?\\d?\\d)$",
            message = "Невалиден IPv4 адрес")
    private String ipAddress;

    @NotNull
    private DeviceType type = DeviceType.Router;

    @NotNull
    private SnmpVersion snmpVersion = SnmpVersion.V2C;

    @Min(value = 1, message = "Портът трябва да е между 1 и 65535")
    @Max(value = 65535, message = "Портът трябва да е между 1 и 65535")
    private int snmpPort = 161;

    private String readCommunity;

    private String writeCommunity;

    /** Попълва формата от съществуващо устройство (за редакция). Write community не се показва. */
    public static DeviceForm from(Device d) {
        DeviceForm f = new DeviceForm();
        f.setName(d.getName());
        f.setIpAddress(d.getIpAddress());
        f.setType(d.getType());
        f.setSnmpVersion(d.getSnmpVersion());
        f.setSnmpPort(d.getSnmpPort());
        f.setReadCommunity(d.getReadCommunity());
        return f;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

    public SnmpVersion getSnmpVersion() {
        return snmpVersion;
    }

    public void setSnmpVersion(SnmpVersion snmpVersion) {
        this.snmpVersion = snmpVersion;
    }

    public int getSnmpPort() {
        return snmpPort;
    }

    public void setSnmpPort(int snmpPort) {
        this.snmpPort = snmpPort;
    }

    public String getReadCommunity() {
        return readCommunity;
    }

    public void setReadCommunity(String readCommunity) {
        this.readCommunity = readCommunity;
    }

    public String getWriteCommunity() {
        return writeCommunity;
    }

    public void setWriteCommunity(String writeCommunity) {
        this.writeCommunity = writeCommunity;
    }
}