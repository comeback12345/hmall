package com.hmall.search.controller;

import com.hmall.common.domain.PageDTO;
import com.hmall.common.utils.UserContext;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.service.IRecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "推荐相关接口")
@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final IRecommendService recommendService;

    @Operation(summary = "获取推荐商品列表")
    @GetMapping("/list")
    public PageDTO<ItemDoc> getRecommendations(
            @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @Parameter(description = "每页数量") @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.getUser();
        return recommendService.getRecommendations(userId, pageNo, pageSize);
    }
}
