package com.valadir.persistence.repository;

import com.valadir.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByAccountId(UUID accountId);

    void deleteByAccountId(UUID accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserEntity u SET u.fullName = :fullName, u.givenName = :givenName WHERE u.accountId = :accountId")
    void updateNamesByAccountId(
        @Param("accountId") UUID accountId,
        @Param("fullName") String fullName,
        @Param("givenName") String givenName
    );
}
