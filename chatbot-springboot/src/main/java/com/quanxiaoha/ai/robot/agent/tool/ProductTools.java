package com.quanxiaoha.ai.robot.agent.tool;

import com.quanxiaoha.ai.robot.agent.ToolAuditService;
import com.quanxiaoha.ai.robot.commerce.CommerceDtos;
import com.quanxiaoha.ai.robot.commerce.CommerceService;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProductTools {

    @Resource
    private CommerceService commerceService;
    @Resource
    private ToolAuditService toolAuditService;

    @Tool(description = """
            Search the product catalog by keyword (name, sku, category, description). \
            Use for product advice, stock, or price questions.
            """)
    public List<CommerceDtos.ProductResult> searchProductCatalog(
            @ToolParam(description = "Search keyword") String keyword) {
        return toolAuditService.record("searchProductCatalog",
                Map.of("keyword", keyword == null ? "" : keyword),
                () -> commerceService.searchProducts(keyword));
    }
}
