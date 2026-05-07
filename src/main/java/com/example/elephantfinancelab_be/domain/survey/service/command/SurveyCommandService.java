package com.example.elephantfinancelab_be.domain.survey.service.command;

import com.example.elephantfinancelab_be.domain.survey.dto.req.SurveyReqDTO;
import com.example.elephantfinancelab_be.domain.survey.dto.res.SurveyResDTO;

public interface SurveyCommandService {

  SurveyResDTO.SubmitResponse submitResponse(Long userId, SurveyReqDTO.SubmitResponse request);
}
