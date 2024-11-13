package com.hmall.user.controller;


import com.hmall.common.utils.BeanUtils;
import com.hmall.user.domain.dto.LoginFormDTO;
import com.hmall.user.domain.dto.UserDTO;
import com.hmall.user.domain.po.User;
import com.hmall.user.domain.vo.UserLoginVO;
import com.hmall.user.service.IUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Api(tags = "用户相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;

    @ApiOperation("用户登录接口")
    @PostMapping("login")
    public UserLoginVO login(@RequestBody @Validated LoginFormDTO loginFormDTO){
        return userService.login(loginFormDTO);
    }

    @ApiOperation("扣减余额")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pw", value = "支付密码"),
            @ApiImplicitParam(name = "amount", value = "支付金额")
    })
    @PutMapping("/money/deduct")
    public void deductMoney(@RequestParam String pw,@RequestParam Integer amount){
        userService.deductMoney(pw, amount);
    }
    @ApiOperation("新增用户")
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

