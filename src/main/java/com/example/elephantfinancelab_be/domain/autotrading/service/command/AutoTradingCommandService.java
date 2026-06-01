package com.example.elephantfinancelab_be.domain.autotrading.service.command;

import com.example.elephantfinancelab_be.domain.autotrading.dto.req.AutoTradingReqDTO;
import com.example.elephantfinancelab_be.domain.autotrading.dto.res.AutoTradingResDTO;

public interface AutoTradingCommandService {

  AutoTradingResDTO.Session startSession(
      Long userId, String idempotencyKey, AutoTradingReqDTO.StartSession request);

  AutoTradingResDTO.Session stopSession(Long userId, String sessionId);

  AutoTradingResDTO.Readiness getReadiness();
}
