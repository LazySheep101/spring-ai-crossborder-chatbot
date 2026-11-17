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
@TableName("t_cs_message")
public class CsMessageDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String chatId;
    private String role;
    private String content;
    private String metaJson;
    private LocalDateTime createTime;
}
