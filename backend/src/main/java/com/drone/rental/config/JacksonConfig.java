package com.drone.rental.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 全局 Jackson 配置
 * - 统一 LocalDateTime 序列化/反序列化格式
 * - 支持多种日期时间输入格式
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册 JavaTimeModule 处理标准 Java 8 时间类型
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 用自定义的多格式反序列化器替换默认的 LocalDateTime 反序列化器
        javaTimeModule.addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());

        // 统一输出格式
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(OUTPUT_FORMAT));

        mapper.registerModule(javaTimeModule);

        // 禁用日期序列化写时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 忽略未知属性，避免前端传多余字段报错
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }
}
