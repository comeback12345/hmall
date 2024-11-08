package com.hmall.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.po.Order;


/**
 *  服务类
 */
public interface IOrderService extends IService<Order> {

    Long createOrder(OrderFormDTO orderFormDTO) throws JsonProcessingException;

    void markOrderPaySuccess(Long orderId);
    void cancelOrder(Long orderId);
}
