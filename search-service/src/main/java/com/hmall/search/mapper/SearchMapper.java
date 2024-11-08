package com.hmall.search.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmall.search.domain.po.Item;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 */
@Mapper
public interface SearchMapper extends BaseMapper<Item> {

}
