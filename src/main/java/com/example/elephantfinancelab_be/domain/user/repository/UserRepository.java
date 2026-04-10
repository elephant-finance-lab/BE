package com.example.elephantfinancelab_be.domain.user.repository;

import com.example.elephantfinancelab_be.domain.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUuid(UUID uuid);

  boolean existsByPhoneAndIdNot(String phone, Long id);
}
