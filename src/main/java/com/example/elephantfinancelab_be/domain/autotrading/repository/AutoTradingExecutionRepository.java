package com.example.elephantfinancelab_be.domain.autotrading.repository;

import com.example.elephantfinancelab_be.domain.autotrading.entity.AutoTradingExecution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoTradingExecutionRepository extends JpaRepository<AutoTradingExecution, Long> {}
