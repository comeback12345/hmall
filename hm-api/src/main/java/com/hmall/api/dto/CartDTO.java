package com.hmall.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "购物车DTO实体")
public class CartDTO {
    @Schema(description = "购物车条目id ")
    private Long id;
    @Schema(description = "sku商品id")
    private Long itemId;
    @Schema(description = "购买数量")
    private Integer num;
    @Schema(description = "商品标题")
    private String name;
    @Schema(description = "价格,单位：分")
    private Integer price;
    @Schema(description = "商品图片")
    private String image;
    @Schema(description = "类目名称")
    private String category;
    @Schema(description = "品牌名称")
    private String brand;
}
