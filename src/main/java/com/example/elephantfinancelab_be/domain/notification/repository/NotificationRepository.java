package com.example.elephantfinancelab_be.domain.notification.repository;

import com.example.elephantfinancelab_be.domain.notification.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  Optional<Notification> findByIdAndUserId(Long id, Long userId);

  List<Notification> findAllByUserIdAndReadFalse(Long userId);
}
