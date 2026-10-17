package com.DeviceManage.deviceManage.repository;

import com.DeviceManage.deviceManage.model.MetricHourly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MetricHourlyRepository extends JpaRepository<MetricHourly, Long> {

    List<MetricHourly> findByDeviceIdAndHourStartBetweenOrderByHourStartAsc(
            Long deviceId, LocalDateTime from, LocalDateTime to);

    /**
     * Обобщава всички подробни измервания преди дадения момент в средни стойности по часове.
     * Изпълнява се директно в MySQL с една заявка (INSERT ... SELECT ... GROUP BY).
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO metric_hourly
                (device_id, hour_start, samples, availability_percent,
                 avg_ping_ms, avg_packet_loss, avg_cpu, avg_memory)
            SELECT device_id,
                   TIMESTAMP(DATE(collected_at), MAKETIME(HOUR(collected_at), 0, 0)),
                   COUNT(*),
                   100.0 * SUM(CASE WHEN reachable = 1 THEN 1 ELSE 0 END) / COUNT(*),
                   AVG(ping_latency_ms),
                   AVG(packet_loss_percent),
                   AVG(cpu_load),
                   AVG(memory_usage)
            FROM metric_samples
            WHERE collected_at < :before
            GROUP BY device_id, TIMESTAMP(DATE(collected_at), MAKETIME(HOUR(collected_at), 0, 0))
            """)
    int aggregateBefore(@Param("before") LocalDateTime before);

    @Modifying
    @Query("delete from MetricHourly h where h.device.id = :deviceId")
    void deleteAllByDeviceId(@Param("deviceId") Long deviceId);
}
