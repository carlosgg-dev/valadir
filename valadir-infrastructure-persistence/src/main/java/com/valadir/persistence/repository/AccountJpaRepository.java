package com.valadir.persistence.repository;

import com.valadir.domain.model.AccountStatus;
import com.valadir.domain.model.Language;
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
    @Query("UPDATE AccountEntity a SET a.status = :status, a.updatedAt = CURRENT_TIMESTAMP WHERE a.id = :id")
    void updateStatusById(@Param("id") UUID id, @Param("status") AccountStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccountEntity a SET a.hashedPassword = :hashedPassword, a.updatedAt = CURRENT_TIMESTAMP WHERE a.id = :id")
    void updatePasswordById(@Param("id") UUID id, @Param("hashedPassword") String hashedPassword);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccountEntity a SET a.language = :language, a.updatedAt = CURRENT_TIMESTAMP WHERE a.id = :id")
    void updateLanguageById(@Param("id") UUID id, @Param("language") Language language);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccountEntity a SET a.email = :email, a.updatedAt = CURRENT_TIMESTAMP WHERE a.id = :id")
    void updateEmailById(@Param("id") UUID id, @Param("email") String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM AccountEntity a WHERE a.status = :status AND a.createdAt < :cutoff")
    int deleteByStatusOlderThan(@Param("status") AccountStatus status, @Param("cutoff") Instant cutoff);
}
