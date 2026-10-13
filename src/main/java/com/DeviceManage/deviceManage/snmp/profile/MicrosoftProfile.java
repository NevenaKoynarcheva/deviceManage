package com.DeviceManage.deviceManage.snmp.profile;

import org.springframework.stereotype.Component;

/** Windows (Microsoft, enterprise номер 311). Използва стандартния HOST-RESOURCES-MIB. */
@Component
public class MicrosoftProfile extends HostResourcesProfile {

    public MicrosoftProfile() {
        super("Microsoft", "1.3.6.1.4.1.311.");
    }
}
