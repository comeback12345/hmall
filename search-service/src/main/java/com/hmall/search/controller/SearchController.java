package com.hmall.search.controller;

import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.CategoryAndBrandVo;
import com.hmall.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @ApiOperation("获取品牌列表")
    @GetMapping("/brands")
    public List<String> getBrands(ItemPageQuery query) {
        return searchService.getBrands(query);
    }

    @ApiOperation("测试 ES 连接")
    @GetMapping("/test-es")
    public String testES() {
        try {
            // 执行一个简单的查询测试
            SearchRequest request = new SearchRequest("items");
            request.source().size(1);
            org.elasticsearch.client.RestHighLevelClient client = 
                new org.elasticsearch.client.RestHighLevelClient(
                    org.elasticsearch.client.RestClient.builder(
                        org.apache.http.HttpHost.create("http://192.168.3.106:9200")
                    )
                );
            org.elasticsearch.action.search.SearchResponse response = 
                client.search(request, RequestOptions.DEFAULT);
            long totalHits = response.getHits().getTotalHits().value;
            client.close();
            return "ES 连接成功！索引 items 中共有 " + totalHits + " 条数据";
        } catch (Exception e) {
            return "ES 连接失败：" + e.getMessage();
        }
    }
}
