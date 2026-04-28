package com.example.elephantfinancelab_be.domain.terms.dto.res;

import com.example.elephantfinancelab_be.domain.terms.entity.TermsType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserTermsResDTO {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class MyTerms {

    private List<Item> items;
  }

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Item {

    private TermsType termsType;
    private boolean agreed;
    private LocalDateTime agreedAt;
  }
}
