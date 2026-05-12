package com.example.elephantfinancelab_be.domain.user.service.query;

import com.example.elephantfinancelab_be.domain.user.dto.res.UserResDTO;
import java.util.List;

public interface AccountQueryService {

  List<UserResDTO.AccountInfo> findAccountList(Long userId);
}
