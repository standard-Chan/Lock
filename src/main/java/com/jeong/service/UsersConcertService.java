package com.jeong.service;

import com.jeong.domain.Concert;
import com.jeong.domain.Users;
import com.jeong.domain.UsersConcert;
import com.jeong.repository.ConcertRepository;
import com.jeong.repository.UsersConcertRepository;
import com.jeong.repository.UsersRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class UsersConcertService {

    private final UsersRepository usersRepository;
    private final ConcertRepository concertRepository;
    private final UsersConcertRepository usersConcertRepository;

    public void joinConcert(Long usersId, Long concertId) {
        Users participant = usersRepository.findById(usersId).orElseThrow(() -> new EntityNotFoundException("[ERROR] 유저를 찾을 수 없음"));
        Concert concert = concertRepository.findById(concertId).orElseThrow(() -> new EntityNotFoundException("[ERROR] 콘서트를 찾을 수 없음"));

        concert.increaseParticipants();

        // COMMIT : 변경사항 더티체킹으로 즉시 반영 (save 안해도 된다.)
        concertRepository.flush();

        UsersConcert usersConcert = UsersConcert
                .builder()
                .concert(concert)
                .users(participant)
                .build();

        usersConcertRepository.save(usersConcert);
    }

    /** 데드락 걸리는 코드 */
    public void deadLockJoinConcert(Long usersId, Long concertId) {
        // 1. SELECT users -> 읽기, MVCC 스냅샷, 락 없음
        Users participant = usersRepository.findById(usersId).orElseThrow(() -> new EntityNotFoundException("[ERROR] 유저를 찾을 수 없음"));
        Concert concert = concertRepository.findById(concertId).orElseThrow(() -> new EntityNotFoundException("[ERROR] 콘서트를 찾을 수 없음"));

        // 2. UsersConcert 객체 생성 (INSERT 아님. 메모리상 객체 생성)
        UsersConcert usersConcert = UsersConcert
                .builder()
                .concert(concert)
                .users(participant)
                .build();

        // 3. concert 객체의 값 변경 (Dirty Checking 대상 등록됨)
        concert.increaseParticipants();

        // 4. usersConcert 영속성 컨텍스트에 등록됨 (쿼리 안 나감)
        usersConcertRepository.save(usersConcert);

        return; // COMMIT
    }


    // 비관적 락 기법
    public void joinConcertWithPessimisticLock(Long usersId, Long concertId) {
        // get pessimistic Lock
        Concert concert = concertRepository.findByIdWithPessimisticLock(concertId).orElseThrow(() -> new EntityNotFoundException("[ERROR] 콘서트를 찾을 수 없음"));
        Users participant = usersRepository.findById(usersId).orElseThrow(() -> new EntityNotFoundException("[ERROR] 유저를 찾을 수 없음"));

        concert.increaseParticipants();

        UsersConcert usersConcert = UsersConcert
                .builder()
                .concert(concert)
                .users(participant)
                .build();

        usersConcertRepository.save(usersConcert);
    }


    // 낙관적 락 기법
    public void joinConcertWithOptimisticLock(Long usersId, Long concertId) {
        // get pessimistic Lock
        Concert concert = concertRepository.findByIdWithOptimisticLock(concertId).orElseThrow(() -> new EntityNotFoundException("[ERROR] 콘서트를 찾을 수 없음"));
        Users participant = usersRepository.findById(usersId).orElseThrow(() -> new EntityNotFoundException("[ERROR] 유저를 찾을 수 없음"));

        concert.increaseParticipants();

        UsersConcert usersConcert = UsersConcert
                .builder()
                .concert(concert)
                .users(participant)
                .build();

        usersConcertRepository.save(usersConcert);
    }

}
