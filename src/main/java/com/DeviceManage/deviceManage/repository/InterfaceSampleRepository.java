package com.DeviceManage.deviceManage.repository;

import com.DeviceManage.deviceManage.model.InterfaceSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InterfaceSampleRepository extends JpaRepository<InterfaceSample, Long> {

    // Последното измерване - от него взимаме момента на последната проверка
    Optional<InterfaceSample> findTopByDeviceIdOrderByCollectedAtDesc(Long deviceId);

    // Всички интерфейси на устройството от една проверка
    List<InterfaceSample> findByDeviceIdAndCollectedAt(Long deviceId, LocalDateTime collectedAt);

    // История на трафика на един интерфейс за графиката
    List<InterfaceSample> findByDeviceIdAndIfIndexAndCollectedAtBetweenOrderByCollectedAtAsc(
            Long deviceId, int ifIndex, LocalDateTime from, LocalDateTime to);

    // Изтрива цялата история на устройството с една заявка (при изтриване на устройство)
    @Modifying
    @Query("delete from InterfaceSample s where s.device.id = :deviceId")
    void deleteAllByDeviceId(@Param("deviceId") Long deviceId);

    // Изтрива подробните записи преди дадения момент (след агрегирането)
    @Modifying
    @Query("delete from InterfaceSample s where s.collectedAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
