package com.hmall.api.client;

import com.hmall.api.dto.CartDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient(value = "cart-service")
public interface CartClient {
    @DeleteMapping("/carts")
    void deleteCartItemByIds(@RequestParam Collection<Long> ids);

    @GetMapping("/carts/user/{userId}")
    List<CartDTO> queryCartsByUserId(@PathVariable("userId") Long userId);
}
