package com.hmall.search.service;

import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.po.ItemDoc;

public interface IRecommendService {
    PageDTO<ItemDoc> getRecommendations(Long userId, Integer pageNo, Integer pageSize);
}
