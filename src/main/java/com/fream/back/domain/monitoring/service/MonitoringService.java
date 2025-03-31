//package com.fream.back.domain.monitoring.service;
//
//import com.fream.back.domain.monitoring.dto.MetricsDTO;
//import com.fream.back.domain.monitoring.dto.RequestStatsDTO;
//import com.fream.back.domain.monitoring.dto.SystemInfoDTO;
//import org.springframework.stereotype.Service;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class MonitoringService {
//
//    public MetricsDTO getBasicMetrics() {
//        MetricsDTO metrics = new MetricsDTO();
//        try {
//            // CPU 사용량
//            Process process = Runtime.getRuntime().exec("top -bn1 | grep 'Cpu(s)' | awk '{print $2}'");
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            metrics.setCpu(Double.parseDouble(reader.readLine().trim()));
//
//            // 메모리 사용량
//            process = Runtime.getRuntime().exec("free | grep Mem | awk '{print $3/$2 * 100.0}'");
//            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            metrics.setMemory(Double.parseDouble(reader.readLine().trim()));
//
//            // 디스크 사용량
//            process = Runtime.getRuntime().exec("df -h / | awk 'NR==2 {print $5}' | sed 's/%//'");
//            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            metrics.setDisk(Double.parseDouble(reader.readLine().trim()));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return metrics;
//    }
//
//    public SystemInfoDTO getSystemInfo() {
//        // 시스템 정보 수집 로직
//        SystemInfoDTO info = new SystemInfoDTO();
//        try {
//            // 호스트명
//            Process process = Runtime.getRuntime().exec("hostname");
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            info.setHostname(reader.readLine().trim());
//
//            // 기타 필요한 시스템 정보 수집
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return info;
//    }
//
//    public RequestStatsDTO getRequestStats(String period) {
//        // 가상의 통계 데이터 (실제로는 로그 파일 파싱 또는 DB에서 가져올 수 있음)
//        RequestStatsDTO stats = new RequestStatsDTO();
//        Map<String, Integer> endpoints = new HashMap<>();
//        endpoints.put("/api/products", 1250);
//        endpoints.put("/api/users", 842);
//        endpoints.put("/api/orders", 567);
//        stats.setTopEndpoints(endpoints);
//
//        // 요청 성공률, 에러율 등의 데이터 설정
//        stats.setSuccessRate(98.5);
//        stats.setErrorRate(1.5);
//
//        // 시간별 요청 수 데이터
//        List<Map<String, Object>> timeData = new ArrayList<>();
//        // 더미 데이터 생성 로직
//
//        stats.setRequestsOverTime(timeData);
//        return stats;
//    }
//}