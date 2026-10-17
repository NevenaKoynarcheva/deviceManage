package com.DeviceManage.deviceManage.controller;

import com.DeviceManage.deviceManage.dto.LinkForm;
import com.DeviceManage.deviceManage.dto.TopologyDto;
import com.DeviceManage.deviceManage.service.DeviceService;
import com.DeviceManage.deviceManage.service.LinkService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LinkController {

    private final LinkService linkService;
    private final DeviceService deviceService;

    public LinkController(LinkService linkService, DeviceService deviceService) {
        this.linkService = linkService;
        this.deviceService = deviceService;
    }

    @GetMapping("/links")
    public String list(Model model) {
        model.addAttribute("links", linkService.findAll());
        return "links";
    }

    @GetMapping("/links/new")
    public String newForm(Model model) {
        model.addAttribute("linkForm", new LinkForm());
        model.addAttribute("devices", deviceService.findAll());
        return "link-form";
    }

    @PostMapping("/links")
    public String create(@Valid @ModelAttribute("linkForm") LinkForm form, BindingResult result,
                         Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                linkService.addLink(form);
                ra.addFlashAttribute("message", "Връзката е добавена");
                return "redirect:/links";
            } catch (IllegalArgumentException e) {
                result.rejectValue("deviceBId", "same", e.getMessage());
            }
        }
        model.addAttribute("devices", deviceService.findAll());
        return "link-form";
    }

    @PostMapping("/links/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        linkService.deleteLink(id);
        ra.addFlashAttribute("message", "Връзката е изтрита");
        return "redirect:/links";
    }

    /** JSON за картата на мрежата. */
    @GetMapping("/api/topology")
    @ResponseBody
    public TopologyDto topology() {
        return linkService.topology();
    }
}
