package com.hmall.search.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.CategoryAndBrandVo;

import java.util.List;

public interface ISearchService extends IService<Item> {

    PageDTO<ItemDoc> EsSearch(ItemPageQuery query);

    CategoryAndBrandVo getFilters(ItemPageQuery query);

    List<String> getBrands(ItemPageQuery query);
}
