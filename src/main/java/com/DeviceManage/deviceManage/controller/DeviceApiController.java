package com.DeviceManage.deviceManage.controller;

import com.DeviceManage.deviceManage.dto.MetricPoint;
import com.DeviceManage.deviceManage.dto.TrafficPoint;
import com.DeviceManage.deviceManage.service.DeviceService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceApiController {

    private final DeviceService deviceService;

    public DeviceApiController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/{id}/metrics")
    public List<MetricPoint> metrics(@PathVariable Long id,
                                     @RequestParam(defaultValue = "1") int hours) {
        deviceService.findById(id); // 404, ако устройството не съществува
        int safeHours = Math.min(Math.max(hours, 1), 720); // от 1 час до 30 дни
        return deviceService.getMetrics(id, safeHours);
    }

    @GetMapping("/{id}/interfaces/{ifIndex}/traffic")
    public List<TrafficPoint> traffic(@PathVariable Long id, @PathVariable int ifIndex,
                                      @RequestParam(defaultValue = "1") int hours) {
        deviceService.findById(id);
        int safeHours = Math.min(Math.max(hours, 1), 168);
        return deviceService.getTraffic(id, ifIndex, safeHours);
    }
}
