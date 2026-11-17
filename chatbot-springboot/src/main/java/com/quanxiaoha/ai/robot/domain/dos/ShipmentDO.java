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
@TableName("t_shipment")
public class ShipmentDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String trackingNumber;
    private String carrier;
    private String status;
    private String latestEvent;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
