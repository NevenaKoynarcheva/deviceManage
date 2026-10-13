package com.DeviceManage.deviceManage.repository;

import com.DeviceManage.deviceManage.model.ChangeLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChangeLogRepository extends JpaRepository<ChangeLogEntry, Long> {

    List<ChangeLogEntry> findTop50ByDeviceIdOrderByPerformedAtDesc(Long deviceId);

    @Modifying
    @Query("delete from ChangeLogEntry c where c.device.id = :deviceId")
    void deleteAllByDeviceId(@Param("deviceId") Long deviceId);
}
