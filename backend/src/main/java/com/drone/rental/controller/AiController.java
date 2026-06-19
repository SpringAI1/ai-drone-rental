package com.drone.rental.controller;

import com.drone.rental.ai.agent.AgentResult;
import com.drone.rental.ai.agent.DroneRentalAgent;
import com.drone.rental.ai.tools.AiTools;
import com.drone.rental.ai.tools.ToolExecutor;
import com.drone.rental.common.Result;
import com.drone.rental.dto.AiStatusDTO;
import com.drone.rental.entity.AiChatMessage;
import com.drone.rental.security.UserContext;
import com.drone.rental.service.AiChatMessageService;
import com.drone.rental.service.AiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
public class AiController {

    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final int MAX_TOOL_CALLS = 5;

    private final RestTemplate restTemplate;
    private final AiChatMessageService aiChatMessageService;
    private final ToolExecutor toolExecutor;
    private final DroneRentalAgent droneRentalAgent;
    private final AiConfigService aiConfigService;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature}")
    private double temperature;

    @Value("${spring.ai.openai.chat.options.max-tokens}")
    private int maxTokens;

    public AiController(RestTemplate restTemplate, AiChatMessageService aiChatMessageService, ToolExecutor toolExecutor, DroneRentalAgent droneRentalAgent, AiConfigService aiConfigService) {
        this.restTemplate = restTemplate;
        this.aiChatMessageService = aiChatMessageService;
        this.toolExecutor = toolExecutor;
        this.droneRentalAgent = droneRentalAgent;
        this.aiConfigService = aiConfigService;
    }

    @PostMapping("/chat")
    public Result<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");

        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息内容不能为空");
        }

        String conversationId = request.get("conversationId");
        if (conversationId == null || conversationId.trim().isEmpty()) {
            conversationId = UUID.randomUUID().toString().replace("-", "");
        }

        Long userId = null;
        try {
            userId = UserContext.getCurrentUserId();
        } catch (Exception ignored) {
        }

        try {
            // 优先使用本地知识库 - 稳定无依赖
            String reply = getLocalKnowledgeReply(message);

            try {
                aiChatMessageService.saveMessage(conversationId, userId, "user", message, model);
                aiChatMessageService.saveMessage(conversationId, null, "ai", reply, model);
            } catch (Exception ignored) {
                // 记录失败不影响回复
            }

            Map<String, String> result = new HashMap<>();
            result.put("conversationId", conversationId);
            result.put("reply", reply);
            return Result.success(result);
        } catch (Exception e) {
            System.out.println("AI 回复失败，使用兜底: " + e.getMessage());
            Map<String, String> result = new HashMap<>();
            result.put("conversationId", conversationId);
            result.put("reply", defaultReply());
            return Result.success(result);
        }
    }

    private static final List<Map.Entry<String, String>> LOCAL_KNOWLEDGE = new ArrayList<>();
    static {
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("价格",
                "设备租金按天计算：入门机型约 ¥99/天，专业航拍约 ¥199/天，旗舰机型约 ¥399/天。\n" +
                "租赁天数越多越优惠：3 天 9 折，7 天 8.5 折，15 天 8 折，30 天 7 折。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("费用",
                "设备租金按天计算：入门机型约 ¥99/天，专业航拍约 ¥199/天，旗舰机型约 ¥399/天。\n" +
                "租赁天数越多越优惠：3 天 9 折，7 天 8.5 折，15 天 8 折，30 天 7 折。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("流程",
                "租赁流程：1) 浏览/搜索无人机；2) 选择租赁天数并下单；3) 填写收货地址并支付；4) 顺丰发货次日达；5) 到期按原路寄回。押金将在无损归还后 24 小时内原路退还。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("怎么租",
                "租赁流程：1) 浏览/搜索无人机；2) 选择租赁天数并下单；3) 填写收货地址并支付；4) 顺丰发货次日达；5) 到期按原路寄回。押金将在无损归还后 24 小时内原路退还。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("押金",
                "押金金额：入门机型 ¥2000，专业机型 ¥5000，旗舰机型 ¥10000。\n" +
                "押金会在设备无损归还并验收后 24 小时内原路退还。信用良好用户可减免 80% 押金。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("保证金",
                "押金金额：入门机型 ¥2000，专业机型 ¥5000，旗舰机型 ¥10000。\n" +
                "押金会在设备无损归还并验收后 24 小时内原路退还。信用良好用户可减免 80% 押金。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("支付",
                "支持多种支付方式：微信支付、支付宝、账户余额充值后支付。所有支付均加密保护。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("微信",
                "支持多种支付方式：微信支付、支付宝、账户余额充值后支付。所有支付均加密保护。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("支付宝",
                "支持多种支付方式：微信支付、支付宝、账户余额充值后支付。所有支付均加密保护。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("物流",
                "默认使用顺丰速运：工作日 14:00 前下单当日发货，一线城市次日达，二线城市 1-2 天，偏远地区 3-5 天。运费由平台承担。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("快递",
                "默认使用顺丰速运：工作日 14:00 前下单当日发货，一线城市次日达，二线城市 1-2 天，偏远地区 3-5 天。运费由平台承担。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("发货",
                "默认使用顺丰速运：工作日 14:00 前下单当日发货，一线城市次日达，二线城市 1-2 天，偏远地区 3-5 天。运费由平台承担。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("归还",
                "到期归还：1) 将设备、电池、配件按清单整理入原包装；2) 点击订单中的「申请归还」按钮；3) 快递员上门揽件（到付）；4) 也可自行顺丰寄到付件到指定地址。请保持设备外观完好。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("退货",
                "到期归还：1) 将设备、电池、配件按清单整理入原包装；2) 点击订单中的「申请归还」按钮；3) 快递员上门揽件（到付）；4) 也可自行顺丰寄到付件到指定地址。请保持设备外观完好。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("退款",
                "订单取消规则：发货前取消 - 全额退款 1 小时到账；已发货未签收 - 支付来回运费；签收后 - 按实际使用天数计费，剩余退还。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("取消",
                "订单取消规则：发货前取消 - 全额退款 1 小时到账；已发货未签收 - 支付来回运费；签收后 - 按实际使用天数计费，剩余退还。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("推荐",
                "按场景推荐：旅行/日常拍摄 → DJI Mini 4 Pro；专业航拍 → DJI Air 2S；商业拍摄 → DJI Mavic 3 Pro；农业/巡检 → DJI Agras T40。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("机型",
                "按场景推荐：旅行/日常拍摄 → DJI Mini 4 Pro；专业航拍 → DJI Air 2S；商业拍摄 → DJI Mavic 3 Pro；农业/巡检 → DJI Agras T40。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("无人机",
                "按场景推荐：旅行/日常拍摄 → DJI Mini 4 Pro；专业航拍 → DJI Air 2S；商业拍摄 → DJI Mavic 3 Pro；农业/巡检 → DJI Agras T40。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("故障",
                "设备故障处理：1) 第一时间联系客服或提交「故障报修」；2) 保留故障时的照片/视频作为证据；3) 非人为损坏由平台承担维修费用；4) 严重故障可申请换机或退款。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("维修",
                "设备故障处理：1) 第一时间联系客服或提交「故障报修」；2) 保留故障时的照片/视频作为证据；3) 非人为损坏由平台承担维修费用；4) 严重故障可申请换机或退款。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("坏了",
                "设备故障处理：1) 第一时间联系客服或提交「故障报修」；2) 保留故障时的照片/视频作为证据；3) 非人为损坏由平台承担维修费用；4) 严重故障可申请换机或退款。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("空域",
                "空域备案须知：1) 所有无人机飞行需实名登记；2) 飞行前请确认所在区域是否为禁飞区；3) 超过 120 米高度飞行需申请空域备案；4) 商业飞行需取得企业飞行资质。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("备案",
                "空域备案须知：1) 所有无人机飞行需实名登记；2) 飞行前请确认所在区域是否为禁飞区；3) 超过 120 米高度飞行需申请空域备案；4) 商业飞行需取得企业飞行资质。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("飞行",
                "空域备案须知：1) 所有无人机飞行需实名登记；2) 飞行前请确认所在区域是否为禁飞区；3) 超过 120 米高度飞行需申请空域备案；4) 商业飞行需取得企业飞行资质。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("客服",
                "人工客服联系方式：客服电话 400-800-8888（9:00-21:00）；邮箱 support@drone-rental.com；在线客服：系统右上角「人工客服」按钮。紧急问题建议直接致电。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("电话",
                "人工客服联系方式：客服电话 400-800-8888（9:00-21:00）；邮箱 support@drone-rental.com。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("联系",
                "人工客服联系方式：客服电话 400-800-8888（9:00-21:00）；邮箱 support@drone-rental.com。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("登录",
                "账号相关：注册支持手机号一键注册；登录支持手机号 + 密码 / 验证码登录；忘记密码可通过短信验证码重置。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("注册",
                "账号相关：注册支持手机号一键注册；登录支持手机号 + 密码 / 验证码登录；忘记密码可通过短信验证码重置。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("密码",
                "账号相关：注册支持手机号一键注册；登录支持手机号 + 密码 / 验证码登录；忘记密码可通过短信验证码重置。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("优惠",
                "新人专享：首租立减 ¥100；邀请好友成功租赁，您得 ¥50 余额；长租优惠：3 天起 9 折，7 天起 8.5 折。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("活动",
                "新人专享：首租立减 ¥100；邀请好友成功租赁，您得 ¥50 余额；长租优惠：3 天起 9 折，7 天起 8.5 折。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("新人",
                "新人专享：首租立减 ¥100；邀请好友成功租赁，您得 ¥50 余额；长租优惠：3 天起 9 折，7 天起 8.5 折。"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("你好",
                "您好！我是无人机租赁系统的 AI 客服助手，很高兴为您服务。我可以为您：推荐无人机机型、解答租赁流程规则、解答订单和支付问题、协助故障报修。请问有什么可以帮助您的？"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("hi",
                "您好！我是无人机租赁系统的 AI 客服助手，很高兴为您服务。我可以为您：推荐无人机机型、解答租赁流程规则、解答订单和支付问题、协助故障报修。请问有什么可以帮助您的？"));
        LOCAL_KNOWLEDGE.add(new java.util.AbstractMap.SimpleEntry<>("hello",
                "您好！我是无人机租赁系统的 AI 客服助手，很高兴为您服务。我可以为您：推荐无人机机型、解答租赁流程规则、解答订单和支付问题、协助故障报修。请问有什么可以帮助您的？"));
    }

    private String getLocalKnowledgeReply(String message) {
        if (message == null || message.trim().isEmpty()) {
            return defaultReply();
        }
        String lower = message.toLowerCase();
        for (Map.Entry<String, String> entry : LOCAL_KNOWLEDGE) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return defaultReply();
    }

    private String defaultReply() {
        return "感谢您的咨询！我可以帮您解答：\n" +
                "1) 价格/租金咨询\n" +
                "2) 租赁流程和规则\n" +
                "3) 押金和支付方式\n" +
                "4) 物流配送和归还\n" +
                "5) 设备故障报修\n" +
                "6) 空域备案\n" +
                "7) 机型推荐\n\n" +
                "如仍有疑问，可拨打人工客服：400-800-8888";
    }

    private String chatWithTools(String systemPrompt, List<AiChatMessage> historyMessages, String currentMessage) {
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        for (AiChatMessage histMsg : historyMessages) {
            Map<String, Object> histMap = new HashMap<>();
            String role = "ai".equals(histMsg.getRole()) ? "assistant" : histMsg.getRole();
            histMap.put("role", role);
            histMap.put("content", histMsg.getContent());
            messages.add(histMap);
        }

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", currentMessage);
        messages.add(userMessage);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("messages", messages);

        List<Map<String, Object>> tools = buildToolDefinitions();
        body.put("tools", tools);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        int toolCallCount = 0;
        String finalReply = null;

        while (toolCallCount < MAX_TOOL_CALLS) {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions", entity, Map.class);

            if (response.getBody() == null) {
                return "AI响应为空";
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                return "AI响应为空";
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");

            if (messageObj.containsKey("tool_calls")) {
                List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) messageObj.get("tool_calls");

                Map<String, Object> toolMessage = new HashMap<>();
                toolMessage.put("role", "assistant");
                toolMessage.put("content", "");
                messages.add(toolMessage);

                for (Map<String, Object> toolCall : toolCalls) {
                    Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                    String toolName = (String) function.get("name");
                    String argumentsJson = (String) function.get("arguments");

                    Map<String, Object> arguments = parseJson(argumentsJson);
                    String toolResult = toolExecutor.execute(toolName, arguments);

                    Map<String, Object> toolCallMessage = new HashMap<>();
                    toolCallMessage.put("role", "tool");
                    toolCallMessage.put("tool_call_id", toolCall.get("id"));
                    toolCallMessage.put("content", toolResult);
                    messages.add(toolCallMessage);
                }

                entity = new HttpEntity<>(body, headers);
                toolCallCount++;
            } else {
                finalReply = (String) messageObj.get("content");
                break;
            }
        }

        return finalReply != null ? finalReply : "AI处理超时，请稍后重试";
    }

    private List<Map<String, Object>> buildToolDefinitions() {
        List<AiTools.ToolDefinition> definitions = AiTools.getToolDefinitions();
        return definitions.stream().map(def -> {
            Map<String, Object> tool = new HashMap<>();
            Map<String, Object> function = new HashMap<>();
            function.put("name", def.getName());
            function.put("description", def.getDescription());

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "object");

            Map<String, Map<String, Object>> properties = new HashMap<>();
            List<String> required = new ArrayList<>();

            for (AiTools.ToolProperty prop : def.getParameters().getProperties()) {
                Map<String, Object> propDef = new HashMap<>();
                propDef.put("type", prop.getType());
                propDef.put("description", prop.getDescription());
                properties.put(prop.getName(), propDef);
            }

            String requiredStr = def.getParameters().getRequired();
            if (requiredStr != null && !requiredStr.isEmpty() && !"查询参数（至少提供一个）".equals(requiredStr)) {
                for (String req : requiredStr.split(",")) {
                    required.add(req.trim());
                }
            }

            parameters.put("properties", properties);
            if (!required.isEmpty()) {
                parameters.put("required", required);
            }

            function.put("parameters", parameters);
            tool.put("type", "function");
            tool.put("function", function);
            return tool;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> parseJson(String json) {
        Map<String, Object> result = new HashMap<>();
        if (json == null || json.isEmpty()) {
            return result;
        }

        json = json.trim();
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

        try {
            if (value.contains(".")) {
                result.put(key, Double.parseDouble(value));
            } else {
                result.put(key, Long.parseLong(value));
            }
        } catch (NumberFormatException e) {
            if ("true".equalsIgnoreCase(value)) {
                result.put(key, true);
            } else if ("false".equalsIgnoreCase(value)) {
                result.put(key, false);
            } else if ("null".equalsIgnoreCase(value)) {
                result.put(key, null);
            } else {
                result.put(key, value);
            }
        }

        return;
    }

    @GetMapping("/history/{conversationId}")
    public Result<List<Map<String, Object>>> getHistory(@PathVariable String conversationId) {
        List<AiChatMessage> messages = aiChatMessageService.getRecentMessages(conversationId, MAX_HISTORY_MESSAGES);
        List<Map<String, Object>> result = messages.stream()
                .map(msg -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("role", msg.getRole());
                    map.put("content", msg.getContent());
                    map.put("createdTime", msg.getCreatedTime());
                    return map;
                })
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @DeleteMapping("/conversation/{conversationId}")
    public Result<Void> clearConversation(@PathVariable String conversationId) {
        aiChatMessageService.clearConversation(conversationId);
        return Result.success();
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("AI服务正常");
    }

    @GetMapping("/tools")
    public Result<List<Map<String, Object>>> getTools() {
        return Result.success(buildToolDefinitions());
    }

    @PostMapping("/agent")
    public Result<AgentResult> agent(@RequestBody Map<String, String> request) {
        String message = request.get("message");

        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息内容不能为空");
        }

        if (!aiConfigService.getAiStatus()) {
            return Result.error(aiConfigService.getAiMaintenanceMessage());
        }

        String conversationId = request.get("conversationId");

        AgentResult result = droneRentalAgent.execute(message, conversationId);

        if (result.isSuccess()) {
            return Result.success(result);
        } else {
            return Result.error(result.getErrorMessage());
        }
    }

    @GetMapping("/status")
    @Operation(summary = "获取AI助手状态")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", aiConfigService.getAiStatus());
        result.put("maintenanceMessage", aiConfigService.getAiMaintenanceMessage());
        return Result.success(result);
    }

    @PutMapping("/admin/status")
    @Operation(summary = "管理员更新AI助手状态")
    public Result<Void> updateStatus(@Valid @RequestBody AiStatusDTO dto) {
        aiConfigService.setAiStatus(dto.getEnabled());
        if (dto.getMaintenanceMessage() != null && !dto.getMaintenanceMessage().isEmpty()) {
            aiConfigService.setAiMaintenanceMessage(dto.getMaintenanceMessage());
        }
        return Result.success();
    }
}