package com.valadir.persistence.repository;

import com.valadir.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    void deleteByAccountId(UUID accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserEntity u WHERE u.accountId IN (SELECT a.id FROM AccountEntity a WHERE a.status = 'PENDING_VERIFICATION' AND a.createdAt < :cutoff)")
    void deleteExpiredPendingVerifications(@Param("cutoff") Instant cutoff);
}
