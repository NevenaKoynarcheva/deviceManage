package com.DeviceManage.deviceManage.snmp.profile;

import com.DeviceManage.deviceManage.model.Device;
import com.DeviceManage.deviceManage.snmp.SnmpClient;
import org.snmp4j.smi.Variable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Cisco IOS / IOS-XE (enterprise номер 9).
 * Cisco не поддържа HOST-RESOURCES-MIB, а има собствени MIB-ове.
 */
@Component
public class CiscoProfile implements VendorProfile {

    // CISCO-PROCESS-MIB: cpmCPUTotal5minRev - средно натоварване за 5 минути, в %
    private static final String OID_CPU_5MIN = "1.3.6.1.4.1.9.9.109.1.1.1.1.8";
    // OLD-CISCO-CPU-MIB: avgBusy5 - за по-стари устройства
    private static final String OID_CPU_OLD = "1.3.6.1.4.1.9.2.1.58.0";
    // CISCO-MEMORY-POOL-MIB: заета и свободна памет по пулове, в байтове
    private static final String OID_MEM_USED = "1.3.6.1.4.1.9.9.48.1.1.1.5";
    private static final String OID_MEM_FREE = "1.3.6.1.4.1.9.9.48.1.1.1.6";

    @Override
    public String name() {
        return "Cisco";
    }

    @Override
    public boolean matches(String sysObjectId) {
        return sysObjectId != null && sysObjectId.startsWith("1.3.6.1.4.1.9.");
    }

    @Override
    public Double readCpuLoad(SnmpClient snmp, Device d) throws IOException {
        Map<String, Variable> cpus = snmp.walk(d.getIpAddress(), d.getSnmpPort(), d.getReadCommunity(), OID_CPU_5MIN);
        if (!cpus.isEmpty()) {
            return VendorProfile.round(cpus.values().stream().mapToLong(Variable::toLong).average().orElse(0));
        }
        Variable old = snmp.getVariable(d.getIpAddress(), d.getSnmpPort(), d.getReadCommunity(), OID_CPU_OLD);
        return old == null ? null : VendorProfile.round(old.toLong());
    }

    @Override
    public Double readMemoryUsage(SnmpClient snmp, Device d) throws IOException {
        Map<String, Variable> used = snmp.walk(d.getIpAddress(), d.getSnmpPort(), d.getReadCommunity(), OID_MEM_USED);
        Map<String, Variable> free = snmp.walk(d.getIpAddress(), d.getSnmpPort(), d.getReadCommunity(), OID_MEM_FREE);
        long usedSum = used.values().stream().mapToLong(Variable::toLong).sum();
        long freeSum = free.values().stream().mapToLong(Variable::toLong).sum();
        if (usedSum + freeSum == 0) {
            return null;
        }
        return VendorProfile.round(100.0 * usedSum / (usedSum + freeSum));
    }
}
