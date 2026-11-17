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
@TableName("t_lang_event")
public class LangEventDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationId;
    private String sourceLang;
    private String targetLang;
    private String toolName;
    private Boolean fallback;
    private String reason;
    private String snippet;
    private LocalDateTime createTime;
}
