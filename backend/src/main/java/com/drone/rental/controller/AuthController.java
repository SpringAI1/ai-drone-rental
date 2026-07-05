package com.drone.rental.controller;

import com.drone.rental.common.Result;
import com.drone.rental.common.ResultCode;
import com.drone.rental.dto.LoginDTO;
import com.drone.rental.dto.RegisterDTO;
import com.drone.rental.security.LoginRateLimiter;
import com.drone.rental.service.UserService;
import com.drone.rental.vo.LoginVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private LoginRateLimiter rateLimiter;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto, HttpServletRequest request) {
        if (!rateLimiter.allowAttempt(request)) {
            return Result.error("请求过于频繁，请5分钟后再试");
        }
        try {
            LoginVO vo = userService.login(dto);
            rateLimiter.resetAttempt(request);
            return Result.success(vo);
        } catch (Exception e) {
            throw e;
        }
    }

    @Operation(summary = "管理员登录")
    @PostMapping("/admin/login")
    public Result<LoginVO> adminLogin(@Validated @RequestBody LoginDTO dto, HttpServletRequest request) {
        if (!rateLimiter.allowAttempt(request)) {
            return Result.error("请求过于频繁，请5分钟后再试");
        }
        try {
            LoginVO vo = userService.adminLogin(dto);
            rateLimiter.resetAttempt(request);
            return Result.success(vo);
        } catch (Exception e) {
            throw e;
        }
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT无状态，客户端清除Token即可
        return Result.success();
    }
}
