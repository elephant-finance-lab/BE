package com.example.elephantfinancelab_be.domain.autotrading.service.query;

import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;

public interface AutoTradingQueryService {

  AutoTradingResDTO.Session findActiveSession(Long userId);

  AutoTradingResDTO.Session findSession(Long userId, String sessionId);

  AutoTradingResDTO.AiStatus findAiStatus(Long userId, String sessionId);

  AutoTradingResDTO.Readiness findReadiness(Long userId, String requestedBundleId);
}
