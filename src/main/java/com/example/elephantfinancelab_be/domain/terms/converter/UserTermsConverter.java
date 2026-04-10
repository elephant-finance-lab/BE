package com.example.elephantfinancelab_be.domain.terms.converter;

import com.example.elephantfinancelab_be.domain.terms.dto.res.UserTermsResDTO;
import com.example.elephantfinancelab_be.domain.terms.entity.TermsType;
import com.example.elephantfinancelab_be.domain.terms.entity.UserTermsAgreement;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserTermsConverter {

  public static UserTermsResDTO.MyTerms toMyTermsRes(List<UserTermsAgreement> agreements) {
    Map<TermsType, UserTermsAgreement> byType = new EnumMap<>(TermsType.class);
    for (UserTermsAgreement a : agreements) {
      UserTermsAgreement previous = byType.putIfAbsent(a.getTermsType(), a);
      if (previous != null) {
        throw new IllegalStateException(
            "Duplicate UserTermsAgreement for termsType: " + a.getTermsType());
      }
    }
    List<UserTermsResDTO.Item> items =
        Stream.of(TermsType.values())
            .map(
                type -> {
                  UserTermsAgreement row = byType.get(type);
                  if (row == null) {
                    return UserTermsResDTO.Item.builder()
                        .termsType(type)
                        .agreed(false)
                        .agreedAt(null)
                        .build();
                  }
                  return UserTermsResDTO.Item.builder()
                      .termsType(type)
                      .agreed(row.isAgreed())
                      .agreedAt(row.getAgreedAt())
                      .build();
                })
            .collect(Collectors.toList());
    return UserTermsResDTO.MyTerms.builder().items(items).build();
  }
}
