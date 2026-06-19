package com.drone.rental.ai.tools;

import java.util.ArrayList;
import java.util.List;

public class AiTools {

    public static List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> tools = new ArrayList<>();

        tools.add(ToolDefinition.builder()
                .name("query_drone_stock")
                .description("查询无人机库存信息，返回无人机的库存数量和状态")
                .parameters(new ToolParameters(
                        "object",
                        new ToolProperty[] {
                                new ToolProperty("brand", "string", "品牌名称，可选，用于筛选"),
                                new ToolProperty("type", "string", "无人机类型，可选，如：航拍、测绘、农业、巡检"),
                                new ToolProperty("min_stock", "integer", "最小库存数量，可选")
                        },
                        "查询条件（至少提供一个）"
                ))
                .build());

        tools.add(ToolDefinition.builder()
                .name("query_drone_detail")
                .description("查询无人机详细信息，包括价格、配置参数等")
                .parameters(new ToolParameters(
                        "object",
                        new ToolProperty[] {
                                new ToolProperty("drone_id", "integer", "无人机ID，必填")
                        },
                        "查询参数"
                ))
                .build());

        tools.add(ToolDefinition.builder()
                .name("query_order_status")
                .description("查询订单状态信息")
                .parameters(new ToolParameters(
                        "object",
                        new ToolProperty[] {
                                new ToolProperty("order_no", "string", "订单编号，必填")
                        },
                        "查询参数"
                ))
                .build());

        tools.add(ToolDefinition.builder()
                .name("query_user_orders")
                .description("查询用户的所有订单记录")
                .parameters(new ToolParameters(
                        "object",
                        new ToolProperty[] {
                                new ToolProperty("user_id", "integer", "用户ID，可选，管理员可查任意用户"),
                                new ToolProperty("order_status", "integer", "订单状态筛选，可选：0-待支付, 1-已支付, 2-租赁中, 3-已归还, 4-已取消, 5-已退款"),
                                new ToolProperty("limit", "integer", "返回记录数量，默认10，最大50")
                        },
                        "查询参数"
                ))
                .build());

        tools.add(ToolDefinition.builder()
                .name("query_maintenance_records")
                .description("查询维修记录")
                .parameters(new ToolParameters(
                        "object",
                        new ToolProperty[] {
                                new ToolProperty("drone_id", "integer", "无人机ID，可选"),
                                new ToolProperty("ticket_no", "string", "工单编号，可选"),
                                new ToolProperty("status", "integer", "维修状态，可选：0-待维修, 1-维修中, 2-已完成, 3-已取消"),
                                new ToolProperty("limit", "integer", "返回记录数量，默认10，最大50")
                        },
                        "查询参数"
                ))
                .build());

        return tools;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ToolDefinition {
        private String name;
        private String description;
        private ToolParameters parameters;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ToolParameters {
        private String type;
        private ToolProperty[] properties;
        private String required;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ToolProperty {
        private String name;
        private String type;
        private String description;
    }
}