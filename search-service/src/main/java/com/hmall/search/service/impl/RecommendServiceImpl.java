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

import java.util.*;
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

            // 获取AI生成的多个关键词列表
            List<String> keywordList = generateKeywordList(itemNames);
            
            if (CollUtils.isEmpty(keywordList)) {
                return getHotProducts(pageNo, pageSize);
            }

            // 对每个关键词搜索，取每个搜索结果的第一条
            List<ItemDoc> recommendedItems = searchByMultipleKeywords(keywordList, pageSize);
            
            if (CollUtils.isEmpty(recommendedItems)) {
                return getHotProducts(pageNo, pageSize);
            }
            
            // 构建分页结果
            PageDTO<ItemDoc> result = new PageDTO<>(
                (long) recommendedItems.size(), 
                1L, 
                recommendedItems
            );
            
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

    /**
     * 根据多个关键词搜索，每个关键词取第一条结果
     */
    private List<ItemDoc> searchByMultipleKeywords(List<String> keywordList, Integer pageSize) {
        List<ItemDoc> allResults = new ArrayList<>();
        Set<String> seenItemIds = new HashSet<>(); // 用于去重，ItemDoc的id是String类型
        
        int targetSize = pageSize != null ? pageSize : 10;
        
        for (String keyword : keywordList) {
            if (allResults.size() >= targetSize) {
                break; // 已经收集足够的商品
            }
            
            try {
                ItemPageQuery query = new ItemPageQuery();
                query.setKey(keyword.trim());
                query.setPageNo(1);
                query.setPageSize(1); // 每个关键词只取第一条
                
                PageDTO<ItemDoc> result = searchService.EsSearch(query);
                
                if (result != null && CollUtils.isNotEmpty(result.getList())) {
                    ItemDoc item = result.getList().get(0);
                    // 去重：如果这个商品已经添加过了，就跳过
                    if (!seenItemIds.contains(item.getId())) {
                        allResults.add(item);
                        seenItemIds.add(item.getId());
                    }
                }
            } catch (Exception e) {
                log.warn("关键词 '{}' 搜索失败: {}", keyword, e.getMessage());
            }
        }
        
        return allResults;
    }

    /**
     * 生成多个独立的搜索关键词列表
     */
    private List<String> generateKeywordList(List<String> itemNames) {
        try {
            String promptContent = buildPromptForMultipleKeywords(itemNames);
            SystemMessage systemMessage = new SystemMessage(
                    "你是一个电商商品推荐助手。根据用户的购物车和购买历史，生成12个不同的商品类别或关键词。" +
                    "每个关键词应该是独立的商品类型，用逗号分隔。" +
                    "例如：红蜻蜓女鞋,诺基亚手机,婴儿纸尿裤,运动鞋,蓝牙耳机,休闲裤,智能手表,双肩包,防晒霜,保温杯,笔记本电脑,墨镜" +
                    "只返回关键词列表，用逗号分隔，不要其他内容。"
            );
            UserMessage userMessage = new UserMessage(promptContent);
            Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

            ChatResponse response = chatModel.call(prompt);
            String keywordsStr = response.getResult().getOutput().getContent();

            log.info("AI生成的关键词列表: {}", keywordsStr);
            
            // 将逗号分隔的字符串转换为列表
            if (keywordsStr != null && !keywordsStr.trim().isEmpty()) {
                return Arrays.stream(keywordsStr.split("[,，]"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .limit(12) // 最多12个关键词
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("AI生成关键词列表失败", e);
            // 降级：直接返回原始商品名称的前12个
            return itemNames.stream()
                    .limit(12)
                    .collect(Collectors.toList());
        }
    }

    private String buildPromptForMultipleKeywords(List<String> itemNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户的购物车和购买历史中的商品：\n");
        for (String name : itemNames) {
            sb.append("- ").append(name).append("\n");
        }
        sb.append("\n请根据这些商品，生成12个不同的商品类别关键词，用于推荐多样化的商品。");
        sb.append("\n每个关键词应该是独立的商品类型，用逗号分隔。");
        return sb.toString();
    }
}
