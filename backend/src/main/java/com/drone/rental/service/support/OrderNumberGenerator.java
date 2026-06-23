package com.drone.rental.service.support;

import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Component;

/**
 * 订单号生成器（从 OrderServiceImpl 抽出以降低类大小）
 */
@Component
public class OrderNumberGenerator {

    /**
     * 生成订单号：ORD + 时间戳 + UUID 前 6 位（大写）
     */
    public String generate() {
        return "ORD" + System.currentTimeMillis()
                + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }
}
