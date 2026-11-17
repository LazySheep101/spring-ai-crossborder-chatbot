package com.quanxiaoha.ai.robot.controller;

import com.google.common.collect.Lists;
import com.quanxiaoha.ai.robot.advisor.CustomerServiceAdvisor;
import com.quanxiaoha.ai.robot.agent.AgentChatService;
import com.quanxiaoha.ai.robot.aspect.ApiOperationLog;
import com.quanxiaoha.ai.robot.config.RagProperties;
import com.quanxiaoha.ai.robot.helpdesk.HelpdeskService;
import com.quanxiaoha.ai.robot.model.vo.chat.AIResponse;
import com.quanxiaoha.ai.robot.model.vo.customerService.*;
import com.quanxiaoha.ai.robot.service.CustomerService;
import com.quanxiaoha.ai.robot.service.HybridRetrievalService;
import com.quanxiaoha.ai.robot.utils.PageResponse;
import com.quanxiaoha.ai.robot.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * AI 客服：支持 Advisor RAG 模式 与 电商 Agent（tools）模式切换
 **/
@RestController
@RequestMapping("/customer-service")
@Slf4j
public class AiCustomerServiceController {

    @Resource
    private CustomerService customerService;
    @Resource
    private HybridRetrievalService hybridRetrievalService;
    @Resource
    private RagProperties ragProperties;
    @Resource
    private AgentChatService agentChatService;
    @Resource
    private HelpdeskService helpdeskService;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    @Value("${customer-service.model}")
    private String model;
    @Value("${customer-service.temperature}")
    private Double temperature;

    @PostMapping("/file/check")
    @ApiOperationLog(description = "检查文件是否存在")
    public Response<CheckFileRspVO> checkFile(@RequestBody @Validated CheckFileReqVO checkFileReqVO) {
        return customerService.checkFile(checkFileReqVO);
    }

    @PostMapping("/file/upload-chunk")
    public Response<?> uploadChunk(@ModelAttribute UploadChunkReqVO uploadChunkReqVO) {
        return customerService.uploadChunk(uploadChunkReqVO);
    }

    @PostMapping("/file/merge-chunk")
    @ApiOperationLog(description = "文件分片合并")
    public Response<?> mergeChunk(@RequestBody @Validated MergeChunkReqVO mergeChunkReqVO) {
        return customerService.mergeChunk(mergeChunkReqVO);
    }

    @PostMapping("/file/delete")
    @ApiOperationLog(description = "删除 Markdown 问答文件")
    public Response<?> deleteMarkdownFile(@RequestBody @Validated DeleteMarkdownFileReqVO deleteMarkdownFileReqVO) {
        return customerService.deleteMarkdownFile(deleteMarkdownFileReqVO);
    }

    @PostMapping("/file/list")
    @ApiOperationLog(description = "Markdown 问答文件分页查询")
    public PageResponse<FindMarkdownFilePageListRspVO> findMarkdownFilePageList(
            @RequestBody @Validated FindMarkdownFilePageListReqVO findMarkdownFilePageListReqVO) {
        return customerService.findMarkdownFilePageList(findMarkdownFilePageListReqVO);
    }

    @PostMapping("/file/update")
    @ApiOperationLog(description = "修改 Markdown 问答文件信息")
    public Response<?> updateMarkdownFile(@RequestBody @Validated UpdateMarkdownFileReqVO updateMarkdownFileReqVO) {
        return customerService.updateMarkdownFile(updateMarkdownFileReqVO);
    }

    /**
     * 流式对话：rag.agent.enabled=true 走工具 Agent；否则走 Advisor RAG（互斥，避免双重召回）
     */
    @PostMapping(value = "/chat/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperationLog(description = "AI 智能客服对话")
    public Flux<AIResponse> chat(@RequestBody @Validated AiCustomerServiceChatReqVO aiChatReqVO) {
        String userMessage = aiChatReqVO.getMessage();
        if (ragProperties.getAgent().isEnabled()) {
            String conversationId = StringUtils.hasText(aiChatReqVO.getChatId())
                    ? aiChatReqVO.getChatId()
                    : UUID.randomUUID().toString();
            log.info("## 客服走 Agent 模式, conversationId={}", conversationId);
            return agentChatService.streamChat(userMessage, conversationId)
                    .doOnSubscribe(s -> {
                        // no-op; chatId 由前端自持，首包 triage meta 可携带
                    });
        }
        log.info("## 客服走 Advisor RAG 模式");
        return advisorRagChat(userMessage);
    }

    /** 兼容旧前端路径 */
    @PostMapping(value = "/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AIResponse> chatLegacy(@RequestBody @Validated AiCustomerServiceChatReqVO aiChatReqVO) {
        return chat(aiChatReqVO);
    }

    /** 公开拉取会话消息（含人工 Inbox 回复） */
    @GetMapping("/chat/{chatId}/messages")
    public Response<?> chatMessages(@PathVariable String chatId) {
        return Response.success(helpdeskService.listMessages(chatId));
    }

    private Flux<AIResponse> advisorRagChat(String userMessage) {
        ChatModel chatModel = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build())
                .build();

        ChatClient.ChatClientRequestSpec chatClientRequestSpec = ChatClient.create(chatModel)
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature))
                .user(userMessage);

        List<Advisor> advisors = Lists.newArrayList();
        advisors.add(new CustomerServiceAdvisor(hybridRetrievalService));
        chatClientRequestSpec.advisors(advisors);

        return chatClientRequestSpec
                .stream()
                .content()
                .map(text -> AIResponse.builder().v(text).build());
    }
}
