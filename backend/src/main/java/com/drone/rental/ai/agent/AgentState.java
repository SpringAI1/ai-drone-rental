package com.drone.rental.ai.agent;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentState {

    private String conversationId;
    private String userQuery;

    @Builder.Default
    private AgentStep currentStep = AgentStep.UNDERSTANDING;

    private String understoodNeed;
    private String droneType;
    private Double budget;
    private String useCase;

    @Builder.Default
    private List<Map<String, Object>> candidateDrones = new ArrayList<>();

    private Map<String, Object> recommendedDrone;

    @Builder.Default
    private Map<String, Object> orderDraft = new HashMap<>();

    @Builder.Default
    private List<String> toolExecutionHistory = new ArrayList<>();

    public enum AgentStep {
        UNDERSTANDING,
        QUERYING_STOCK,
        QUERYING_DETAILS,
        RECOMMENDING,
        GENERATING_ORDER,
        COMPLETED
    }

    public void addToolExecution(String toolName, String result) {
        toolExecutionHistory.add("[" + toolName + "] " + result.substring(0, Math.min(result.length(), 100)) + "...");
    }

    public String getStateSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("当前步骤: ").append(currentStep).append("\n");
        sb.append("用户需求: ").append(understoodNeed).append("\n");
        sb.append("无人机类型: ").append(droneType).append("\n");
        sb.append("预算: ").append(budget).append("\n");
        sb.append("使用场景: ").append(useCase).append("\n");
        sb.append("候选无人机数量: ").append(candidateDrones.size()).append("\n");
        sb.append("已推荐无人机: ").append(recommendedDrone != null ? recommendedDrone.get("model") : "无").append("\n");
        return sb.toString();
    }
}