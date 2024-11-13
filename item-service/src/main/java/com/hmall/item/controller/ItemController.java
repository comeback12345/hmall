package com.hmall.item.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hmall.api.dto.ItemDTO;
import com.hmall.api.dto.OrderDetailDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.domain.PageQuery;
import com.hmall.common.utils.BeanUtils;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "商品管理相关接口")
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final IItemService itemService;
    private final RabbitTemplate rabbitTemplate;

    @ApiOperation("分页查询商品")
    @GetMapping("/page")
    public PageDTO<ItemDTO> queryItemByPage(PageQuery query) {
        // 1.分页查询
        Page<Item> result = itemService.page(query.toMpPage("update_time", false));
        // 2.封装并返回
        return PageDTO.of(result, ItemDTO.class);
    }

    @ApiOperation("根据id批量查询商品")
    @GetMapping
    public List<ItemDTO> queryItemByIds(@RequestParam List<Long> ids){
        return itemService.queryItemByIds(ids);
    }

    @ApiOperation("根据id查询商品")
    @GetMapping("{id}")
    public ItemDTO queryItemById(@PathVariable Long id) {
        return BeanUtils.copyBean(itemService.getById(id), ItemDTO.class);
    }

    @ApiOperation("新增商品")
    @PostMapping
    public void saveItem(@RequestBody ItemDTO itemdto) throws JsonProcessingException {
        // 新增
        itemdto.setStatus(1);
        Item item = BeanUtils.copyBean(itemdto, Item.class);
        itemService.save(item);
        ItemDoc itemDoc = BeanUtils.copyProperties(item, ItemDoc.class);
        //BeanUtils.copyProperties(item, itemDoc);
        //ObjectMapper objectMapper = new ObjectMapper();
        //String itemIdsJson = objectMapper.writeValueAsString(itemDoc);
        rabbitTemplate.convertAndSend("es.item.direct","item.change",itemDoc,message-> {
            message.getMessageProperties().setHeader("method", "add");
            return message;
        });
    }

    @ApiOperation("更新商品状态")
    @PutMapping("/status/{id}/{status}")
    public void updateItemStatus(@PathVariable Long id, @PathVariable Integer status){
        Item item = new Item();
        item.setId(id);
        item.setStatus(status);
        itemService.updateById(item);
    }

    @ApiOperation("更新商品")
    @PutMapping
    public void updateItem(@RequestBody ItemDTO item) {
        // 不允许修改商品状态，所以强制设置为null，更新时，就会忽略该字段
        item.setStatus(null);
        // 更新
        itemService.updateById(BeanUtils.copyBean(item, Item.class));
        ItemDoc itemDoc = BeanUtils.copyBean(item, ItemDoc.class);
        rabbitTemplate.convertAndSend("es.item.direct","item.change",itemDoc,message-> {
            message.getMessageProperties().setHeader("method", "update");
            return message;
        });
    }

    @ApiOperation("根据id删除商品")
    @DeleteMapping("{id}")
    public void deleteItemById(@PathVariable Long id) {
        itemService.removeById(id);
        ItemDoc itemDoc = new ItemDoc();
        itemDoc.setId(id.toString());
        rabbitTemplate.convertAndSend("es.item.direct","item.change",itemDoc,message-> {
            message.getMessageProperties().setHeader("method", "delete");
            return message;
        });
    }

    @ApiOperation("批量扣减库存")
    @PutMapping("/stock/deduct")
    public void deductStock(@RequestBody List<OrderDetailDTO> items){
        itemService.deductStock(items);
    }

    @ApiOperation("批量恢复库存")
    @PutMapping("/stock/restore")
    public void restoreStock(@RequestBody List<OrderDetailDTO> items){
        itemService.restoreStock(items);
    }
}
