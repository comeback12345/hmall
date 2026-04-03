package com.hmall.common.utils;

import cn.hutool.core.lang.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.concurrent.ListenableFutureCallback;

public class RabbitMqHelper {

    private final RabbitTemplate rabbitTemplate;

    private static final Logger log = LoggerFactory.getLogger(RabbitMqHelper.class);

    public RabbitMqHelper(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendMessage(String exchange, String routingKey, Object msg){
        log.debug("准备发送消息，exchange:{}, routingKey:{}, msg:{}", exchange, routingKey, msg);
        rabbitTemplate.convertAndSend(exchange, routingKey, msg);
    }

    public void sendDelayMessage(String exchange, String routingKey, Object msg, int delay){
        rabbitTemplate.convertAndSend(exchange, routingKey, msg, message -> {
            // 使用 setMessageProperty 替代过时的 setDelay 方法
            message.getMessageProperties().setDelay(Integer.valueOf(delay));
            return message;
        });
    }

    public void sendMessageWithConfirm(String exchange, String routingKey, Object msg, int maxRetries){
        log.debug("准备发送消息，exchange:{}, routingKey:{}, msg:{}", exchange, routingKey, msg);
        CorrelationData cd = new CorrelationData(UUID.randomUUID().toString(true));
        cd.getFuture().whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("处理ack回执失败", ex);
                return;
            }
            if (result != null && result.isAck()) {
                return;
            }
            log.debug("消息发送失败，收到nack，已重试次数：{}", 0);
            if(0 >= maxRetries){
                log.error("消息发送重试次数耗尽，发送失败");
                return;
            }
            CorrelationData newCd = new CorrelationData(UUID.randomUUID().toString(true));
            sendMessageWithConfirm(exchange, routingKey, msg, maxRetries, newCd, 1);
        });
        rabbitTemplate.convertAndSend(exchange, routingKey, msg, cd);
    }

    private void sendMessageWithConfirm(String exchange, String routingKey, Object msg, int maxRetries, CorrelationData cd, int retryCount){
        cd.getFuture().whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("处理ack回执失败", ex);
                return;
            }
            if (result != null && result.isAck()) {
                return;
            }
            log.debug("消息发送失败，收到nack，已重试次数：{}", retryCount);
            if(retryCount >= maxRetries){
                log.error("消息发送重试次数耗尽，发送失败");
                return;
            }
            CorrelationData newCd = new CorrelationData(UUID.randomUUID().toString(true));
            sendMessageWithConfirm(exchange, routingKey, msg, maxRetries, newCd, retryCount + 1);
        });
        rabbitTemplate.convertAndSend(exchange, routingKey, msg, cd);
    }
}