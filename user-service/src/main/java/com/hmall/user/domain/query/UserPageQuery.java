package com.hmall.user.domain.query;

import com.hmall.common.domain.PageQuery;
import com.hmall.user.enums.UserStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "商品分页查询条件")
public class UserPageQuery extends PageQuery {

    @ApiModelProperty("用户id")
    private Long id;
    @ApiModelProperty("用户名")
    private String name;
    @ApiModelProperty("用户手机号")
    private String phone;
    @ApiModelProperty("用户状态")
    private UserStatus status;
    @ApiModelProperty("连续失败次数")
    private int numberofConsecutiveFailures;
    @ApiModelProperty("最近登录失败时间")
    private LocalDateTime LoginFailedfulTime;
    @ApiModelProperty("最近登录成功时间")
    private LocalDateTime LoginSuccessfulTime;
}