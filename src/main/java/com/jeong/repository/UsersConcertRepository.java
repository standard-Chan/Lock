package com.jeong.repository;

import com.jeong.domain.Concert;
import com.jeong.domain.UsersConcert;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersConcertRepository extends JpaRepository<UsersConcert, Long> {

    @Query("SELECT COUNT(uc) FROM UsersConcert uc WHERE uc.concert.id = :concert_id")
    int countByConcertId(@Param("concert_id") Long concertId);
}
