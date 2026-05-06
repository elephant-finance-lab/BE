package com.example.elephantfinancelab_be.domain.survey.entity;

import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.global.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "survey_responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SurveyResponse extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "short_term_loss_reaction", nullable = false, length = 50)
  private ShortTermLossReaction shortTermLossReaction;

  @Enumerated(EnumType.STRING)
  @Column(name = "daily_loss_reaction", nullable = false, length = 50)
  private DailyLossReaction dailyLossReaction;

  @Enumerated(EnumType.STRING)
  @Column(name = "investment_priority", nullable = false, length = 50)
  private InvestmentPriority investmentPriority;

  @Enumerated(EnumType.STRING)
  @Column(name = "investment_period", nullable = false, length = 50)
  private InvestmentPeriod investmentPeriod;

  @Enumerated(EnumType.STRING)
  @Column(name = "investment_purpose", nullable = false, length = 50)
  private InvestmentPurpose investmentPurpose;

  @Enumerated(EnumType.STRING)
  @Column(name = "investment_style", nullable = false, length = 50)
  private InvestmentStyle investmentStyle;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "survey_response_interested_industries",
      joinColumns = @JoinColumn(name = "survey_response_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "interested_industry", nullable = false, length = 50)
  private Set<InterestedIndustry> interestedIndustries;

  @Enumerated(EnumType.STRING)
  @Column(name = "acceptable_loss_range", nullable = false, length = 50)
  private AcceptableLossRange acceptableLossRange;
}
