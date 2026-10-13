package com.DeviceManage.deviceManage.snmp.profile;

import com.DeviceManage.deviceManage.model.Device;
import com.DeviceManage.deviceManage.snmp.SnmpClient;
import org.snmp4j.smi.Variable;

import java.io.IOException;
import java.util.Map;

/**
 * Общ профил, базиран на стандартния HOST-RESOURCES-MIB (RFC 2790).
 * Поддържа се от Windows (SNMP Service), Linux (Net-SNMP), MikroTik и много други.
 */
public abstract class HostResourcesProfile implements VendorProfile {

    // hrProcessorLoad - натоварване на всеки процесор/ядро за последната минута, в %
    static final String OID_HR_PROCESSOR_LOAD = "1.3.6.1.2.1.25.3.3.1.2";
    // hrStorageTable
    static final String OID_HR_STORAGE_TYPE = "1.3.6.1.2.1.25.2.3.1.2";
    static final String OID_HR_STORAGE_SIZE = "1.3.6.1.2.1.25.2.3.1.5";
    static final String OID_HR_STORAGE_USED = "1.3.6.1.2.1.25.2.3.1.6";
    // hrStorageRam - типът на записа за оперативната памет
    static final String HR_STORAGE_RAM = "1.3.6.1.2.1.25.2.1.2";

    private final String name;
    private final String sysObjectIdPrefix;

    protected HostResourcesProfile(String name, String sysObjectIdPrefix) {
        this.name = name;
        this.sysObjectIdPrefix = sysObjectIdPrefix;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean matches(String sysObjectId) {
        return sysObjectIdPrefix != null && sysObjectId != null && sysObjectId.startsWith(sysObjectIdPrefix);
    }

    @Override
    public Double readCpuLoad(SnmpClient snmp, Device d) throws IOException {
        // Броят процесори е различен при всяко устройство, затова обхождаме целия клон
        Map<String, Variable> loads = snmp.walk(d.getIpAddress(), d.getSnmpPort(),
                d.getReadCommunity(), OID_HR_PROCESSOR_LOAD);
        if (loads.isEmpty()) {
            return null;
        }
        double avg = loads.values().stream().mapToLong(Variable::toLong).average().orElse(0);
        return VendorProfile.round(avg);
    }

    @Override
    public Double readMemoryUsage(SnmpClient snmp, Device d) throws IOException {
        // Намираме реда от hrStorageTable, чийто тип е "оперативна памет"
        Map<String, Variable> types = snmp.walk(d.getIpAddress(), d.getSnmpPort(),
                d.getReadCommunity(), OID_HR_STORAGE_TYPE);
        String ramIndex = types.entrySet().stream()
                .filter(e -> HR_STORAGE_RAM.equals(e.getValue().toString()))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        if (ramIndex == null) {
            return null;
        }
        Variable size = snmp.getVariable(d.getIpAddress(), d.getSnmpPort(), d.getReadCommunity(),
                OID_HR_STORAGE_SIZE + "." + ramIndex);
        Variable used = snmp.getVariable(d.getIpAddress(), d.getSnmpPort(), d.getReadCommunity(),
                OID_HR_STORAGE_USED + "." + ramIndex);
        if (size == null || used == null || size.toLong() == 0) {
            return null;
        }
        // И двете стойности са в едни и същи единици (hrStorageAllocationUnits), затова те се съкращават
        return VendorProfile.round(100.0 * used.toLong() / size.toLong());
    }
}
