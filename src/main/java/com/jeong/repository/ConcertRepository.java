package com.jeong.repository;

import com.jeong.domain.Concert;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE) // FOR UPDATE
    @Query("SELECT c FROM Concert c WHERE c.id = :concert_id")
    Optional<Concert> findByIdWithPessimisticLock(@Param("concert_id") Long concertId);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT c FROM Concert c WHERE c.id = :concert_id")
    Optional<Concert> findByIdWithOptimisticLock(@Param("concert_id") Long concertId);
}


