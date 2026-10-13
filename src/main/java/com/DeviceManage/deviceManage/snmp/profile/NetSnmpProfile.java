package com.DeviceManage.deviceManage.snmp.profile;

import org.springframework.stereotype.Component;

/** Linux/Unix с Net-SNMP агент (enterprise номер 8072). Използва стандартния HOST-RESOURCES-MIB. */
@Component
public class NetSnmpProfile extends HostResourcesProfile {

    public NetSnmpProfile() {
        super("Net-SNMP", "1.3.6.1.4.1.8072.");
    }
}
