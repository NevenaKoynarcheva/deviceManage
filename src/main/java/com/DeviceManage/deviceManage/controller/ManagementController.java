package com.DeviceManage.deviceManage.controller;

import com.DeviceManage.deviceManage.model.ChangeLogEntry;
import com.DeviceManage.deviceManage.model.Device;
import com.DeviceManage.deviceManage.service.DeviceService;
import com.DeviceManage.deviceManage.service.ManagementService;
import com.DeviceManage.deviceManage.service.ManagementService.SystemField;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/devices/{id}/manage")
public class ManagementController {

    private final DeviceService deviceService;
    private final ManagementService managementService;

    public ManagementController(DeviceService deviceService, ManagementService managementService) {
        this.deviceService = deviceService;
        this.managementService = managementService;
    }

    @GetMapping
    public String page(@PathVariable Long id, Model model) {
        Device device = deviceService.findById(id);
        model.addAttribute("device", device);
        model.addAttribute("systemInfo", managementService.readSystemInfo(device));
        model.addAttribute("interfaces", deviceService.findLatestInterfaces(id));
        model.addAttribute("history", managementService.history(id));
        return "device-manage";
    }

    @PostMapping("/system")
    public String setSystem(@PathVariable Long id, @RequestParam SystemField field,
                            @RequestParam String value, Principal principal, RedirectAttributes ra) {
        try {
            report(ra, managementService.setSystemField(id, field, value, username(principal)));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/devices/" + id + "/manage";
    }

    @PostMapping("/interfaces/{ifIndex}")
    public String setInterface(@PathVariable Long id, @PathVariable int ifIndex,
                               @RequestParam boolean up, Principal principal, RedirectAttributes ra) {
        report(ra, managementService.setInterfaceAdminStatus(id, ifIndex, up, username(principal)));
        return "redirect:/devices/" + id + "/manage";
    }

    private static void report(RedirectAttributes ra, ChangeLogEntry entry) {
        if (entry.isSuccess()) {
            ra.addFlashAttribute("message", entry.getOperation() + ": успешно");
        } else {
            ra.addFlashAttribute("error", entry.getOperation() + ": неуспешно - " + entry.getMessage());
        }
    }

    /** Името на влезлия потребител; до добавянето на вход - null. */
    private static String username(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
