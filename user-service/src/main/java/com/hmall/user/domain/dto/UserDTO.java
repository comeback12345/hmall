package com.hmall.user.domain.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Schema(description = "新增用户实体")
@Accessors(chain = true)
@TableName("user")
public class UserDTO {

    @Schema(description = "用户名")
    private String name;

    @Schema(description = "手机")
    private String mobile;

    @Schema(description = "用户身份")
    private String role;

    @Schema(description = "用户状态 1-正常，2-冻结")
    private Integer status;

    @Schema(description = "用户余额")
    private Integer balance;
}