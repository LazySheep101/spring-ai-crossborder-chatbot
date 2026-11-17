package com.quanxiaoha.ai.robot.domain.dos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_tool_call_log")
public class ToolCallLogDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationId;
    private String toolName;
    private String argsSummary;
    private String resultSummary;
    private Boolean success;
    private Long latencyMs;
    private LocalDateTime createTime;
}
