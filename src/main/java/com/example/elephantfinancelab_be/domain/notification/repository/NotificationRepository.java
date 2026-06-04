package com.example.elephantfinancelab_be.domain.notification.repository;

import com.example.elephantfinancelab_be.domain.notification.entity.Notification;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  Optional<Notification> findByIdAndUserId(Long id, Long userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update Notification n set n.read = true, n.readAt = :readAt "
          + "where n.userId = :userId and n.read = false")
  int markAllReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
