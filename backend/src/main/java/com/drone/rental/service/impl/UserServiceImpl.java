package com.drone.rental.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.common.Constants;
import com.drone.rental.common.ResultCode;
import com.drone.rental.common.exception.BusinessException;
import com.drone.rental.dto.LoginDTO;
import com.drone.rental.dto.RegisterDTO;
import com.drone.rental.dto.UserUpdateDTO;
import com.drone.rental.entity.User;
import com.drone.rental.mapper.UserMapper;
import com.drone.rental.security.JwtUtil;
import com.drone.rental.security.UserContext;
import com.drone.rental.service.UserService;
import com.drone.rental.vo.LoginVO;
import com.drone.rental.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void register(RegisterDTO dto) {
        User existUser = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(DigestUtil.md5Hex(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRole(Constants.ROLE_USER);
        user.setStatus(Constants.USER_STATUS_NORMAL);
        user.setCreditStatus(Constants.CREDIT_STATUS_NORMAL);

        this.save(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        String encryptedPassword = DigestUtil.md5Hex(dto.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        if (user.getStatus() == Constants.USER_STATUS_DISABLED) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setToken(token);

        return vo;
    }

    @Override
    public LoginVO adminLogin(LoginDTO dto) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        String encryptedPassword = DigestUtil.md5Hex(dto.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        if (user.getRole() != Constants.ROLE_ADMIN) {
            throw new BusinessException("无管理员权限");
        }

        if (user.getStatus() == Constants.USER_STATUS_DISABLED) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setToken(token);

        return vo;
    }

    @Override
    public UserVO getCurrentUserInfo() {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public void updateUserInfo(UserUpdateDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        if (StringUtils.hasText(dto.getNickname())) {
            user.setNickname(dto.getNickname());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            user.setPhone(dto.getPhone());
        }
        if (StringUtils.hasText(dto.getEmail())) {
            user.setEmail(dto.getEmail());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            user.setAvatar(dto.getAvatar());
        }
        if (StringUtils.hasText(dto.getAddress())) {
            user.setAddress(dto.getAddress());
        }

        this.updateById(user);
    }

    @Override
    public void updatePassword(String oldPassword, String newPassword) {
        Long userId = UserContext.getCurrentUserId();
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        String encryptedOldPassword = DigestUtil.md5Hex(oldPassword);
        if (!encryptedOldPassword.equals(user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        user.setPassword(DigestUtil.md5Hex(newPassword));
        this.updateById(user);
    }

    @Override
    public IPage<UserVO> pageUsers(Integer pageNum, Integer pageSize, String keyword, Integer status) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreatedTime);

        IPage<User> userPage = this.page(page, wrapper);

        return userPage.convert(user -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        });
    }

    @Override
    public UserVO getUserDetail(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        if (user.getRole() == Constants.ROLE_ADMIN && status == Constants.USER_STATUS_DISABLED) {
            throw new BusinessException("不能禁用管理员账号");
        }

        user.setStatus(status);
        this.updateById(user);
    }

    @Override
    public void resetPassword(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        user.setPassword(DigestUtil.md5Hex("123456"));
        this.updateById(user);
    }

    @Override
    public void updateCreditStatus(Long userId, Integer creditStatus) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        user.setCreditStatus(creditStatus);
        this.updateById(user);
    }

    @Override
    public void checkUserOperable(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        if (user.getStatus() == Constants.USER_STATUS_DISABLED) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        if (user.getCreditStatus() == Constants.CREDIT_STATUS_BAD) {
            throw new BusinessException(ResultCode.USER_CREDIT_BAD);
        }
    }

    @Override
    public void recharge(java.math.BigDecimal amount) {
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }

        Long userId = UserContext.getCurrentUserId();
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        if (user.getBalance() == null) {
            user.setBalance(java.math.BigDecimal.ZERO);
        }
        user.setBalance(user.getBalance().add(amount));
        this.updateById(user);
    }
}