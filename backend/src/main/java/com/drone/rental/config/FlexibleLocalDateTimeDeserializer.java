package com.drone.rental.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 多格式 LocalDateTime 反序列化器
 * 支持常见格式：yyyy-MM-dd HH:mm, yyyy-MM-dd HH:mm:ss, yyyy-MM-dd, ISO 8601 等
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss")
    );

    private static final List<DateTimeFormatter> DATE_ONLY_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    );

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String value = parser.getValueAsString();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String str = value.trim();

        // 先尝试完整日期时间格式
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(str, fmt);
            } catch (Exception ignored) {
            }
        }

        // 再尝试只有日期的格式，补 00:00:00
        for (DateTimeFormatter fmt : DATE_ONLY_FORMATTERS) {
            try {
                return LocalDate.parse(str, fmt).atStartOfDay();
            } catch (Exception ignored) {
            }
        }

        // 最后尝试 ISO 标准解析（兜底）
        try {
            return LocalDateTime.parse(str);
        } catch (Exception e) {
            throw new IOException("无法解析日期时间: " + str + "，请使用 yyyy-MM-dd HH:mm 格式");
        }
    }
}
