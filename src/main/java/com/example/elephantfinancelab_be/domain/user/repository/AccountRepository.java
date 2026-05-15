package com.example.elephantfinancelab_be.domain.user.repository;

import com.example.elephantfinancelab_be.domain.user.entity.Account;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

  List<Account> findAllByUserId(Long userId);

  boolean existsByAccountNumber(String accountNumber);
}
