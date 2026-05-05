package com.example.elephantfinancelab_be.domain.survey.service.command;

import com.example.elephantfinancelab_be.domain.survey.converter.SurveyConverter;
import com.example.elephantfinancelab_be.domain.survey.dto.req.SurveyReqDTO;
import com.example.elephantfinancelab_be.domain.survey.dto.res.SurveyResDTO;
import com.example.elephantfinancelab_be.domain.survey.entity.SurveyResponse;
import com.example.elephantfinancelab_be.domain.survey.repository.SurveyResponseRepository;
import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.exception.UserException;
import com.example.elephantfinancelab_be.domain.user.exception.code.UserErrorCode;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SurveyCommandServiceImpl implements SurveyCommandService {

  private final UserRepository userRepository;
  private final SurveyResponseRepository surveyResponseRepository;

  @Override
  public SurveyResDTO.SubmitResponse submitResponse(
      Long userId, SurveyReqDTO.SubmitResponse request) {
    User user = findActiveUser(userId);
    SurveyResponse surveyResponse =
        surveyResponseRepository.save(SurveyConverter.toSurveyResponse(user, request));
    return SurveyConverter.toSubmitResponse(surveyResponse);
  }

  private User findActiveUser(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    if (user.isDeleted()) {
      throw new UserException(UserErrorCode.USER_NOT_FOUND);
    }
    return user;
  }
}
