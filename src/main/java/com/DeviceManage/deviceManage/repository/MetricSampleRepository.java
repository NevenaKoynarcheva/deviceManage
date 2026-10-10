package com.DeviceManage.deviceManage.repository;

import com.DeviceManage.deviceManage.model.MetricSample;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MetricSampleRepository extends JpaRepository<MetricSample, Long> {

    // История за графика: всички измервания на устройство в даден период
    List<MetricSample> findByDeviceIdAndCollectedAtBetweenOrderByCollectedAtAsc(
            Long deviceId, LocalDateTime from, LocalDateTime to);

    // Последното измерване за началния екран
    Optional<MetricSample> findTopByDeviceIdOrderByCollectedAtDesc(Long deviceId);
}

