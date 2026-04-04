package com.hmall.search.service.impl;

import com.hmall.api.client.CartClient;
import com.hmall.api.client.TradeClient;
import com.hmall.api.dto.CartDTO;
import com.hmall.api.dto.PurchaseHistoryDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.utils.CollUtils;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.service.IRecommendService;
import com.hmall.search.service.ISearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendServiceImpl implements IRecommendService {

    private final CartClient cartClient;
    private final TradeClient tradeClient;
    private final ISearchService searchService;
    private final OpenAiChatModel chatModel;

    @Override
    public PageDTO<ItemDoc> getRecommendations(Long userId, Integer pageNo, Integer pageSize) {
        try {
            List<CartDTO> carts = cartClient.queryCartsByUserId(userId);
            List<PurchaseHistoryDTO> purchaseHistory = tradeClient.queryPurchaseHistoryByUserId();

            List<String> itemNames = new ArrayList<>();

            if (CollUtils.isNotEmpty(carts)) {
                itemNames.addAll(carts.stream().map(CartDTO::getName).collect(Collectors.toList()));
            }
            if (CollUtils.isNotEmpty(purchaseHistory)) {
                itemNames.addAll(purchaseHistory.stream().map(PurchaseHistoryDTO::getName).collect(Collectors.toList()));
            }

            if (CollUtils.isEmpty(itemNames)) {
                return getHotProducts(pageNo, pageSize);
            }

            String searchKeywords = generateSearchKeywords(itemNames);

            ItemPageQuery query = new ItemPageQuery();
            query.setKey(searchKeywords);
            query.setPageNo(pageNo != null ? pageNo : 1);
            query.setPageSize(pageSize != null ? pageSize : 10);

            PageDTO<ItemDoc> result = searchService.EsSearch(query);
            
            if (result == null || CollUtils.isEmpty(result.getList())) {
                return getHotProducts(pageNo, pageSize);
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取推荐商品失败", e);
            return getHotProducts(pageNo, pageSize);
        }
    }

    private PageDTO<ItemDoc> getHotProducts(Integer pageNo, Integer pageSize) {
        try {
            ItemPageQuery query = new ItemPageQuery();
            query.setPageNo(pageNo != null ? pageNo : 1);
            query.setPageSize(pageSize != null ? pageSize : 10);
            query.setSortBy("sold");
            query.setIsAsc(false);
            
            return searchService.EsSearch(query);
        } catch (Exception e) {
            log.error("获取热门商品失败", e);
            return new PageDTO<>();
        }
    }

    private String generateSearchKeywords(List<String> itemNames) {
        try {
            String promptContent = buildPrompt(itemNames);
            SystemMessage systemMessage = new SystemMessage(
                    "你是一个电商商品推荐助手。根据用户的购物车和购买历史，生成一个综合的搜索查询字符串，" +
                    "这个查询应该能找到多种相关但不同类型的商品，而不是只匹配一种。" +
                    "只返回查询字符串，不要其他内容。"
            );
            UserMessage userMessage = new UserMessage(promptContent);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            ChatResponse response = chatModel.call(prompt);
            String keywords = response.getResult().getOutput().getContent();

            log.info("AI生成的搜索关键词: {}", keywords);
            return keywords != null ? keywords.trim() : "";
        } catch (Exception e) {
            log.error("AI生成关键词失败", e);
            return String.join(" ", itemNames.subList(0, Math.min(3, itemNames.size())));
        }
    }

    private String buildPrompt(List<String> itemNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户的购物车和购买历史中的商品：\n");
        for (String name : itemNames) {
            sb.append("- ").append(name).append("\n");
        }
        sb.append("\n请根据这些商品，生成一个搜索查询字符串，用于推荐多样化的相关商品。");
        return sb.toString();
    }
}
