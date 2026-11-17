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
@TableName("t_commerce_action_request")
public class CommerceActionRequestDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String actionType;
    private String orderNo;
    private String customerContact;
    private String reason;
    private String payload;
    private String status;
    private String reviewer;
    private String reviewNote;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
