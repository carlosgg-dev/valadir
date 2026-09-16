package com.valadir.persistence.repository;

import com.valadir.domain.model.AccountStatus;
import com.valadir.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByAccountId(UUID accountId);

    void deleteByAccountId(UUID accountId);

    // Matches the accounts the purge is about to remove, so their profiles go first
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserEntity u WHERE u.accountId IN (SELECT a.id FROM AccountEntity a WHERE a.status = :status AND a.createdAt < :cutoff)")
    void deleteByAccountStatusOlderThan(@Param("status") AccountStatus status, @Param("cutoff") Instant cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserEntity u SET u.fullName = :fullName, u.givenName = :givenName, u.updatedAt = CURRENT_TIMESTAMP WHERE u.accountId = :accountId")
    void updateNamesByAccountId(@Param("accountId") UUID accountId, @Param("fullName") String fullName, @Param("givenName") String givenName
    );
}
