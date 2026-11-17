package com.quanxiaoha.ai.robot.commerce;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quanxiaoha.ai.robot.domain.dos.AdminUserDO;
import com.quanxiaoha.ai.robot.domain.dos.CustomerDO;
import com.quanxiaoha.ai.robot.domain.dos.OrderDO;
import com.quanxiaoha.ai.robot.domain.dos.OrderItemDO;
import com.quanxiaoha.ai.robot.domain.dos.ProductDO;
import com.quanxiaoha.ai.robot.domain.dos.ShipmentDO;
import com.quanxiaoha.ai.robot.domain.mapper.AdminUserMapper;
import com.quanxiaoha.ai.robot.domain.mapper.CustomerMapper;
import com.quanxiaoha.ai.robot.domain.mapper.OrderItemMapper;
import com.quanxiaoha.ai.robot.domain.mapper.OrderMapper;
import com.quanxiaoha.ai.robot.domain.mapper.ProductMapper;
import com.quanxiaoha.ai.robot.domain.mapper.ShipmentMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 启动时确保电商 Agent 表存在，并写入演示种子数据（幂等）。
 */
@Component
@Order(20)
@Slf4j
public class CommerceSchemaInitializer implements ApplicationRunner {

    @Resource
    private DataSource dataSource;
    @Resource
    private CustomerMapper customerMapper;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private ShipmentMapper shipmentMapper;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private AdminAuthService adminAuthService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("sql/commerce_agent.sql"));
            populator.setContinueOnError(true);
            populator.execute(dataSource);
            seedIfEmpty();
            ensureDemoUsers();
            adminAuthService.migratePlaintextPasswords();
            log.info("## 电商 Agent 表结构与种子数据就绪");
        } catch (Exception e) {
            log.error("## 电商 Agent 表初始化失败（请确认 PostgreSQL 可用）", e);
        }
    }

    private void seedIfEmpty() {
        Long productCount = productMapper.selectCount(Wrappers.emptyWrapper());
        if (productCount != null && productCount > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        CustomerDO c1 = CustomerDO.builder().name("张三").email("zhangsan@example.com")
                .phone("13800001111").createTime(now).updateTime(now).build();
        CustomerDO c2 = CustomerDO.builder().name("李四").email("lisi@example.com")
                .phone("13900002222").createTime(now).updateTime(now).build();
        customerMapper.insert(c1);
        customerMapper.insert(c2);

        ProductDO p1 = ProductDO.builder().sku("XZ-SOCK-01").name("小智AI 联名袜")
                .description("透气棉袜，最终售出商品不支持无理由退货").price(new BigDecimal("39.90"))
                .stock(200).category("服饰").createTime(now).updateTime(now).build();
        ProductDO p2 = ProductDO.builder().sku("XZ-HOODIE-02").name("小智AI 连帽卫衣")
                .description("加绒卫衣，支持签收后 7 天无理由退货").price(new BigDecimal("199.00"))
                .stock(80).category("服饰").createTime(now).updateTime(now).build();
        ProductDO p3 = ProductDO.builder().sku("XZ-CUP-03").name("小智AI 保温杯")
                .description("不锈钢保温杯 500ml").price(new BigDecimal("89.00"))
                .stock(120).category("周边").createTime(now).updateTime(now).build();
        productMapper.insert(p1);
        productMapper.insert(p2);
        productMapper.insert(p3);

        OrderDO o1 = OrderDO.builder().orderNo("ORD-10001").customerId(c1.getId())
                .status("DELIVERED").totalAmount(new BigDecimal("238.90")).currency("CNY")
                .shippingAddress("上海市浦东新区张江路 88 号").createTime(now.minusDays(5)).updateTime(now).build();
        OrderDO o2 = OrderDO.builder().orderNo("ORD-10002").customerId(c2.getId())
                .status("SHIPPED").totalAmount(new BigDecimal("89.00")).currency("CNY")
                .shippingAddress("杭州市西湖区文三路 100 号").createTime(now.minusDays(2)).updateTime(now).build();
        OrderDO o3 = OrderDO.builder().orderNo("ORD-10003").customerId(c1.getId())
                .status("PAID").totalAmount(new BigDecimal("199.00")).currency("CNY")
                .shippingAddress("上海市浦东新区张江路 88 号").createTime(now.minusHours(6)).updateTime(now).build();
        orderMapper.insert(o1);
        orderMapper.insert(o2);
        orderMapper.insert(o3);

        orderItemMapper.insert(OrderItemDO.builder().orderId(o1.getId()).productId(p1.getId())
                .sku(p1.getSku()).productName(p1.getName()).quantity(1).unitPrice(p1.getPrice()).build());
        orderItemMapper.insert(OrderItemDO.builder().orderId(o1.getId()).productId(p2.getId())
                .sku(p2.getSku()).productName(p2.getName()).quantity(1).unitPrice(p2.getPrice()).build());
        orderItemMapper.insert(OrderItemDO.builder().orderId(o2.getId()).productId(p3.getId())
                .sku(p3.getSku()).productName(p3.getName()).quantity(1).unitPrice(p3.getPrice()).build());
        orderItemMapper.insert(OrderItemDO.builder().orderId(o3.getId()).productId(p2.getId())
                .sku(p2.getSku()).productName(p2.getName()).quantity(1).unitPrice(p2.getPrice()).build());

        shipmentMapper.insert(ShipmentDO.builder().orderId(o1.getId()).trackingNumber("SF1234567890")
                .carrier("顺丰").status("DELIVERED").latestEvent("已签收").createTime(now).updateTime(now).build());
        shipmentMapper.insert(ShipmentDO.builder().orderId(o2.getId()).trackingNumber("YT9876543210")
                .carrier("圆通").status("IN_TRANSIT").latestEvent("运输中：到达杭州转运中心")
                .createTime(now).updateTime(now).build());

        adminUserMapper.insert(AdminUserDO.builder().username("admin")
                .password(AdminAuthService.hashPassword("123456"))
                .displayName("管理员").role("supervisor").createTime(now).build());
        adminUserMapper.insert(AdminUserDO.builder().username("agent")
                .password(AdminAuthService.hashPassword("agent123"))
                .displayName("小智AI 客服").role("agent").createTime(now).build());
        adminUserMapper.insert(AdminUserDO.builder().username("supervisor")
                .password(AdminAuthService.hashPassword("super123"))
                .displayName("客服主管").role("supervisor").createTime(now).build());
    }

    private void ensureDemoUsers() {
        LocalDateTime now = LocalDateTime.now();
        ensureUser("admin", "123456", "管理员", "supervisor", now);
        ensureUser("agent", "agent123", "小智AI 客服", "agent", now);
        ensureUser("supervisor", "super123", "客服主管", "supervisor", now);
    }

    private void ensureUser(String username, String rawPassword, String displayName, String role, LocalDateTime now) {
        Long count = adminUserMapper.selectCount(Wrappers.<AdminUserDO>lambdaQuery()
                .eq(AdminUserDO::getUsername, username));
        if (count != null && count > 0) {
            return;
        }
        adminUserMapper.insert(AdminUserDO.builder()
                .username(username)
                .password(AdminAuthService.hashPassword(rawPassword))
                .displayName(displayName)
                .role(role)
                .createTime(now)
                .build());
    }
}
