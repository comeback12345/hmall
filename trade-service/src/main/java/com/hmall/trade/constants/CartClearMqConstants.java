package com.hmall.trade.constants;

public interface CartClearMqConstants {
    String ClearCart_EXCHANGE_NAME = "trade.topic";
    String ClearCart_ORDER_QUEUE_NAME = "cart.clear.queue";
    String ClearCart_ORDER_KEY = "order.create";
}
