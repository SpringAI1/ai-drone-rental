package com.drone.rental.ai.agent;

import com.drone.rental.ai.tools.ToolExecutor;
import com.drone.rental.entity.Drone;
import com.drone.rental.mapper.DroneMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DroneRentalAgent {

    private final RestTemplate restTemplate;
    private final ToolExecutor toolExecutor;
    private final DroneMapper droneMapper;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    public DroneRentalAgent(RestTemplate restTemplate, ToolExecutor toolExecutor, DroneMapper droneMapper) {
        this.restTemplate = restTemplate;
        this.toolExecutor = toolExecutor;
        this.droneMapper = droneMapper;
    }

    public AgentResult execute(String userQuery, String conversationId) {
        AgentState state = AgentState.builder()
                .conversationId(conversationId != null ? conversationId : UUID.randomUUID().toString().replace("-", ""))
                .userQuery(userQuery)
                .build();

        try {
            state = understandRequirement(state);
            state = queryStock(state);
            state = recommendDrone(state);
            state = generateOrderDraft(state);
            state.setCurrentStep(AgentState.AgentStep.COMPLETED);

            return AgentResult.builder()
                    .success(true)
                    .conversationId(state.getConversationId())
                    .recommendedDrone(state.getRecommendedDrone())
                    .orderDraft(state.getOrderDraft())
                    .summary(buildSummary(state))
                    .build();

        } catch (Exception e) {
            return AgentResult.builder()
                    .success(false)
                    .conversationId(state.getConversationId())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private AgentState understandRequirement(AgentState state) {
        state.setCurrentStep(AgentState.AgentStep.UNDERSTANDING);

        String prompt = """
                你是一个无人机租赁需求分析专家。请分析用户的需求并提取以下信息：
                
                用户输入：%s
                
                请按JSON格式输出：
                {
                    "understoodNeed": "对用户需求的理解",
                    "droneType": "无人机类型（如：航拍、测绘、农业、巡检）",
                    "budget": 预算金额（每天）,
                    "useCase": "具体使用场景"
                }
                
                如果无法提取某项，请返回null。预算提取数字即可。
                """.formatted(state.getUserQuery());

        String response = callLLM(prompt);
        Map<String, Object> result = parseJson(response);

        state.setUnderstoodNeed((String) result.get("understoodNeed"));
        state.setDroneType((String) result.get("droneType"));
        state.setUseCase((String) result.get("useCase"));

        Object budgetObj = result.get("budget");
        if (budgetObj instanceof Number) {
            state.setBudget(((Number) budgetObj).doubleValue());
        } else if (budgetObj instanceof String) {
            try {
                state.setBudget(Double.parseDouble(((String) budgetObj).replaceAll("[^0-9.]", "")));
            } catch (Exception ignored) {
                state.setBudget(null);
            }
        }

        return state;
    }

    private AgentState queryStock(AgentState state) {
        state.setCurrentStep(AgentState.AgentStep.QUERYING_STOCK);

        String type = state.getDroneType();
        if (type == null || type.isEmpty()) {
            type = "航拍";
        }

        Map<String, Object> params = new HashMap<>();
        params.put("type", type);

        String toolResult = toolExecutor.execute("query_drone_stock", params);
        state.addToolExecution("query_drone_stock", toolResult);

        List<Map<String, Object>> drones = parseDroneStockResult(toolResult);

        if (state.getBudget() != null) {
            drones = drones.stream()
                    .filter(d -> {
                        Object priceObj = d.get("price");
                        if (priceObj instanceof Number) {
                            return ((Number) priceObj).doubleValue() <= state.getBudget();
                        }
                        return true;
                    })
                    .toList();
        }

        state.setCandidateDrones(drones);
        return state;
    }

    private List<Map<String, Object>> parseDroneStockResult(String result) {
        List<Map<String, Object>> drones = new ArrayList<>();
        String[] sections = result.split("【");

        for (String section : sections) {
            if (section.isEmpty()) continue;

            Map<String, Object> drone = new HashMap<>();

            Pattern modelPattern = Pattern.compile("(.+?)】");
            Matcher modelMatcher = modelPattern.matcher(section);
            if (modelMatcher.find()) {
                String modelInfo = modelMatcher.group(1).trim();
                String[] parts = modelInfo.split(" ");
                if (parts.length >= 2) {
                    drone.put("brand", parts[0]);
                    drone.put("model", modelInfo);
                }
            }

            Pattern stockPattern = Pattern.compile("库存：(\\d+)台");
            Matcher stockMatcher = stockPattern.matcher(section);
            if (stockMatcher.find()) {
                drone.put("stock", Integer.parseInt(stockMatcher.group(1)));
            }

            Pattern pricePattern = Pattern.compile("日租金：¥([\\d.]+)/天");
            Matcher priceMatcher = pricePattern.matcher(section);
            if (priceMatcher.find()) {
                drone.put("price", Double.parseDouble(priceMatcher.group(1)));
            }

            Pattern statusPattern = Pattern.compile("状态：(.+)");
            Matcher statusMatcher = statusPattern.matcher(section);
            if (statusMatcher.find()) {
                drone.put("status", statusMatcher.group(1));
            }

            Pattern idPattern = Pattern.compile("ID：(\\d+)");
            Matcher idMatcher = idPattern.matcher(section);
            if (idMatcher.find()) {
                drone.put("id", Long.parseLong(idMatcher.group(1)));
            }

            if (drone.containsKey("model")) {
                drones.add(drone);
            }
        }

        return drones;
    }

    private AgentState recommendDrone(AgentState state) {
        state.setCurrentStep(AgentState.AgentStep.RECOMMENDING);

        List<Map<String, Object>> candidates = state.getCandidateDrones();

        if (candidates.isEmpty()) {
            state.setRecommendedDrone(null);
            return state;
        }

        Map<String, Object> bestDrone = candidates.stream()
                .filter(d -> "在售".equals(d.get("status")))
                .filter(d -> {
                    Object stockObj = d.get("stock");
                    return stockObj instanceof Number && ((Number) stockObj).intValue() > 0;
                })
                .min((a, b) -> {
                    double scoreA = calculateScore(a, state);
                    double scoreB = calculateScore(b, state);
                    return Double.compare(scoreB, scoreA);
                })
                .orElse(candidates.get(0));

        if (bestDrone.containsKey("id")) {
            Map<String, Object> detailParams = new HashMap<>();
            detailParams.put("drone_id", bestDrone.get("id"));
            String detailResult = toolExecutor.execute("query_drone_detail", detailParams);
            state.addToolExecution("query_drone_detail", detailResult);

            Map<String, Object> details = parseDroneDetailResult(detailResult);
            bestDrone.putAll(details);
        }

        state.setRecommendedDrone(bestDrone);
        return state;
    }

    private double calculateScore(Map<String, Object> drone, AgentState state) {
        double score = 0;

        Object priceObj = drone.get("price");
        Object stockObj = drone.get("stock");

        if (priceObj instanceof Number && stockObj instanceof Number) {
            double price = ((Number) priceObj).doubleValue();
            int stock = ((Number) stockObj).intValue();

            if (state.getBudget() != null) {
                if (price <= state.getBudget()) {
                    score += (state.getBudget() - price) / state.getBudget() * 50;
                } else {
                    score += Math.max(0, 50 - (price - state.getBudget()) / state.getBudget() * 50);
                }
            } else {
                score += 30;
            }

            score += Math.min(stock * 2, 30);
        }

        String model = (String) drone.get("model");
        String useCase = state.getUseCase();
        if (model != null && useCase != null) {
            if (model.toLowerCase().contains(useCase.toLowerCase()) ||
                    useCase.contains("婚礼") || useCase.contains("视频")) {
                score += 20;
            }
        }

        return score;
    }

    private Map<String, Object> parseDroneDetailResult(String result) {
        Map<String, Object> details = new HashMap<>();

        Pattern pattern = Pattern.compile("(.+?)：(.+)");
        Matcher matcher = pattern.matcher(result);

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();

            switch (key) {
                case "型号" -> details.put("model", value);
                case "品牌" -> details.put("brand", value);
                case "类型" -> details.put("type", value);
                case "描述" -> details.put("description", value);
                case "日租金" -> details.put("price", Double.parseDouble(value.replaceAll("[^0-9.]", "")));
                case "库存" -> details.put("stock", Integer.parseInt(value.replaceAll("[^0-9]", "")));
                case "续航时间" -> details.put("flightTime", value);
                case "最大载重" -> details.put("maxPayload", value);
                case "最大速度" -> details.put("maxSpeed", value);
                case "最大航程" -> details.put("maxRange", value);
                case "状态" -> details.put("status", value);
                case "上架状态" -> details.put("onShelf", value);
            }
        }

        return details;
    }

    private AgentState generateOrderDraft(AgentState state) {
        state.setCurrentStep(AgentState.AgentStep.GENERATING_ORDER);

        Map<String, Object> drone = state.getRecommendedDrone();
        if (drone == null) {
            return state;
        }

        Map<String, Object> orderDraft = new HashMap<>();
        orderDraft.put("orderNo", "ORD" + System.currentTimeMillis());
        orderDraft.put("droneId", drone.get("id"));
        orderDraft.put("droneModel", drone.get("model"));
        orderDraft.put("droneBrand", drone.get("brand"));
        orderDraft.put("pricePerDay", drone.get("price"));
        orderDraft.put("rentalDays", 1);
        orderDraft.put("totalAmount", drone.get("price"));
        orderDraft.put("depositAmount", ((Number) drone.get("price")).doubleValue() * 2);
        orderDraft.put("useCase", state.getUseCase());
        orderDraft.put("orderStatus", "草稿");

        state.setOrderDraft(orderDraft);
        return state;
    }

    private String buildSummary(AgentState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("您好！根据您的需求，我为您推荐以下方案：\n\n");

        if (state.getRecommendedDrone() != null) {
            Map<String, Object> drone = state.getRecommendedDrone();
            sb.append("【推荐无人机】\n");
            sb.append("型号：").append(drone.get("model")).append("\n");
            sb.append("品牌：").append(drone.get("brand")).append("\n");
            sb.append("类型：").append(drone.get("type")).append("\n");
            sb.append("日租金：¥").append(drone.get("price")).append("/天\n");
            sb.append("库存：").append(drone.get("stock")).append("台\n");

            if (drone.containsKey("description")) {
                sb.append("描述：").append(drone.get("description")).append("\n");
            }
            if (drone.containsKey("flightTime")) {
                sb.append("续航时间：").append(drone.get("flightTime")).append("\n");
            }
            if (drone.containsKey("maxPayload")) {
                sb.append("最大载重：").append(drone.get("maxPayload")).append("\n");
            }

            sb.append("\n");
        }

        if (state.getOrderDraft() != null) {
            Map<String, Object> order = state.getOrderDraft();
            sb.append("【订单草稿】\n");
            sb.append("订单编号：").append(order.get("orderNo")).append("\n");
            sb.append("租赁天数：").append(order.get("rentalDays")).append("天\n");
            sb.append("订单金额：¥").append(order.get("totalAmount")).append("\n");
            sb.append("押金：¥").append(order.get("depositAmount")).append("\n");
            sb.append("使用场景：").append(order.get("useCase")).append("\n");
        }

        sb.append("\n如需调整租赁天数或有其他需求，请随时告诉我！");
        return sb.toString();
    }

    private String callLLM(String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.3);
        body.put("max_tokens", 500);

        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/chat/completions", entity, Map.class);

        if (response.getBody() == null) {
            return "{}";
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        if (choices == null || choices.isEmpty()) {
            return "{}";
        }

        Map<String, Object> choice = choices.get(0);
        Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
        return (String) messageObj.get("content");
    }

    private Map<String, Object> parseJson(String json) {
        Map<String, Object> result = new HashMap<>();
        if (json == null || json.isEmpty()) {
            return result;
        }

        json = json.trim();
        if (json.startsWith("```json")) {
            json = json.substring(7);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }

        int braceCount = 0;
        int start = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"' && !escaped) {
                inString = !inString;
                continue;
            }

            if (inString) continue;

            if (c == '{' || c == '[') {
                braceCount++;
            } else if (c == '}' || c == ']') {
                braceCount--;
            } else if (c == ',' && braceCount == 0) {
                parsePair(json.substring(start, i), result);
                start = i + 1;
            }
        }

        if (start < json.length()) {
            parsePair(json.substring(start), result);
        }

        return result;
    }

    private void parsePair(String pair, Map<String, Object> result) {
        int colonIndex = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < pair.length(); i++) {
            char c = pair.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (inString) continue;

            if (c == ':') {
                colonIndex = i;
                break;
            }
        }

        if (colonIndex <= 0 || colonIndex >= pair.length() - 1) {
            return;
        }

        String key = pair.substring(0, colonIndex).trim();
        if (key.startsWith("\"")) key = key.substring(1);
        if (key.endsWith("\"")) key = key.substring(0, key.length() - 1);

        String value = pair.substring(colonIndex + 1).trim();

        if (value.startsWith("\"")) {
            value = value.substring(1);
        }
        if (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }
        value = value.replace("\\\"", "\"").replace("\\\\", "\\");

        if ("null".equalsIgnoreCase(value)) {
            result.put(key, null);
        } else if ("true".equalsIgnoreCase(value)) {
            result.put(key, true);
        } else if ("false".equalsIgnoreCase(value)) {
            result.put(key, false);
        } else {
            try {
                if (value.contains(".")) {
                    result.put(key, Double.parseDouble(value));
                } else {
                    result.put(key, Long.parseLong(value));
                }
            } catch (NumberFormatException e) {
                result.put(key, value);
            }
        }
    }
}