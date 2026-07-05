package com.drone.rental.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.drone.rental.entity.AiConfig;
import com.drone.rental.mapper.AiConfigMapper;
import com.drone.rental.service.AiConfigService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
@Service
public class AiConfigServiceImpl extends ServiceImpl<AiConfigMapper, AiConfig> implements AiConfigService {

    private static final Logger log = LoggerFactory.getLogger(AiConfigServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;

    public AiConfigServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 启动时增量补齐 schema（idempotent）。
     * 完整 schema 见 resources/sql/init.sqlite.sql。
     * 这里只做轻量级 ALTER（IF NOT EXISTS）兜底，避免启动时刷一屏 MySQL 方言的 SQLITE_ERROR WARN。
     */
    @PostConstruct
    public void init() {
        safeAddColumn("user", "balance", "DECIMAL(10,2) DEFAULT 0.00");
        safeAddColumn("comment", "parent_id", "INTEGER DEFAULT NULL");
        safeAddColumn("rental_order", "payment_method", "INTEGER DEFAULT NULL");
        safeAddColumn("rental_order", "pay_time", "TEXT DEFAULT NULL");
        safeAddColumn("rental_order", "ship_time", "TEXT DEFAULT NULL");
        safeAddColumn("rental_order", "receive_time", "TEXT DEFAULT NULL");
        safeAddColumn("rental_order", "return_time", "TEXT DEFAULT NULL");
    }

    /**
     * 安全添加列：先 PRAGMA table_info 探测列是否存在，避免 SQLite 在重复 ADD COLUMN 时抛错。
     * <p>
     * 注意：SQLite 本身不支持 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`，
     * 旧实现直接拼 `ALTER TABLE ... ADD COLUMN ...`，启动会刷大量 WARN。
     * 改为先查列是否存在后再决定是否执行。
     */
    private void safeAddColumn(String table, String column, String columnDef) {
        try {
            String checkSql = "PRAGMA table_info(" + table + ")";
            java.util.List<java.util.Map<String, Object>> columns = jdbcTemplate.queryForList(checkSql);
            boolean exists = columns.stream()
                    .anyMatch(row -> column.equalsIgnoreCase(String.valueOf(row.get("name"))));
            if (exists) {
                log.debug("Column {}.{} already exists, skip", table, column);
                return;
            }
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + columnDef);
            log.info("Added column {}.{}", table, column);
        } catch (Exception e) {
            log.warn("Failed to add column {}.{}: {}", table, column, e.getMessage());
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