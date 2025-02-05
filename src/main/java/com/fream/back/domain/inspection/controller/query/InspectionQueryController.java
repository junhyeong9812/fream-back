package com.fream.back.domain.inspection.controller.query;

import com.fream.back.domain.inspection.dto.InspectionStandardResponseDto;
import com.fream.back.domain.inspection.entity.InspectionCategory;
import com.fream.back.domain.inspection.service.query.InspectionStandardQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/inspections")
@RequiredArgsConstructor
public class InspectionQueryController {

    private final InspectionStandardQueryService queryService;

    // === 검수 기준 페이징 조회 ===
    @GetMapping
    public ResponseEntity<Page<InspectionStandardResponseDto>> getStandards(
            @RequestParam(name = "category", required = false) InspectionCategory category,
            Pageable pageable) {
        Page<InspectionStandardResponseDto> response;

        if (category != null) {
            response = queryService.getStandardsByCategory(category, pageable);
        } else {
            response = queryService.getStandards(pageable);
        }
        return ResponseEntity.ok(response);
    }

    // === 단일 조회 ===
    @GetMapping("/{id}")
    public ResponseEntity<InspectionStandardResponseDto> getStandard(@PathVariable("id") Long id) {
        InspectionStandardResponseDto response = queryService.getStandard(id);
        return ResponseEntity.ok(response);
    }

    // 파일 다운로드: /inspections/files/{inspectionId}/{fileName}
    @GetMapping("/files/{inspectionId}/{fileName}")
    public ResponseEntity<Resource> getInspectionFile(@PathVariable Long inspectionId,
                                                      @PathVariable String fileName) {
        try {
            // baseDir = /home/ubuntu/fream/inspection
            // subDir = inspection_{inspectionId}
            Path filePath = Paths.get("/home/ubuntu/fream/inspection", "inspection_" + inspectionId, fileName).normalize();

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String contentDisposition = "inline; filename=\"" + fileName + "\"";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

