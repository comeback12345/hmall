package com.hmall.search.controller;

import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.CategoryAndBrandVo;
import com.hmall.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final ISearchService searchService;

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDoc> search(ItemPageQuery query) {
        return searchService.EsSearch(query);
    }

    @ApiOperation("分类聚合接口")
    @PostMapping("/filters")
    public CategoryAndBrandVo getFilters(@RequestBody ItemPageQuery query) {
        return searchService.getFilters(query);
    }
}
