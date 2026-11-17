package com.quanxiaoha.ai.robot.commerce;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quanxiaoha.ai.robot.domain.dos.CommerceActionRequestDO;
import com.quanxiaoha.ai.robot.domain.dos.OrderDO;
import com.quanxiaoha.ai.robot.domain.mapper.CommerceActionRequestMapper;
import com.quanxiaoha.ai.robot.domain.mapper.OrderMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommerceApprovalService {

    @Resource
    private CommerceActionRequestMapper actionRequestMapper;
    @Resource
    private OrderMapper orderMapper;

    public List<CommerceActionRequestDO> list(String status) {
        return actionRequestMapper.selectList(Wrappers.<CommerceActionRequestDO>lambdaQuery()
                .eq(StringUtils.hasText(status), CommerceActionRequestDO::getStatus, status)
                .orderByDesc(CommerceActionRequestDO::getId));
    }

    public CommerceActionRequestDO review(Long id, boolean approve, String reviewer, String note) {
        CommerceActionRequestDO req = actionRequestMapper.selectById(id);
        if (req == null) {
            throw new IllegalArgumentException("审批单不存在: " + id);
        }
        if (!"PENDING".equals(req.getStatus())) {
            throw new IllegalStateException("审批单已处理: " + req.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        req.setStatus(approve ? "APPROVED" : "REJECTED");
        req.setReviewer(StringUtils.hasText(reviewer) ? reviewer : "supervisor");
        req.setReviewNote(note);
        req.setUpdateTime(now);
        actionRequestMapper.updateById(req);

        if (approve) {
            applyOrderSideEffect(req);
        }
        return req;
    }

    private void applyOrderSideEffect(CommerceActionRequestDO req) {
        OrderDO order = orderMapper.selectOne(Wrappers.<OrderDO>lambdaQuery()
                .eq(OrderDO::getOrderNo, req.getOrderNo()).last("limit 1"));
        if (order == null) {
            return;
        }
        String type = req.getActionType() == null ? "" : req.getActionType().toUpperCase();
        switch (type) {
            case "RETURN" -> order.setStatus("RETURN_REQUESTED");
            case "REFUND" -> order.setStatus("REFUND_APPROVED");
            case "REPLACEMENT" -> order.setStatus("REPLACEMENT_APPROVED");
            case "ADDRESS_CHANGE" -> {
                if (StringUtils.hasText(req.getPayload())) {
                    order.setShippingAddress(req.getPayload());
                }
                order.setStatus("ADDRESS_CHANGE_APPROVED");
            }
            default -> {
            }
        }
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }
}
