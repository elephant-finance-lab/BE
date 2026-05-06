package com.example.elephantfinancelab_be.domain.survey.dto.req;

import com.example.elephantfinancelab_be.domain.survey.entity.AcceptableLossRange;
import com.example.elephantfinancelab_be.domain.survey.entity.DailyLossReaction;
import com.example.elephantfinancelab_be.domain.survey.entity.InterestedIndustry;
import com.example.elephantfinancelab_be.domain.survey.entity.InvestmentPeriod;
import com.example.elephantfinancelab_be.domain.survey.entity.InvestmentPriority;
import com.example.elephantfinancelab_be.domain.survey.entity.InvestmentPurpose;
import com.example.elephantfinancelab_be.domain.survey.entity.InvestmentStyle;
import com.example.elephantfinancelab_be.domain.survey.entity.ShortTermLossReaction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class SurveyReqDTO {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Schema(description = "선호도 조사 응답 제출 요청")
  public static class SubmitResponse {

    @NotNull(message = "단기 손실 발생 시 대응은 필수입니다.")
    @Schema(
        description =
            "투자 금액이 단기간에 손실이 난다면 어떻게 할지 선택합니다. "
                + "SELL_TO_LIMIT_LOSS: 바로 매도하고 손실을 줄인다, "
                + "WATCH_SITUATION: 상황을 지켜본다, "
                + "ADDITIONAL_BUY: 추가 매수 기회로 본다",
        example = "WATCH_SITUATION")
    private ShortTermLossReaction shortTermLossReaction;

    @NotNull(message = "하루 10% 손실 발생 시 대응은 필수입니다.")
    @Schema(
        description =
            "하루에 10% 손실이 발생한다면 어떻게 할지 선택합니다. "
                + "CLOSE_POSITION: 불안하지만 정리한다, "
                + "HOLD_POSITION: 불안하지만 유지한다, "
                + "SEE_AS_OPPORTUNITY: 오히려 기회라고 생각한다",
        example = "HOLD_POSITION")
    private DailyLossReaction dailyLossReaction;

    @NotNull(message = "투자 우선순위는 필수입니다.")
    @Schema(
        description =
            "투자에서 더 중요한 것을 선택합니다. "
                + "PRINCIPAL_PROTECTION: 원금 보존, "
                + "STABLE_RETURN: 안정적인 수익, "
                + "HIGH_RETURN_POTENTIAL: 높은 수익 가능성",
        example = "STABLE_RETURN")
    private InvestmentPriority investmentPriority;

    @NotNull(message = "투자 기간은 필수입니다.")
    @Schema(
        description =
            "생각하는 투자 기간을 선택합니다. "
                + "WITHIN_ONE_MONTH: 1개월 이내, "
                + "TWO_TO_SIX_MONTHS: 2~6개월, "
                + "OVER_SIX_MONTHS: 6개월 이상",
        example = "TWO_TO_SIX_MONTHS")
    private InvestmentPeriod investmentPeriod;

    @NotNull(message = "투자 목적은 필수입니다.")
    @Schema(
        description =
            "투자 목적을 선택합니다. "
                + "POCKET_MONEY: 용돈, "
                + "ASSET_GROWTH: 자산 증식, "
                + "HIGH_RETURN_PURSUIT: 큰 수익 추구",
        example = "ASSET_GROWTH")
    private InvestmentPurpose investmentPurpose;

    @NotNull(message = "관심 있는 투자 스타일은 필수입니다.")
    @Schema(
        description =
            "관심 있는 투자 스타일을 선택합니다. "
                + "STABLE_BLUE_CHIP: 안정적인 대형주, "
                + "GROWTH_COMPANY: 성장 가능성 있는 기업, "
                + "HIGH_VOLATILITY_STOCK: 변동성이 큰 종목",
        example = "GROWTH_COMPANY")
    private InvestmentStyle investmentStyle;

    @NotEmpty(message = "관심 산업군은 하나 이상 선택해야 합니다.")
    @Size(max = 8, message = "관심 산업군은 8개 이하여야 합니다.")
    @Schema(
        description =
            "관심 산업군을 복수 선택합니다. "
                + "IT_AI, BIO, ENTERTAINMENT, SEMICONDUCTOR, FINANCE, "
                + "CONSUMER_GOODS, SECONDARY_BATTERY, ETC",
        example = "[\"IT_AI\", \"SEMICONDUCTOR\", \"FINANCE\"]")
    private Set<InterestedIndustry> interestedIndustries;

    @NotNull(message = "감수 가능한 손실 범위는 필수입니다.")
    @Schema(
        description =
            "투자 금액 기준으로 감수 가능한 손실 범위를 선택합니다. "
                + "UNDER_FIVE_PERCENT: 5% 이하, "
                + "FIVE_TO_TEN_PERCENT: 5~10%, "
                + "TEN_TO_TWENTY_PERCENT: 10~20%, "
                + "OVER_TWENTY_PERCENT: 20% 이상",
        example = "FIVE_TO_TEN_PERCENT")
    private AcceptableLossRange acceptableLossRange;
  }
}
