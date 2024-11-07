package com.hmall.trade.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.api.client.CartClient;
import com.hmall.common.utils.UserContext;
import com.hmall.trade.constants.CartClearMqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
public class OrderCreateListener {
    private final CartClient cartClient;
    private final ObjectMapper objectMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = CartClearMqConstants.ClearCart_ORDER_QUEUE_NAME, durable = "true"),
            exchange = @Exchange(name = CartClearMqConstants.ClearCart_EXCHANGE_NAME),
            key = CartClearMqConstants.ClearCart_ORDER_KEY
    ))
    public void listenPaySuccess(String itemIdsJson, Message message) {
        Long userId = message.getMessageProperties().getHeader("userId");
        log.info("监听到清空购物车的消息，用户ID：{}，商品ID：{}", userId, message);
        UserContext.setUser(userId);
        try {
            Set<Long> itemIds = objectMapper.readValue(itemIdsJson, new TypeReference<Set<Long>>() {});
            cartClient.deleteCartItemByIds(itemIds);
        } catch (IOException e) {
            log.info("转换错啦..............");
            e.printStackTrace();
        }
    }
}
