package com.quanxiaoha.ai.robot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 小智AI 启动时幂等初始化核心元数据表（对话 / 知识库文件 / 向量表 / 扩展）。
 * 电商业务表由 {@link com.quanxiaoha.ai.robot.commerce.CommerceSchemaInitializer} 继续初始化。
 */
@Component
@Order(10)
@Slf4j
public class RobotSchemaInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    public RobotSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("sql/ai_robot.sql"));
            populator.setContinueOnError(true);
            populator.setSeparator(";");
            populator.execute(dataSource);
            log.info("## 小智AI 核心元数据表就绪（ai_robot.sql）");
        } catch (Exception e) {
            log.error("## 小智AI 核心元数据初始化失败（请确认 PostgreSQL + pgvector 可用）", e);
            throw e instanceof RuntimeException re ? re : new IllegalStateException(e);
        }
    }
}
