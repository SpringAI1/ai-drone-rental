package com.drone.rental.ai.agent;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {

    private boolean success;
    private String conversationId;
    private Map<String, Object> recommendedDrone;
    private Map<String, Object> orderDraft;
    private String summary;
    private String errorMessage;
}