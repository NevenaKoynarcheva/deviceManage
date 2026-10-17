package com.DeviceManage.deviceManage.controller;

import com.DeviceManage.deviceManage.service.AggregationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Ръчно пускане на агрегирането (само за администратор - всеки POST изисква ADMIN). */
@Controller
public class MaintenanceController {

    private final AggregationService aggregationService;

    public MaintenanceController(AggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @PostMapping("/maintenance/aggregate")
    public String aggregate(RedirectAttributes ra) {
        aggregationService.aggregate();
        ra.addFlashAttribute("message", "Агрегирането на старите данни е изпълнено");
        return "redirect:/";
    }
}
