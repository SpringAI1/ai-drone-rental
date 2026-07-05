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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    /** BCrypt 密码编码器 - 替换不安全的 MD5 */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /** 密码最小长度 */
    private static final int MIN_PASSWORD_LENGTH = 6;

    /**
     * 验证密码是否匹配，兼容旧 MD5 格式。
     * 返回 true 表示匹配，调用方可通过 needsMigration() 判断是否需要升级哈希。
     */
    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) return false;
        // BCrypt 哈希以 $2a$、$2b$、$2y$ 开头
        if (storedPassword.startsWith("$2")) {
            return PASSWORD_ENCODER.matches(rawPassword, storedPassword);
        }
        // 兼容旧 MD5 哈希（32 位 hex）
        return DigestUtil.md5Hex(rawPassword).equals(storedPassword);
    }

    /** 判断存储的密码是否为旧 MD5 格式，需要迁移 */
    private boolean needsMigration(String storedPassword) {
        return storedPassword != null && !storedPassword.startsWith("$2");
    }

    /** 密码复杂度校验 */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("密码长度不能少于" + MIN_PASSWORD_LENGTH + "位");
        }
        // 必须包含字母和数字
        boolean hasLetter = false, hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasLetter || !hasDigit) {
            throw new BusinessException("密码必须包含字母和数字");
        }
    }

    @Override
    public void register(RegisterDTO dto) {
        // 密码复杂度校验
        validatePasswordStrength(dto.getPassword());

        User existUser = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PASSWORD_ENCODER.encode(dto.getPassword()));
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

        if (!passwordMatches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 旧 MD5 密码自动迁移到 BCrypt
        if (needsMigration(user.getPassword())) {
            user.setPassword(PASSWORD_ENCODER.encode(dto.getPassword()));
            this.updateById(user);
            log.info("用户 {} 密码已从 MD5 迁移至 BCrypt", user.getId());
        }

        if (user.getStatus() == Constants.USER_STATUS_DISABLED) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        if (user.getRole() != Constants.ROLE_USER) {
            throw new BusinessException("此账号为管理员，请使用管理端登录");
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

        if (!passwordMatches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 旧 MD5 密码自动迁移到 BCrypt
        if (needsMigration(user.getPassword())) {
            user.setPassword(PASSWORD_ENCODER.encode(dto.getPassword()));
            this.updateById(user);
            log.info("管理员 {} 密码已从 MD5 迁移至 BCrypt", user.getId());
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

        if (!passwordMatches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 新密码复杂度校验
        validatePasswordStrength(newPassword);

        user.setPassword(PASSWORD_ENCODER.encode(newPassword));
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
        user.setPassword(PASSWORD_ENCODER.encode("123456"));
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

    @Override
    public void increaseBalance(Long userId, java.math.BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return;
        }
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