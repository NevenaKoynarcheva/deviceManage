package com.DeviceManage.deviceManage.service;

import com.DeviceManage.deviceManage.model.ChangeLogEntry;
import com.DeviceManage.deviceManage.model.Device;
import com.DeviceManage.deviceManage.repository.ChangeLogRepository;
import com.DeviceManage.deviceManage.snmp.SnmpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Управляващи операции чрез SNMP SetRequest.
 * Всяка операция (успешна или не) се записва в журнала на промените.
 */
@Service
public class ManagementService {

    private static final Logger log = LoggerFactory.getLogger(ManagementService.class);

    /** Полетата от SNMPv2-MIB system, които могат да се променят. */
    public enum SystemField {
        NAME("sysName", "1.3.6.1.2.1.1.5.0", "Име на устройството"),
        LOCATION("sysLocation", "1.3.6.1.2.1.1.6.0", "Местоположение"),
        CONTACT("sysContact", "1.3.6.1.2.1.1.4.0", "Контакт");

        public final String mibName;
        public final String oid;
        public final String label;

        SystemField(String mibName, String oid, String label) {
            this.mibName = mibName;
            this.oid = oid;
            this.label = label;
        }
    }

    // IF-MIB ifAdminStatus: 1 = up (включен), 2 = down (изключен от администратор)
    private static final String OID_IF_ADMIN_STATUS = "1.3.6.1.2.1.2.2.1.7.";

    private final DeviceService deviceService;
    private final SnmpClient snmpClient;
    private final ChangeLogRepository changeLogRepository;

    public ManagementService(DeviceService deviceService, SnmpClient snmpClient,
                             ChangeLogRepository changeLogRepository) {
        this.deviceService = deviceService;
        this.snmpClient = snmpClient;
        this.changeLogRepository = changeLogRepository;
    }

    /** Текущите стойности на sysName, sysLocation и sysContact. */
    public Map<SystemField, String> readSystemInfo(Device d) {
        Map<SystemField, String> info = new LinkedHashMap<>();
        for (SystemField f : SystemField.values()) {
            info.put(f, readOrNull(d, f.oid));
        }
        return info;
    }

    public List<ChangeLogEntry> history(Long deviceId) {
        return changeLogRepository.findTop50ByDeviceIdOrderByPerformedAtDesc(deviceId);
    }

    /** Променя sysName, sysLocation или sysContact. Връща записа от журнала. */
    public ChangeLogEntry setSystemField(Long deviceId, SystemField field, String value, String username) {
        Device d = deviceService.findById(deviceId);
        String newValue = value == null ? "" : value.trim();
        if (newValue.length() > 255) {
            throw new IllegalArgumentException("Стойността може да е най-много 255 символа");
        }
        return execute(d, "Смяна на " + field.mibName, field.oid,
                new OctetString(newValue), newValue, username);
    }

    /** Включва или изключва мрежов интерфейс (ifAdminStatus). */
    public ChangeLogEntry setInterfaceAdminStatus(Long deviceId, int ifIndex, boolean up, String username) {
        Device d = deviceService.findById(deviceId);
        return execute(d, (up ? "Включване" : "Изключване") + " на интерфейс " + ifIndex,
                OID_IF_ADMIN_STATUS + ifIndex, new Integer32(up ? 1 : 2), up ? "up" : "down", username);
    }

    private ChangeLogEntry execute(Device d, String operation, String oid, Variable value,
                                   String displayValue, String username) {
        ChangeLogEntry entry = new ChangeLogEntry();
        entry.setDevice(d);
        entry.setPerformedAt(LocalDateTime.now());
        entry.setUsername(username);
        entry.setOperation(operation);
        entry.setOid(oid);
        entry.setNewValue(displayValue);
        entry.setOldValue(readOrNull(d, oid));

        if (d.getWriteCommunity() == null || d.getWriteCommunity().isBlank()) {
            entry.setSuccess(false);
            entry.setMessage("Устройството няма зададена write community");
        } else {
            try {
                snmpClient.set(d.getIpAddress(), d.getSnmpPort(), d.getWriteCommunity(), oid, value);
                entry.setSuccess(true);
            } catch (Exception e) {
                entry.setSuccess(false);
                entry.setMessage(e.getMessage());
            }
        }
        log.info("{} на {} ({}): {} -> {} [{}]", operation, d.getName(), d.getIpAddress(),
                entry.getOldValue(), displayValue, entry.isSuccess() ? "успех" : entry.getMessage());
        return changeLogRepository.save(entry);
    }

    private String readOrNull(Device d, String oid) {
        if (d.getReadCommunity() == null) {
            return null;
        }
        try {
            Variable v = snmpClient.getVariable(d.getIpAddress(), d.getSnmpPort(), d.getReadCommunity(), oid);
            if (v == null) {
                return null;
            }
            if (oid.startsWith(OID_IF_ADMIN_STATUS)) {
                return v.toInt() == 1 ? "up" : "down";
            }
            return v.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
