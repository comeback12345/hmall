package com.hmall.user.controller;


import com.hmall.common.utils.BeanUtils;
import com.hmall.user.domain.dto.LoginFormDTO;
import com.hmall.user.domain.dto.UserDTO;
import com.hmall.user.domain.po.User;
import com.hmall.user.domain.vo.UserLoginVO;
import com.hmall.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "用户相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "用户登录接口")
    @PostMapping("login")
    public UserLoginVO login(@RequestBody @Validated LoginFormDTO loginFormDTO){
        return userService.login(loginFormDTO);
    }

    @Operation(summary = "扣减余额")
    @Parameters({
            @Parameter(name = "pw", description = "支付密码"),
            @Parameter(name = "amount", description = "支付金额")
    })
    @PutMapping("/money/deduct")
    public void deductMoney(@RequestParam String pw,@RequestParam Integer amount){
        userService.deductMoney(pw, amount);
    }
    @Operation(summary = "新增用户")
    @PostMapping
    public void saveItem(@RequestBody UserDTO userdto) {
        // 新增用户
        userdto.setStatus(1).setBalance(1000000);
        User user = BeanUtils.copyBean(userdto, User.class);
        user.setUsername(userdto.getName()).setPassword(passwordEncoder.encode("123"))
                .setCreateTime(LocalDateTime.now()).setUpdateTime(LocalDateTime.now());
        userService.save(user);
    }
}