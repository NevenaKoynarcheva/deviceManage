package com.DeviceManage.deviceManage.controller;

import com.DeviceManage.deviceManage.dto.DeviceOverview;
import com.DeviceManage.deviceManage.model.DeviceStatus;
import com.DeviceManage.deviceManage.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<DeviceOverview> all = dashboardService.overview();
        model.addAttribute("total", all.size());
        model.addAttribute("up", count(all, DeviceStatus.UP));
        model.addAttribute("down", count(all, DeviceStatus.DOWN));
        model.addAttribute("unknown", count(all, DeviceStatus.UNKNOWN));
        model.addAttribute("problems", all.stream().filter(DeviceOverview::hasProblems).toList());
        return "dashboard";
    }

    private static long count(List<DeviceOverview> all, DeviceStatus status) {
        return all.stream().filter(o -> o.device().getStatus() == status).count();
    }
}
