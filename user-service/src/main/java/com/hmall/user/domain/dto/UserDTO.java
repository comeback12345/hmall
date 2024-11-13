package com.hmall.user.domain.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@ApiModel(description = "新增用户实体")
@Accessors(chain = true)
@TableName("user")
public class UserDTO {

    @ApiModelProperty("用户名")
    private String name;

    @ApiModelProperty("手机")
    private String mobile;

    @ApiModelProperty("用户身份")
    private String role;

    @ApiModelProperty("用户状态 1-正常，2-冻结")
    private Integer status;

    @ApiModelProperty("用户余额")
    private Integer balance;
}
