package com.drone.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.entity.AiConfig;
import com.drone.rental.mapper.AiConfigMapper;
import com.drone.rental.service.AiConfigService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AiConfigServiceImpl extends ServiceImpl<AiConfigMapper, AiConfig> implements AiConfigService {

    private static final Logger log = LoggerFactory.getLogger(AiConfigServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;

    public AiConfigServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS ai_config (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "config_key VARCHAR(100) NOT NULL," +
                    "config_value TEXT," +
                    "description TEXT," +
                    "created_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "updated_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
        } catch (Exception e) {
            log.warn("Failed to create ai_config table, may already exist: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE user ADD COLUMN balance DECIMAL(10,2) DEFAULT 0.00 COMMENT '账户余额';");
            log.info("Successfully added balance column to user table");
        } catch (Exception e) {
            log.warn("Failed to add balance column to user table, may already exist: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS notification (" +
                    "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID'," +
                    "user_id BIGINT NOT NULL COMMENT '用户ID'," +
                    "type TINYINT NOT NULL COMMENT '通知类型'," +
                    "title VARCHAR(100) NOT NULL COMMENT '通知标题'," +
                    "content VARCHAR(500) DEFAULT NULL COMMENT '通知内容'," +
                    "business_id BIGINT DEFAULT NULL COMMENT '关联业务ID'," +
                    "read_status TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读'," +
                    "created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                    "deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除'," +
                    "PRIMARY KEY (id)," +
                    "KEY idx_user_id (user_id)," +
                    "KEY idx_read_status (read_status)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            log.info("Successfully created notification table");
        } catch (Exception e) {
            log.warn("Failed to create notification table, may already exist: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE comment ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '父评论ID';");
            log.info("Successfully added parent_id column to comment table");
        } catch (Exception e) {
            log.warn("Failed to add parent_id column to comment table, may already exist: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE rental_order ADD COLUMN payment_method TINYINT DEFAULT NULL COMMENT '支付方式';");
            log.info("Successfully added payment_method column to rental_order table");
        } catch (Exception e) {
            log.warn("Failed to add payment_method column to rental_order table, may already exist: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE rental_order ADD COLUMN pay_time DATETIME DEFAULT NULL COMMENT '支付时间';");
            log.info("Successfully added pay_time column to rental_order table");
        } catch (Exception e) {
            log.warn("Failed to add pay_time column to rental_order table, may already exist: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE rental_order ADD COLUMN ship_time DATETIME DEFAULT NULL COMMENT '发货时间';");
            log.info("Successfully added ship_time column to rental_order table");
        } catch (Exception e) {
            log.warn("Failed to add ship_time column to rental_order table, may already exist: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE rental_order ADD COLUMN receive_time DATETIME DEFAULT NULL COMMENT '收货时间';");
            log.info("Successfully added receive_time column to rental_order table");
        } catch (Exception e) {
            log.warn("Failed to add receive_time column to rental_order table, may already exist: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("ALTER TABLE rental_order ADD COLUMN return_time DATETIME DEFAULT NULL COMMENT '归还时间';");
            log.info("Successfully added return_time column to rental_order table");
        } catch (Exception e) {
            log.warn("Failed to add return_time column to rental_order table, may already exist: {}", e.getMessage());
        }
    }

    private static final String AI_STATUS_KEY = "ai.status.enabled";
    private static final String AI_MAINTENANCE_MESSAGE_KEY = "ai.maintenance.message";

    @Override
    @Cacheable(value = "aiStatus", key = "'ai_enabled'", unless = "#result == null")
    public boolean getAiStatus() {
        AiConfig config = getByKey(AI_STATUS_KEY);
        if (config == null) {
            return true;
        }
        return "true".equals(config.getConfigValue());
    }

    @Override
    @CacheEvict(value = "aiStatus", allEntries = true)
    public void setAiStatus(boolean enabled) {
        AiConfig config = getByKey(AI_STATUS_KEY);
        if (config == null) {
            config = new AiConfig();
            config.setConfigKey(AI_STATUS_KEY);
            config.setDescription("AI助手状态: true-启用, false-维护中");
        }
        config.setConfigValue(String.valueOf(enabled));
        saveOrUpdate(config);
    }

    @Override
    @Cacheable(value = "aiStatus", key = "'ai_maintenance_message'", unless = "#result == null")
    public String getAiMaintenanceMessage() {
        AiConfig config = getByKey(AI_MAINTENANCE_MESSAGE_KEY);
        if (config == null) {
            return "AI助手正在维护中，请稍后再试";
        }
        return config.getConfigValue();
    }

    @Override
    @CacheEvict(value = "aiStatus", allEntries = true)
    public void setAiMaintenanceMessage(String message) {
        AiConfig config = getByKey(AI_MAINTENANCE_MESSAGE_KEY);
        if (config == null) {
            config = new AiConfig();
            config.setConfigKey(AI_MAINTENANCE_MESSAGE_KEY);
            config.setDescription("AI助手维护提示消息");
        }
        config.setConfigValue(message);
        saveOrUpdate(config);
    }

    private AiConfig getByKey(String key) {
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getConfigKey, key);
        return getOne(wrapper);
    }
}