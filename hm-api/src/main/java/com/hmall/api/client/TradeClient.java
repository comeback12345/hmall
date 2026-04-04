package com.hmall.api.client;

import com.hmall.api.dto.PurchaseHistoryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@FeignClient("trade-service")
public interface TradeClient {
    @PutMapping("/orders/{orderId}")
    void markOrderPaySuccess(@PathVariable Long orderId);

    @GetMapping("/orders/history")
    List<PurchaseHistoryDTO> queryPurchaseHistoryByUserId();
}
