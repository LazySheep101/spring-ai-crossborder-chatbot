package com.quanxiaoha.ai.robot.model.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * SSE 响应：文本增量 + 旁路元数据（citation/action/ticket/blocked）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIResponse {

    /** text | meta | blocked */
    @Builder.Default
    private String type = "text";

    private String v;
    private String reasoning;

    /** citation | action | ticket | triage */
    private String metaKind;

    private List<Map<String, Object>> citations;
    private Long actionRequestId;
    private String actionType;
    private String actionStatus;
    private String ticketNo;
    private String specialist;
    private String sourceLang;
    private String message;
}
