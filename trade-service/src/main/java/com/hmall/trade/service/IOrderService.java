package com.hmall.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hmall.api.dto.PurchaseHistoryDTO;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.po.Order;

import java.util.List;


/**
 *  服务类
 */
public interface IOrderService extends IService<Order> {

    Long createOrder(OrderFormDTO orderFormDTO) throws JsonProcessingException;

    void markOrderPaySuccess(Long orderId);
    void cancelOrder(Long orderId);
    
    List<PurchaseHistoryDTO> queryPurchaseHistoryByUserId(Long userId);
}
