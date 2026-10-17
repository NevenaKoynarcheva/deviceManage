package com.DeviceManage.deviceManage.repository;

import com.DeviceManage.deviceManage.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<Link, Long> {

    // Изтрива всички връзки на устройство (при изтриване на устройството)
    @Modifying
    @Query("delete from Link l where l.deviceA.id = :deviceId or l.deviceB.id = :deviceId")
    void deleteAllByDeviceId(@Param("deviceId") Long deviceId);
}
