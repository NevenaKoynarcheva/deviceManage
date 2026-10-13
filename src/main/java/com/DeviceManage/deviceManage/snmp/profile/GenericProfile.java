package com.DeviceManage.deviceManage.snmp.profile;

import org.springframework.stereotype.Component;

/** Резервен профил: използва се, когато производителят не е разпознат. */
@Component
public class GenericProfile extends HostResourcesProfile {

    public GenericProfile() {
        super("Generic", null);
    }
}
