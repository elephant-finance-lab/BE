package com.example.elephantfinancelab_be.domain.survey.converter;

import com.example.elephantfinancelab_be.domain.survey.dto.req.SurveyReqDTO;
import com.example.elephantfinancelab_be.domain.survey.dto.res.SurveyResDTO;
import com.example.elephantfinancelab_be.domain.survey.entity.SurveyResponse;
import com.example.elephantfinancelab_be.domain.user.entity.User;

public final class SurveyConverter {

  private SurveyConverter() {}

  public static SurveyResponse toSurveyResponse(User user, SurveyReqDTO.SubmitResponse request) {
    return SurveyResponse.builder()
        .user(user)
        .shortTermLossReaction(request.getShortTermLossReaction())
        .dailyLossReaction(request.getDailyLossReaction())
        .investmentPriority(request.getInvestmentPriority())
        .investmentPeriod(request.getInvestmentPeriod())
        .investmentPurpose(request.getInvestmentPurpose())
        .investmentStyle(request.getInvestmentStyle())
        .interestedIndustries(request.getInterestedIndustries())
        .acceptableLossRange(request.getAcceptableLossRange())
        .build();
  }

  public static SurveyResDTO.SubmitResponse toSubmitResponse(SurveyResponse surveyResponse) {
    return SurveyResDTO.SubmitResponse.builder()
        .surveyResponseId(surveyResponse.getId())
        .createdAt(surveyResponse.getCreatedAt())
        .build();
  }
}
