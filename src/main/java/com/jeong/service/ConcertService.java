package com.jeong.service;

import com.jeong.domain.Concert;
import com.jeong.repository.ConcertRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConcertService {

    private final ConcertRepository concertRepository;

    @Transactional
    public void increaseParticipants(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(()->new EntityNotFoundException("해당 콘서트를 찾을 수 없습니다."));

        concert.increaseParticipants();
        concertRepository.save(concert);
    }
}
