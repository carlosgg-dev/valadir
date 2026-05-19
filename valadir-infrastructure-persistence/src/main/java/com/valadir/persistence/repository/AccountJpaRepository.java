package com.valadir.persistence.repository;

import com.valadir.domain.model.AccountStatus;
import com.valadir.persistence.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccountEntity a SET a.status = :status WHERE a.id = :id")
    void updateStatusById(@Param("id") UUID id, @Param("status") AccountStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AccountEntity a WHERE a.status = :status AND a.createdAt < :cutoff")
    int deleteByStatusOlderThan(@Param("status") AccountStatus status, @Param("cutoff") Instant cutoff);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccountEntity a SET a.hashedPassword = :hashedPassword WHERE a.id = :id")
    void updatePasswordById(@Param("id") UUID id, @Param("hashedPassword") String hashedPassword);
}
