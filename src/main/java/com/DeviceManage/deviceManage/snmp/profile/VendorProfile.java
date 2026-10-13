package com.DeviceManage.deviceManage.snmp.profile;

import com.DeviceManage.deviceManage.model.Device;
import com.DeviceManage.deviceManage.snmp.SnmpClient;

import java.io.IOException;

/**
 * Профил на производител (шаблон Strategy).
 * Стандартните показатели (uptime, интерфейси) са еднакви за всички устройства,
 * но CPU и паметта се намират в различни OID-та при различните производители.
 * Всеки профил знае откъде да ги прочете. Нов производител се добавя
 * с нов клас, без промяна в останалия код.
 */
public interface VendorProfile {

    /** Името, което се записва в Device.vendor. */
    String name();

    /** Дали профилът отговаря на устройство с дадения sysObjectID. */
    boolean matches(String sysObjectId);

    /** Натоварване на процесора в %, или null ако не може да се прочете. */
    Double readCpuLoad(SnmpClient snmp, Device device) throws IOException;

    /** Използвана оперативна памет в %, или null ако не може да се прочете. */
    Double readMemoryUsage(SnmpClient snmp, Device device) throws IOException;

    /** Закръгля до един знак след десетичната запетая. */
    static Double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
