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
@TableName("t_agent_step")
public class AgentStepDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String runId;
    private String stepType;
    private String name;
    private String detail;
    private String error;
    private Boolean success;
    private Long latencyMs;
    private LocalDateTime createTime;
}
