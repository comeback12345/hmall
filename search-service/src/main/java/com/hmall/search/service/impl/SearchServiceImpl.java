package com.hmall.search.service.impl;


import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.utils.CollUtils;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.CategoryAndBrandVo;
import com.hmall.search.mapper.SearchMapper;
import com.hmall.search.service.ISearchService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;



@Service
public class SearchServiceImpl extends ServiceImpl<SearchMapper, Item> implements ISearchService {
    @Resource
    private  RestHighLevelClient restHighLevelClient;
    @Override
    public PageDTO<ItemDoc> EsSearch(ItemPageQuery query) {
        PageDTO<ItemDoc> result = new PageDTO<>();
        //1.构造请求
        SearchRequest searchRequest = new SearchRequest("items");
        //2.构造查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //精准总数
        searchRequest.source().trackTotalHits(true);
        if (query.getKey()!=null && !"".equals(query.getKey())) {
//            searchRequest.source()
//                    .query(QueryBuilders.matchQuery("name", query.getKey()));
            boolQueryBuilder.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        //高亮
        searchRequest.source().highlighter(
                SearchSourceBuilder.highlight()
                        .field("name")
                        .preTags("<em>")
                        .postTags("</em>")
        );
        //分页
        searchRequest.source().from(query.from()).size(query.getPageSize());//获取页数
        //排序
        if (query.getSortBy() != null && !"".equals(query.getSortBy())){
            searchRequest.source().sort(query.getSortBy(), query.getIsAsc()? SortOrder.ASC : SortOrder.DESC);
        }else {
            searchRequest.source().sort("_score", SortOrder.DESC);
            //searchRequest.source().sort("price",query.getIsAsc()? SortOrder.DESC : SortOrder.ASC);
            //searchRequest.source().sort("updateTime",query.getIsAsc()? SortOrder.ASC : SortOrder.DESC);
        }
        //分类
        if (query.getCategory()!=null && !"".equals(query.getCategory())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category", query.getCategory()));
//            searchRequest.source().query(QueryBuilders.termQuery("category", query.getCategory()));
        }
        //品牌
        if (query.getBrand()!=null && !"".equals(query.getBrand())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("brand", query.getBrand()));
//            searchRequest.source().query(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        //价格
        if (query.getMinPrice()!=null && query.getMaxPrice()!=null) {
            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()).lte(query.getMaxPrice()));
//            searchRequest.source().query(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()).lte(query.getMaxPrice()));
        }


        //searchRequest.source().query(boolQueryBuilder);
        //排名 广告优先
        searchRequest.source().query(QueryBuilders.functionScoreQuery(boolQueryBuilder,
                new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(QueryBuilders.termQuery("isAD", true),
                                ScoreFunctionBuilders.weightFactorFunction(100))
                }).boostMode(CombineFunction.MULTIPLY));

        try {
            //发起请求
            SearchResponse search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //解析结果
            result.setTotal(search.getHits().getTotalHits().value);
            result.setPages(search.getHits().getTotalHits().value%query.getPageSize()==0?search.getHits().getTotalHits().value/query.getPageSize():search.getHits().getTotalHits().value/query.getPageSize()+1);
            final SearchHit[] hits = search.getHits().getHits();
            List<ItemDoc> list=new ArrayList<>();
            for (SearchHit hit : hits) {
                // 手动解析 JSON，避免日期字段转换异常
                String sourceJson = hit.getSourceAsString();
                cn.hutool.json.JSONObject jsonObject = new cn.hutool.json.JSONObject(sourceJson);
                ItemDoc itemDoc = new ItemDoc();
                itemDoc.setId(jsonObject.getStr("id"));
                itemDoc.setName(jsonObject.getStr("name"));
                itemDoc.setPrice(jsonObject.getInt("price"));
                itemDoc.setImage(jsonObject.getStr("image"));
                itemDoc.setCategory(jsonObject.getStr("category"));
                itemDoc.setBrand(jsonObject.getStr("brand"));
                itemDoc.setSold(jsonObject.getInt("sold", 0));
                itemDoc.setCommentCount(jsonObject.getInt("commentCount", 0));
                itemDoc.setIsAD(jsonObject.getBool("isAD", false));
                
                Map<String, HighlightField> hfs = hit.getHighlightFields();
                if (CollUtils.isNotEmpty(hfs)) {
                    // 5.1.有高亮结果，获取name的高亮结果
                    HighlightField hf = hfs.get("name");
                    if (hf != null) {
                        // 5.2.获取第一个高亮结果片段，就是商品名称的高亮值
                        String hfName = hf.getFragments()[0].string();
                        itemDoc.setName(hfName);
                    }
                }
                list.add(itemDoc);
            }
            result.setList(list);
        } catch (IOException e) {
            log.error("查询ES失败,出现异常");
        }
        //返回
        return result;
    }

    //获得分类和品牌的聚合值
    @Override
    public CategoryAndBrandVo getFilters(ItemPageQuery query) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        CategoryAndBrandVo categoryAndBrandVo = new CategoryAndBrandVo();
        // 1.创建Request
        SearchRequest request = new SearchRequest("items");
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (query.getKey()!=null && !"".equals(query.getKey())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        if (query.getCategory()!=null && !"".equals(query.getCategory())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        if (query.getBrand()!=null && !"".equals(query.getBrand())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        if (query.getMinPrice()!=null && query.getMaxPrice()!=null){
            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()).lte(query.getMaxPrice()));
        }
        request.source().query(boolQueryBuilder).size(0);

        request.source().aggregation(
                AggregationBuilders.terms("category_agg").field("category").size(100)
        );

        request.source().aggregation(
                AggregationBuilders.terms("brand_agg").field("brand").size(100));
        List<String> categoryList = new ArrayList<>();
        List<String> brandList = new ArrayList<>();
        // 4.发送请求
        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            
            // 调试信息：打印响应详情
            System.out.println("==== ES 响应开始 ====");
            System.out.println("总命中数：" + response.getHits().getTotalHits().value);
            Aggregations aggregations = response.getAggregations();
            System.out.println("聚合对象：" + aggregations);
            
            if (aggregations != null) {
                Terms categoryTerms = aggregations.get("category_agg");
                System.out.println("分类聚合对象：" + categoryTerms);
                if (categoryTerms != null) {
                    List<? extends Terms.Bucket> buckets = categoryTerms.getBuckets();
                    System.out.println("分类桶数量：" + buckets.size());
                    for (Terms.Bucket bucket : buckets) {
                        String category = bucket.getKeyAsString();
                        long docCount = bucket.getDocCount();
                        System.out.println("分类：" + category + ", 文档数：" + docCount);
                        categoryList.add(category);
                    }
                }
                
                Terms brandTerms = aggregations.get("brand_agg");
                System.out.println("品牌聚合对象：" + brandTerms);
                if (brandTerms != null) {
                    List<? extends Terms.Bucket> buckets1 = brandTerms.getBuckets();
                    System.out.println("品牌桶数量：" + buckets1.size());
                    for (Terms.Bucket bucket : buckets1) {
                        String brand = bucket.getKeyAsString();
                        long docCount = bucket.getDocCount();
                        System.out.println("品牌：" + brand + ", 文档数：" + docCount);
                        brandList.add(brand);
                    }
                }
            } else {
                System.out.println("聚合结果为 null！");
            }
            System.out.println("==== ES 响应结束 ====");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        categoryAndBrandVo.setCategory(categoryList);
        categoryAndBrandVo.setBrand(brandList);
        return categoryAndBrandVo;
    }

    @Override
    public List<String> getBrands(ItemPageQuery query) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<String> brandList = new ArrayList<>();
        
        SearchRequest request = new SearchRequest("items");
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        
        if (query.getKey() != null && !"".equals(query.getKey())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        
        request.source().query(boolQueryBuilder).size(0);
        
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg")
                .field("brand")
                .size(200)  // 设置足够大的值，获取所有品牌
                .order(BucketOrder.count(false));
        
        request.source().aggregation(brandAgg);
        
        try {
            SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
            Aggregations aggregations = response.getAggregations();
            Terms brandTerms = aggregations.get("brand_agg");
            List<? extends Terms.Bucket> buckets = brandTerms.getBuckets();
            for (Terms.Bucket bucket : buckets) {
                String brand = bucket.getKeyAsString();
                brandList.add(brand);
            }
        } catch (IOException e) {
            log.error("发送请求异常", e);
        }
        
        return brandList;
    }
}
