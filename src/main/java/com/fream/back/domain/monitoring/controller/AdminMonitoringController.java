package com.fream.back.domain.monitoring.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/monitoring")
public class AdminMonitoringController {

    @Value("${kibana.url:https://www.pinjun.xyz/kibana}")
    private String kibanaUrl;

    @GetMapping
    public String monitoringDashboard(Model model) {
        model.addAttribute("kibanaUrl", kibanaUrl);
        return "admin/monitoring";
    }
}