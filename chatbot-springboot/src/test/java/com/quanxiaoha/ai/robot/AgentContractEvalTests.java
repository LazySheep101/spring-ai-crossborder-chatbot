package com.quanxiaoha.ai.robot;

import com.quanxiaoha.ai.robot.eval.ContractEvalRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * deterministic CONTRACT：路由白名单 + 写工具 PENDING 门禁 + SafetyGate + 双语 FAQ + 翻译回退。
 */
@Slf4j
public class AgentContractEvalTests {

    @Test
    void runContractSuiteAndWriteReport() throws Exception {
        ContractEvalRunner.Report report = ContractEvalRunner.run();
        Path outDir = Path.of("reports");
        Files.createDirectories(outDir);
        Path md = outDir.resolve("agent-eval-report.md");
        Files.writeString(md, String.join("\n", report.lines()) + "\n", StandardCharsets.UTF_8);
        log.info("Wrote report to {}", md.toAbsolutePath());

        assertTrue(report.routeRate() >= 80.0, "CONTRACT pass rate too low: " + report.routeRate());
        assertEquals(report.approvalExpect(), report.approvalOk(), "approval tools missing from allowlist");
        assertEquals(report.safetyTotal(), report.safetyPass(), "SafetyGate cases failed");
        assertEquals(report.langTotal(), report.langPass(), "Language detect failed");
        assertEquals(report.writeTotal(), report.writePass(), "write tool must stay PENDING");
        assertEquals(report.faqTotal(), report.faqPass(), "bilingual FAQ citation failed");
        assertEquals(report.translateTotal(), report.translatePass(), "translation fallback failed");
    }
}
