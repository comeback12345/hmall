package com.hmall.user.domain.query;

import com.hmall.common.domain.PageQuery;
import com.hmall.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "商品分页查询条件")
public class UserPageQuery extends PageQuery {

    @Schema(description = "用户id")
    private Long id;
    @Schema(description = "用户名")
    private String name;
    @Schema(description = "用户手机号")
    private String phone;
    @Schema(description = "用户状态")
    private UserStatus status;
    @Schema(description = "连续失败次数")
    private int numberofConsecutiveFailures;
    @Schema(description = "最近登录失败时间")
    private LocalDateTime LoginFailedfulTime;
    @Schema(description = "最近登录成功时间")
    private LocalDateTime LoginSuccessfulTime;
}