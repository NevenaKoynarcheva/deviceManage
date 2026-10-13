package com.DeviceManage.deviceManage.snmp.profile;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Избира подходящия профил за устройство.
 * Spring автоматично събира всички класове, които имплементират VendorProfile,
 * така че нов профил се включва само като се създаде новият клас.
 */
@Component
public class VendorProfileResolver {

    private final List<VendorProfile> profiles;
    private final GenericProfile generic;

    public VendorProfileResolver(List<VendorProfile> profiles, GenericProfile generic) {
        this.profiles = profiles;
        this.generic = generic;
    }

    /** Разпознава производителя по sysObjectID (1.3.6.1.4.1.<номер на производител>...). */
    public VendorProfile detect(String sysObjectId) {
        return profiles.stream()
                .filter(p -> p.matches(sysObjectId))
                .findFirst()
                .orElse(generic);
    }

    /** Намира профила по името, записано в Device.vendor. */
    public VendorProfile byName(String vendor) {
        return profiles.stream()
                .filter(p -> p.name().equalsIgnoreCase(vendor))
                .findFirst()
                .orElse(generic);
    }
}
