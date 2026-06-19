package com.drone.rental.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.drone.rental.entity.AiConfig;

public interface AiConfigService extends IService<AiConfig> {

    boolean getAiStatus();

    void setAiStatus(boolean enabled);

    String getAiMaintenanceMessage();

    void setAiMaintenanceMessage(String message);
}
