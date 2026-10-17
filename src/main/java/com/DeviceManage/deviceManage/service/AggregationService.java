package com.DeviceManage.deviceManage.service;

import com.DeviceManage.deviceManage.repository.InterfaceSampleRepository;
import com.DeviceManage.deviceManage.repository.MetricHourlyRepository;
import com.DeviceManage.deviceManage.repository.MetricSampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Нощна задача за агрегиране на историческите данни.
 * 1) Подробните измервания, по-стари от N дни, се обобщават в средни стойности по часове.
 * 2) След това подробните редове се изтриват.
 * Така графиките за дълъг период остават възможни, а базата не расте безкрайно.
 */
@Service
public class AggregationService {

    private static final Logger log = LoggerFactory.getLogger(AggregationService.class);

    private final MetricHourlyRepository hourlyRepository;
    private final MetricSampleRepository metricSampleRepository;
    private final InterfaceSampleRepository interfaceSampleRepository;

    @Value("${nms.retention.raw-days:7}")
    private int rawDays;

    public AggregationService(MetricHourlyRepository hourlyRepository,
                              MetricSampleRepository metricSampleRepository,
                              InterfaceSampleRepository interfaceSampleRepository) {
        this.hourlyRepository = hourlyRepository;
        this.metricSampleRepository = metricSampleRepository;
        this.interfaceSampleRepository = interfaceSampleRepository;
    }

    @Scheduled(cron = "${nms.aggregation.cron:0 15 3 * * *}")
    @Transactional
    public void aggregate() {
        // Границата е закръглена до цял час, за да не се раздели един час на две части
        LocalDateTime before = LocalDateTime.now().minusDays(rawDays).truncatedTo(ChronoUnit.HOURS);

        int hours = hourlyRepository.aggregateBefore(before);
        int metrics = metricSampleRepository.deleteOlderThan(before);
        int interfaces = interfaceSampleRepository.deleteOlderThan(before);

        log.info("Агрегиране преди {}: {} нови часови записа, изтрити {} измервания и {} записа за интерфейси",
                before, hours, metrics, interfaces);
    }
}
