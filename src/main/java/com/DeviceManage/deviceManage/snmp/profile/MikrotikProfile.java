package com.DeviceManage.deviceManage.snmp.profile;

import org.springframework.stereotype.Component;

/** MikroTik RouterOS (enterprise номер 14988). Използва стандартния HOST-RESOURCES-MIB. */
@Component
public class MikrotikProfile extends HostResourcesProfile {

    public MikrotikProfile() {
        super("MikroTik", "1.3.6.1.4.1.14988.");
    }
}
