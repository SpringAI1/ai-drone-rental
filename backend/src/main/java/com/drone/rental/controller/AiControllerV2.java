package com.drone.rental.controller;

import com.drone.rental.common.Result;
import com.drone.rental.entity.AiChatMessage;
import com.drone.rental.security.UserContext;
import com.drone.rental.service.AiChatMessageService;
import com.drone.rental.service.AiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/ai/v2")
@RequiredArgsConstructor
public class AiControllerV2 {

    private final AiConfigService aiConfigService;
    private final AiChatMessageService aiChatMessageService;
    private final RestTemplate restTemplate;

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

    private static final String SYSTEM_PROMPT =
            "你是一个专业、友好的无人机租赁平台客服助手。\n" +
            "你需要用简洁、自然的中文回答用户的问题。\n" +
            "【业务范围】\n" +
            "1. 设备租金：入门机型（Mini 4 Pro 级别）约 ¥99/天，专业航拍（Air 2S 级别）约 ¥199/天，旗舰机型（Mavic 3 Pro 级别）约 ¥399/天。\n" +
            "2. 押金：入门机型 ¥2000，专业机型 ¥5000，旗舰机型 ¥10000。无损归还后 24 小时内退还。\n" +
            "3. 租赁流程：浏览 → 选择天数 → 下单 → 支付 → 顺丰发货 → 使用 → 申请归还 → 快递揽件 → 验收退款。\n" +
            "4. 支付方式：微信支付、支付宝、账户余额。\n" +
            "5. 物流：顺丰速运，同城次日达，一二线城市 1-2 天。\n" +
            "6. 故障处理：保留证据，提交故障报修，非人为损坏平台承担维修费用。\n" +
            "7. 空域备案：飞行需实名登记，超 120 米需申请备案，商业飞行需企业资质。\n" +
            "【推荐机型】\n" +
            "- 旅行/日常拍摄：DJI Mini 4 Pro\n" +
            "- 专业航拍：DJI Air 2S\n" +
            "- 商业拍摄：DJI Mavic 3 Pro\n" +
            "- 农业/巡检：DJI Agras T40\n" +
            "【客服电话】400-800-8888（9:00-21:00）\n\n" +
            "请根据以上信息回答用户问题。如无法精准回答，礼貌引导用户联系人工客服。";

    private static final List<Map.Entry<String, String>> LOCAL_KNOWLEDGE = List.of(
            entry("价格", "设备租金按天计算：入门机型约 ¥99/天，专业航拍约 ¥199/天，旗舰机型约 ¥399/天。\n租赁天数越多越优惠：3 天 9 折，7 天 8.5 折，15 天 8 折，30 天 7 折。"),
            entry("费用", "设备租金按天计算：入门机型约 ¥99/天，专业航拍约 ¥199/天，旗舰机型约 ¥399/天。\n租赁天数越多越优惠：3 天 9 折，7 天 8.5 折，15 天 8 折，30 天 7 折。"),
            entry("租金", "设备租金按天计算：入门机型约 ¥99/天，专业航拍约 ¥199/天，旗舰机型约 ¥399/天。\n租赁天数越多越优惠：3 天 9 折，7 天 8.5 折，15 天 8 折，30 天 7 折。"),
            entry("多少钱", "设备租金按天计算：入门机型约 ¥99/天，专业航拍约 ¥199/天，旗舰机型约 ¥399/天。\n租赁天数越多越优惠：3 天 9 折，7 天 8.5 折，15 天 8 折，30 天 7 折。"),
            entry("流程", "租赁流程：1) 浏览/搜索无人机；2) 选择租赁天数并下单；3) 填写收货地址并支付；4) 顺丰发货次日达；5) 到期按原路寄回。\n押金将在无损归还后 24 小时内原路退还。"),
            entry("怎么租", "租赁流程：1) 浏览/搜索无人机；2) 选择租赁天数并下单；3) 填写收货地址并支付；4) 顺丰发货次日达；5) 到期按原路寄回。"),
            entry("下单", "租赁流程：1) 浏览/搜索无人机；2) 选择租赁天数并下单；3) 填写收货地址并支付；4) 顺丰发货次日达；5) 到期按原路寄回。"),
            entry("押金", "押金金额：入门机型 ¥2000，专业机型 ¥5000，旗舰机型 ¥10000。\n押金会在设备无损归还并验收后 24 小时内原路退还。信用良好用户可减免 80% 押金。"),
            entry("保证金", "押金金额：入门机型 ¥2000，专业机型 ¥5000，旗舰机型 ¥10000。\n押金会在设备无损归还并验收后 24 小时内原路退还。"),
            entry("支付", "支持多种支付方式：微信支付、支付宝、账户余额（充值后可用）。所有支付均有加密保护。"),
            entry("微信", "支持微信支付，支付完成后订单即时生效。"),
            entry("支付宝", "支持支付宝支付，支付完成后订单即时生效。"),
            entry("物流", "默认使用顺丰速运：工作日 14:00 前下单当日发货，一线城市次日达，二线城市 1-2 天，偏远地区 3-5 天。运费由平台承担。"),
            entry("快递", "默认使用顺丰速运发货。工作日 14:00 前下单当日发货。"),
            entry("发货", "顺丰速运发货：工作日 14:00 前下单当日发货，一线城市次日达。"),
            entry("归还", "到期归还：1) 将设备/电池/配件按清单装入原包装；2) 点击订单中的「申请归还」；3) 快递员上门揽件（到付）；4) 也可自行顺丰寄到付件。"),
            entry("退货", "到期归还：1) 将设备按清单装入原包装；2) 点击订单中的「申请归还」；3) 快递员上门揽件。"),
            entry("寄回", "到期归还：点击订单中的「申请归还」，快递员上门揽件。设备无损验收后押金退还。"),
            entry("退款", "订单取消规则：发货前取消 - 全额退款 1 小时到账；已发货未签收 - 支付来回运费；签收后 - 按实际使用天数计费，剩余退还。"),
            entry("取消", "订单取消可在「我的订单」中操作。发货前取消全额退款，发货后按实际使用天数计费。"),
            entry("退订", "订单取消可在「我的订单」中操作。详情可致电 400-800-8888。"),
            entry("推荐", "按场景推荐：\n旅行/日常拍摄 → DJI Mini 4 Pro\n专业航拍 → DJI Air 2S\n商业拍摄 → DJI Mavic 3 Pro\n农业/巡检 → DJI Agras T40"),
            entry("选什么", "按场景推荐：\n旅行/日常拍摄 → DJI Mini 4 Pro\n专业航拍 → DJI Air 2S\n商业拍摄 → DJI Mavic 3 Pro"),
            entry("机型", "热门机型：\nDJI Mini 4 Pro（入门，¥99/天）\nDJI Air 2S（专业，¥199/天）\nDJI Mavic 3 Pro（旗舰，¥399/天）"),
            entry("无人机", "平台提供 DJI 全系列无人机租赁：\nDJI Mini 4 Pro（¥99/天）\nDJI Air 2S（¥199/天）\nDJI Mavic 3 Pro（¥399/天）\nDJI Agras T40（农业/巡检）"),
            entry("故障", "设备故障处理：1) 第一时间联系客服或提交「故障报修」；2) 保留故障时的照片/视频作为证据；3) 非人为损坏由平台承担维修费用；4) 严重故障可申请换机或退款。"),
            entry("坏了", "如设备损坏，请立即提交「故障报修」并保留证据，非人为损坏由平台承担维修费用。"),
            entry("维修", "维修服务：非人为损坏由平台承担维修费用；严重故障可申请换机或退款。"),
            entry("报错", "设备异常请立即联系客服或在「我的 - 故障报修」提交。"),
            entry("空域", "空域备案须知：1) 所有无人机飞行需实名登记；2) 飞行前确认所在区域是否为禁飞区；3) 超过 120 米高度飞行需申请空域备案；4) 商业飞行需取得企业飞行资质。"),
            entry("备案", "空域备案：飞行超 120 米需申请备案。可在平台「空域备案」功能中一键提交。"),
            entry("飞行", "飞行前请确认：实名登记完成，所在区域非禁飞区。超 120 米高度需申请空域备案。"),
            entry("禁飞", "禁飞区包括机场周边、军事管制区、政府重要建筑上空等。飞行前请确认当地飞行规定。"),
            entry("客服", "人工客服联系方式：\n客服电话 400-800-8888（9:00-21:00）\n邮箱 support@drone-rental.com\n紧急问题建议直接致电。"),
            entry("联系", "客服电话 400-800-8888（9:00-21:00）\n邮箱 support@drone-rental.com"),
            entry("电话", "客服电话：400-800-8888（9:00-21:00）。紧急问题建议直接致电。"),
            entry("登录", "账号登录：支持手机号 + 密码 / 验证码登录。忘记密码可通过短信验证码重置。"),
            entry("注册", "账号注册：支持手机号一键注册。注册后立即享受新人首租立减 ¥100 优惠。"),
            entry("账号", "账号相关：注册支持手机号一键注册；登录支持手机号 + 密码/验证码。"),
            entry("密码", "忘记密码：点击登录页「忘记密码」，通过短信验证码即可重置。"),
            entry("优惠", "新人专享：首租立减 ¥100\n邀请好友：好友成功租赁，您得 ¥50 余额\n长租优惠：3 天起 9 折，7 天起 8.5 折"),
            entry("活动", "新人专享：首租立减 ¥100。长租优惠：3 天起 9 折，7 天起 8.5 折。"),
            entry("新人", "新人专享：首租立减 ¥100！立即注册并开始您的首次租赁体验吧 🛸"),
            entry("你好", "您好！我是无人机租赁系统的 AI 客服助手 🛸\n我可以帮您：查询设备信息和价格、了解租赁流程和规则、解答订单和支付问题、推荐适合的无人机。请问有什么可以帮助您的吗？"),
            entry("hi", "您好！我是无人机租赁系统的 AI 客服助手 🛸 请问有什么可以帮助您的吗？"),
            entry("hello", "您好！我是无人机租赁系统的 AI 客服助手 🛸 请问有什么可以帮助您的吗？")
    );

    private static Map.Entry<String, String> entry(String key, String value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    @PostMapping("/chat")
    @Operation(summary = "AI 智能对话（优先本地知识库 + 大模型兜底）")
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

        // 1) 优先走本地知识库匹配（快、免费）
        String localReply = getLocalReply(message);
        boolean matchedLocal = !localReply.equals(defaultReply());

        String reply;
        if (matchedLocal) {
            // 2a) 本地知识库有匹配 → 直接返回
            reply = localReply;
            log.debug("[AI] 本地知识库命中: {}", message);
        } else if (!aiConfigService.getAiStatus()) {
            // 2b) AI 关闭 → 返回本地默认
            reply = defaultReply();
        } else {
            // 2c) 本地未命中且 AI 开启 → 调用大模型
            try {
                reply = callLargeModel(conversationId, message);
                log.debug("[AI] 大模型返回: message={}", message);
            } catch (Exception e) {
                log.warn("[AI] 大模型调用失败，降级至本地回复: {}", e.getMessage());
                reply = defaultReply();
            }
        }

        // 3) 保存对话记录（不影响主流程）
        try {
            aiChatMessageService.saveMessage(conversationId, userId, "user", message, model);
            aiChatMessageService.saveMessage(conversationId, userId, "ai", reply, model);
        } catch (Exception ignored) {
        }

        Map<String, String> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("reply", reply);
        return Result.success(result);
    }

    /**
     * 调用大模型（OpenAI 兼容 API / 阿里云 Dashscope 通义千问）
     */
    private String callLargeModel(String conversationId, String message) {
        // 拉取最近的历史消息作为上下文
        List<AiChatMessage> history = new ArrayList<>();
        try {
            List<AiChatMessage> list = aiChatMessageService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiChatMessage>()
                            .eq(AiChatMessage::getConversationId, conversationId)
                            .orderByDesc(AiChatMessage::getCreatedTime)
                            .last("LIMIT 10")
            );
            for (int i = list.size() - 1; i >= 0; i--) {
                history.add(list.get(i));
            }
        } catch (Exception ignored) {
        }

        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", SYSTEM_PROMPT);
        messages.add(systemMessage);

        for (AiChatMessage hist : history) {
            Map<String, Object> m = new HashMap<>();
            m.put("role", "ai".equals(hist.getRole()) ? "assistant" : hist.getRole());
            m.put("content", hist.getContent());
            messages.add(m);
        }

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.add(userMessage);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.info("[AI] 调用大模型: url={}, model={}", baseUrl + "/chat/completions", model);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/chat/completions", entity, Map.class);

        if (response.getBody() == null) {
            throw new RuntimeException("AI 响应为空");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("AI 响应无 choices");
        }

        Map<String, Object> choice = choices.get(0);
        Map<String, Object> messageObj = (Map<String, Object>) choice.get("message");
        if (messageObj == null) {
            throw new RuntimeException("AI 响应无 message");
        }

        String content = (String) messageObj.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("AI 响应 content 为空");
        }

        return content;
    }

    private String getLocalReply(String message) {
        if (!StringUtils.hasText(message)) {
            return defaultReply();
        }
        String lower = message.toLowerCase();
        for (Map.Entry<String, String> item : LOCAL_KNOWLEDGE) {
            if (lower.contains(item.getKey())) {
                return item.getValue();
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

    @PostMapping("/knowledge/search")
    @Operation(summary = "搜索知识库（本地关键字）")
    public Result<List<String>> searchKnowledge(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (!StringUtils.hasText(query)) {
            return Result.success(List.of());
        }
        String lower = query.toLowerCase();
        List<String> results = new ArrayList<>();
        for (Map.Entry<String, String> item : LOCAL_KNOWLEDGE) {
            if (lower.contains(item.getKey())) {
                results.add(item.getValue());
                if (results.size() >= 5) break;
            }
        }
        if (results.isEmpty()) {
            results.add(defaultReply());
        }
        return Result.success(results);
    }

    @GetMapping("/status")
    @Operation(summary = "获取 AI 服务状态")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", aiConfigService.getAiStatus());
        status.put("maintenanceMessage", aiConfigService.getAiMaintenanceMessage());
        status.put("model", model);
        return Result.success(status);
    }

    @PutMapping("/admin/status")
    @Operation(summary = "管理员设置 AI 服务状态")
    public Result<Void> setStatus(@RequestBody Map<String, Object> request) {
        Boolean enabled = (Boolean) request.get("enabled");
        String maintenanceMessage = (String) request.get("maintenanceMessage");
        if (enabled != null) {
            aiConfigService.setAiStatus(enabled);
        }
        if (maintenanceMessage != null) {
            aiConfigService.setAiMaintenanceMessage(maintenanceMessage);
        }
        return Result.success();
    }
}
