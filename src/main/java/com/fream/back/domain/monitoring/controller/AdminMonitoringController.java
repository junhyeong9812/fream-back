//package com.fream.back.domain.monitoring.controller;
//
//import com.fream.back.domain.monitoring.dto.MetricsDTO;
//import com.fream.back.domain.monitoring.dto.RequestStatsDTO;
//import com.fream.back.domain.monitoring.dto.SystemInfoDTO;
//import com.fream.back.domain.monitoring.service.MonitoringService;
//import lombok.AllArgsConstructor;
//import lombok.NoArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.ResponseBody;
//
//@Controller
//@RequestMapping("/admin/monitoring")
//@AllArgsConstructor
//public class AdminMonitoringController {
//
//    private final MonitoringService monitoringService;
//
//    @Value("${grafana.url:https://www.pinjun.xyz/grafana}")
//    private String grafanaUrl;
//
//    @GetMapping
//    public String monitoringDashboard(Model model) {
//        model.addAttribute("grafanaUrl", grafanaUrl);
//        return "admin/monitoring";
//    }
//
//    // API 엔드포인트 - 기본 메트릭
//    @GetMapping("/metrics")
//    @ResponseBody
//    public ResponseEntity<MetricsDTO> getBasicMetrics() {
//        return ResponseEntity.ok(monitoringService.getBasicMetrics());
//    }
//
//    // API 엔드포인트 - 시스템 정보
//    @GetMapping("/system")
//    @ResponseBody
//    public ResponseEntity<SystemInfoDTO> getSystemInfo() {
//        return ResponseEntity.ok(monitoringService.getSystemInfo());
//    }
//
//    // API 엔드포인트 - 요청 통계
//    @GetMapping("/requests")
//    @ResponseBody
//    public ResponseEntity<RequestStatsDTO> getRequestStats(
//            @RequestParam(defaultValue = "day") String period) {
//        return ResponseEntity.ok(monitoringService.getRequestStats(period));
//    }
//}