package com.fream.back;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith 경계 진단용 테스트.
 *
 * <p>도메인 패키지(com.fream.back.domain)의 각 하위 패키지를 모듈로 보고,
 * 탐지된 모듈 목록과 모듈 경계 위반(순환 의존 등)을 출력한다.
 * 리팩토링 초기 단계에서는 위반이 다수 존재하므로 {@code verify()} 실패를
 * 빌드 실패로 두지 않고 메시지로 캡처하여 위반 백로그의 ground truth로 사용한다.
 */
class ModularityTests {

    private static final String DOMAIN_BASE_PACKAGE = "com.fream.back.domain";

    @Test
    void captureModuleStructureAndViolations() {
        ApplicationModules modules = ApplicationModules.of(DOMAIN_BASE_PACKAGE);

        System.out.println("=== DETECTED MODULES START ===");
        modules.forEach(module -> System.out.println(module.toString()));
        System.out.println("=== DETECTED MODULES END ===");

        try {
            modules.verify();
            System.out.println("=== NO MODULITH VIOLATIONS ===");
        } catch (Throwable t) {
            System.out.println("=== MODULITH VIOLATIONS START ===");
            System.out.println(t.getMessage());
            System.out.println("=== MODULITH VIOLATIONS END ===");
        }
    }

    /**
     * 모듈 의존 다이어그램/캔버스를 생성한다(아키텍처 Before 스냅샷).
     * 출력은 빌드 산출이므로 실패해도 빌드를 막지 않는다.
     */
    @Test
    void writeModuleDocumentation() {
        try {
            ApplicationModules modules = ApplicationModules.of(DOMAIN_BASE_PACKAGE);
            new Documenter(modules).writeDocumentation();
            System.out.println("=== MODULITH DOCS WRITTEN ===");
        } catch (Throwable t) {
            System.out.println("=== MODULITH DOCS SKIPPED: " + t.getMessage() + " ===");
        }
    }
}
