package com.DeviceManage.deviceManage.controller;

import com.DeviceManage.deviceManage.dto.DeviceForm;
import com.DeviceManage.deviceManage.model.DeviceType;
import com.DeviceManage.deviceManage.model.SnmpVersion;
import com.DeviceManage.deviceManage.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("devices", deviceService.findAll());
        return "devices";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("deviceForm", new DeviceForm());
        addOptions(model);
        return "device-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("deviceForm") DeviceForm form,
                         BindingResult result, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                deviceService.addDevice(form);
                ra.addFlashAttribute("message", "Устройството е добавено");
                return "redirect:/devices";
            } catch (IllegalArgumentException e) {
                result.rejectValue("ipAddress", "duplicate", e.getMessage());
            }
        }
        addOptions(model);
        return "device-form";
    }

    private void addOptions(Model model) {
        model.addAttribute("types", DeviceType.values());
        model.addAttribute("versions", SnmpVersion.values());
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("device", deviceService.findById(id));
        model.addAttribute("latest", deviceService.findLatestSample(id).orElse(null));
        return "device-details";
    }

}