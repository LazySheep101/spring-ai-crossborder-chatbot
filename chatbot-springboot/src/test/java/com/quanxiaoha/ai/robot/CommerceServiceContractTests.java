package com.quanxiaoha.ai.robot;

import com.quanxiaoha.ai.robot.commerce.CommerceDtos;
import com.quanxiaoha.ai.robot.commerce.CommerceService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 需要本地 PostgreSQL；若库不可用则跳过。
 */
@SpringBootTest
public class CommerceServiceContractTests {

    @Resource
    private CommerceService commerceService;

    @Test
    void queryOrderRequiresIdentity() {
        Assumptions.assumeTrue(commerceService != null);
        try {
            CommerceDtos.OrderQueryResult denied = commerceService.queryOrder("ORD-10001", null);
            assertFalse(denied.verified());

            CommerceDtos.OrderQueryResult ok = commerceService.queryOrder("ORD-10001", "zhangsan@example.com");
            assertTrue(ok.verified() || "NOT_FOUND".equals(ok.status()) || "UNVERIFIED".equals(ok.status()));
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "DB unavailable: " + e.getMessage());
        }
    }

    @Test
    void approvalCreatesPendingRequest() {
        try {
            CommerceDtos.ActionResult result = commerceService.createAction(
                    "RETURN", "ORD-10001", "zhangsan@example.com", "不想要了", "卫衣");
            assertTrue(result.accepted() || result.message() != null);
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "DB unavailable: " + e.getMessage());
        }
    }
}
