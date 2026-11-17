package com.quanxiaoha.ai.robot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author: lazysheep101
 * @Date: 2026/10/8 13:44
 * @Version: v1.0.0
 * @Description: RAG 检索优化相关配置项
 *
 *  切分 / ES / 混合检索 / 重排序等开关与参数统一放这里，方便随时切换「优化前 / 优化后」做对比
 **/
@Data
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    /**
     * 文档切分配置
     */
    private Splitter splitter = new Splitter();

    @Data
    public static class Splitter {

        /**
         * 是否启用 Token 切分
         */
        private boolean enabled = true;

        /**
         * 每个文档块的目标大小（单位: Token）
         */
        private int chunkSize = 800;

        /**
         * 块内按标点截断时，允许的最小块字符数
         */
        private int minChunkSizeChars = 350;
    }

    /**
     * Elasticsearch 关键词检索配置
     */
    private Elasticsearch elasticsearch = new Elasticsearch();

    /**
     * 混合检索配置（向量 + ES 关键词 → RRF 融合）
     */
    private Hybrid hybrid = new Hybrid();

    /**
     * 重排序配置（RRF / 召回候选 → Rerank 精排 → 再截断进 Prompt）
     */
    private Rerank rerank = new Rerank();

    /**
     * 电商客服 Agent（工具调用）配置
     */
    private Agent agent = new Agent();

    @Data
    public static class Elasticsearch {

        /**
         * 是否启用 ES 双写双删（关闭后仅保留向量检索）
         */
        private boolean enabled = true;

        /**
         * 文档块索引名称
         */
        private String indexName = "rag_chunk";
    }

    @Data
    public static class Hybrid {

        /**
         * 是否启用混合检索（关闭后查询侧退回纯向量）
         */
        private boolean enabled = true;

        /**
         * 每一路召回的候选数（向量 / ES 各自 topK）
         */
        private int candidateTopK = 10;

        /**
         * 最终塞进 Prompt 的条数（开启 Rerank 时表示精排后的条数）
         */
        private int finalTopK = 5;

        /**
         * RRF 调和常数，常用 60；越大则排名靠后的文档衰减越慢
         */
        private int rrfK = 60;
    }

    @Data
    public static class Rerank {

        /**
         * 是否启用重排序（关闭后保持「召回/RRF → 直接截断 finalTopK」）
         */
        private boolean enabled = false;

        /**
         * 百炼文本排序模型，推荐 qwen3-rerank / gte-rerank-v2
         */
        private String model = "qwen3-rerank";

        /**
         * 文本排序 HTTP 地址；留空则根据 spring.ai.openai.base-url 自动推导 MaaS 地址
         */
        private String apiUrl = "";

        /**
         * API Key；留空则复用 spring.ai.openai.api-key
         */
        private String apiKey = "";

        /**
         * 送入 Rerank 前的候选池大小（RRF 融合或纯向量截断到此数量后再精排）
         */
        private int candidateTopK = 20;

        /**
         * 相关性分数阈值：低于该分的文档丢弃；0 表示不过滤
         */
        private double scoreThreshold = 0.0;

        /**
         * 单条文档送入 Rerank 时的最大字符数，避免超长块触发 400
         */
        private int maxDocumentChars = 4000;

        /**
         * 排序任务说明（qwen3-rerank 等支持）；FAQ 客服场景默认按「找能回答问题的段落」
         */
        private String instruct = "Given a web search query, retrieve relevant passages that answer the query.";
    }

    @Data
    public static class Agent {

        /**
         * 是否启用电商 Agent（true：ChatClient.tools，关闭 Advisor 双重召回；false：沿用 CustomerServiceAdvisor RAG）
         */
        private boolean enabled = true;

        /**
         * Agent 模式 System Prompt（可被配置覆盖）
         */
        private String systemPrompt = """
                你是「小智AI」跨境电商智能客服 Agent。必须通过工具完成查单、物流、商品、政策检索与退换货申请。
                规则：
                1. 先判断意图，再调用对应工具；不要编造订单、物流或政策。
                2. 查订单/退换货时必须校验客户邮箱或手机号。
                3. 退货、退款、换货、改地址只能创建待审批请求，禁止声称已直接退款成功。
                4. 政策类问题必须调用 refundPolicyRAG，并基于返回片段回答。
                5. 客户明确要求人工或情绪激烈时，调用 escalateToHuman。
                6. 工具结果不足时如实说明，可引导补充订单号与联系方式。
                """;
    }

}
