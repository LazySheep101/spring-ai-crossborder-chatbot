package com.quanxiaoha.ai.robot.commerce;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quanxiaoha.ai.robot.domain.dos.CommerceActionRequestDO;
import com.quanxiaoha.ai.robot.domain.dos.CustomerDO;
import com.quanxiaoha.ai.robot.domain.dos.OrderDO;
import com.quanxiaoha.ai.robot.domain.dos.OrderItemDO;
import com.quanxiaoha.ai.robot.domain.dos.ProductDO;
import com.quanxiaoha.ai.robot.domain.dos.ShipmentDO;
import com.quanxiaoha.ai.robot.domain.mapper.CommerceActionRequestMapper;
import com.quanxiaoha.ai.robot.domain.mapper.CustomerMapper;
import com.quanxiaoha.ai.robot.domain.mapper.OrderItemMapper;
import com.quanxiaoha.ai.robot.domain.mapper.OrderMapper;
import com.quanxiaoha.ai.robot.domain.mapper.ProductMapper;
import com.quanxiaoha.ai.robot.domain.mapper.ShipmentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CommerceService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private CustomerMapper customerMapper;
    @Resource
    private ShipmentMapper shipmentMapper;
    @Resource
    private ProductMapper productMapper;
    @Resource
    private CommerceActionRequestMapper actionRequestMapper;

    public CommerceDtos.OrderQueryResult queryOrder(String orderNo, String customerContact) {
        OrderDO order = findOrder(orderNo);
        if (order == null) {
            return new CommerceDtos.OrderQueryResult(orderNo, "NOT_FOUND", false, List.of(),
                    null, null, null, null, null, null, "未找到该订单，请核对订单号。");
        }
        CustomerDO customer = customerMapper.selectById(order.getCustomerId());
        boolean verified = verifyIdentity(customer, customerContact);
        if (!verified) {
            return new CommerceDtos.OrderQueryResult(order.getOrderNo(), "UNVERIFIED", false, List.of(),
                    null, null, null, null, null, null,
                    "身份校验未通过：请提供下单邮箱或手机号后再查询订单详情。");
        }
        List<OrderItemDO> items = orderItemMapper.selectList(Wrappers.<OrderItemDO>lambdaQuery()
                .eq(OrderItemDO::getOrderId, order.getId()));
        List<String> itemLines = items.stream()
                .map(i -> i.getProductName() + " x" + i.getQuantity())
                .toList();
        ShipmentDO shipment = shipmentMapper.selectOne(Wrappers.<ShipmentDO>lambdaQuery()
                .eq(ShipmentDO::getOrderId, order.getId())
                .last("limit 1"));
        return new CommerceDtos.OrderQueryResult(
                order.getOrderNo(),
                order.getStatus(),
                true,
                itemLines,
                order.getTotalAmount() == null ? null : order.getTotalAmount().toPlainString(),
                order.getCurrency(),
                shipment == null ? null : shipment.getTrackingNumber(),
                shipment == null ? null : shipment.getCarrier(),
                shipment == null ? null : shipment.getStatus(),
                order.getShippingAddress(),
                "订单查询成功。");
    }

    public CommerceDtos.LogisticsResult trackLogistics(String orderNo, String customerContact) {
        OrderDO order = findOrder(orderNo);
        if (order == null) {
            return new CommerceDtos.LogisticsResult(orderNo, null, null, null, null, "未找到该订单。");
        }
        CustomerDO customer = customerMapper.selectById(order.getCustomerId());
        if (!verifyIdentity(customer, customerContact)) {
            return new CommerceDtos.LogisticsResult(order.getOrderNo(), null, null, null, null,
                    "身份校验未通过，无法查看物流。");
        }
        ShipmentDO shipment = shipmentMapper.selectOne(Wrappers.<ShipmentDO>lambdaQuery()
                .eq(ShipmentDO::getOrderId, order.getId())
                .last("limit 1"));
        if (shipment == null) {
            return new CommerceDtos.LogisticsResult(order.getOrderNo(), null, null, order.getStatus(), null,
                    "该订单暂无物流信息（可能尚未发货）。");
        }
        return new CommerceDtos.LogisticsResult(order.getOrderNo(), shipment.getTrackingNumber(),
                shipment.getCarrier(), shipment.getStatus(), shipment.getLatestEvent(), "物流查询成功。");
    }

    public List<CommerceDtos.ProductResult> searchProducts(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        List<ProductDO> products = productMapper.selectList(Wrappers.<ProductDO>lambdaQuery()
                .and(StringUtils.hasText(kw), w -> w
                        .like(ProductDO::getName, kw)
                        .or().like(ProductDO::getDescription, kw)
                        .or().like(ProductDO::getSku, kw)
                        .or().like(ProductDO::getCategory, kw))
                .last("limit 10"));
        return products.stream()
                .map(p -> new CommerceDtos.ProductResult(
                        p.getSku(), p.getName(), p.getDescription(),
                        p.getPrice().toPlainString(), p.getStock(), p.getCategory()))
                .toList();
    }

    public CommerceDtos.ActionResult createAction(String actionType, String orderNo, String customerContact,
                                                   String reason, String payload) {
        OrderDO order = findOrder(orderNo);
        CustomerDO customer = order == null ? null : customerMapper.selectById(order.getCustomerId());
        ActionGate.Decision decision = ActionGate.decide(
                order != null,
                verifyIdentity(customer, customerContact),
                StringUtils.hasText(customerContact));
        if (!decision.accepted()) {
            return CommerceDtos.ActionResult.rejected(actionType, order == null ? orderNo : order.getOrderNo(),
                    decision.message());
        }
        LocalDateTime now = LocalDateTime.now();
        CommerceActionRequestDO req = CommerceActionRequestDO.builder()
                .actionType(actionType)
                .orderNo(order.getOrderNo())
                .customerContact(customerContact.trim())
                .reason(reason)
                .payload(payload)
                .status("PENDING")
                .createTime(now)
                .updateTime(now)
                .build();
        actionRequestMapper.insert(req);
        return CommerceDtos.ActionResult.pending(actionType, order.getOrderNo(), req.getId(),
                "已创建待人工审批请求，AI 不会直接改单或退款。");
    }

    public List<OrderDO> listOrders() {
        return orderMapper.selectList(Wrappers.<OrderDO>lambdaQuery().orderByDesc(OrderDO::getId));
    }

    public OrderDO getOrderByNo(String orderNo) {
        return findOrder(orderNo);
    }

    public List<OrderItemDO> listOrderItems(Long orderId) {
        return orderItemMapper.selectList(Wrappers.<OrderItemDO>lambdaQuery().eq(OrderItemDO::getOrderId, orderId));
    }

    public CustomerDO getCustomer(Long id) {
        return customerMapper.selectById(id);
    }

    public List<CustomerDO> listCustomers() {
        return customerMapper.selectList(Wrappers.<CustomerDO>lambdaQuery().orderByDesc(CustomerDO::getId));
    }

    public ShipmentDO getShipmentByOrderId(Long orderId) {
        return shipmentMapper.selectOne(Wrappers.<ShipmentDO>lambdaQuery()
                .eq(ShipmentDO::getOrderId, orderId).last("limit 1"));
    }

    public List<ProductDO> listProducts() {
        return productMapper.selectList(Wrappers.emptyWrapper());
    }

    private OrderDO findOrder(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        String normalized = orderNo.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        OrderDO byNormalized = orderMapper.selectOne(Wrappers.<OrderDO>lambdaQuery()
                .eq(OrderDO::getOrderNo, normalized)
                .last("limit 1"));
        if (byNormalized != null) {
            return byNormalized;
        }
        return orderMapper.selectOne(Wrappers.<OrderDO>lambdaQuery()
                .eq(OrderDO::getOrderNo, orderNo.trim())
                .last("limit 1"));
    }

    private boolean verifyIdentity(CustomerDO customer, String contact) {
        if (customer == null || !StringUtils.hasText(contact)) {
            return false;
        }
        String c = contact.trim().toLowerCase(Locale.ROOT);
        return (customer.getEmail() != null && customer.getEmail().equalsIgnoreCase(c))
                || (customer.getPhone() != null && customer.getPhone().equals(contact.trim()));
    }
}
