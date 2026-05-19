package com.valadir.persistence.repository;

import com.valadir.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByAccountId(UUID accountId);

    void deleteByAccountId(UUID accountId);
}
