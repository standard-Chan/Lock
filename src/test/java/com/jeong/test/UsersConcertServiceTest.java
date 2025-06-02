package com.jeong.test;

import com.jeong.domain.Concert;
import com.jeong.domain.Users;
import com.jeong.repository.ConcertRepository;
import com.jeong.repository.UsersConcertRepository;
import com.jeong.repository.UsersRepository;
import com.jeong.service.ConcertService;
import com.jeong.service.UsersConcertService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@Slf4j
class UsersConcertServiceTest {
    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private ConcertService concertService;

    private Concert concert;
    private List<Users> participants;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private UsersConcertService usersConcertService;
    @Autowired
    private UsersConcertRepository usersConcertRepository;

    @BeforeEach
    public void init() {
        concert = Concert.builder()
                .name("잔나비 콘서트")
                .description("잔나비의 라스트 콘서트")
                .concertDate(LocalDateTime.now())
                .maxParticipants(100)
                .currentParticipants(0)
                .build();
        concertRepository.save(concert);

        participants = new ArrayList<>();
        for (int i=0; i<200; i++) {
            Users user = new Users("유저"+i);
            participants.add(usersRepository.save(user));
        }
    }

    @Test
    @DisplayName("데드락 테스트")
    public void ConcertJoinTest() throws InterruptedException {
        // BeforeEach 영속성 컨텍스트에 저장된 concert 업데이트
        concert = concertRepository.findById(concert.getId()).orElseThrow();

        int participantsNumber = participants.size();

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch countDownLatch = new CountDownLatch(participantsNumber);
        ExecutorService executor = Executors.newFixedThreadPool(30);

        for (int i=0; i<participantsNumber; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    usersConcertService.deadLockJoinConcert(participants.get(index).getId(), concert.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("참가 실패 {}", e.getMessage());
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        // 대기
        countDownLatch.await();

        concert = concertRepository.findById(concert.getId()).orElseThrow();

        System.out.println("======= 멀티 스레드 테스트 결과 =======");
        System.out.println(concert.getName());
        System.out.println("시도한 사람 : " + participantsNumber);
        System.out.println("생성된 사람 : " + usersConcertRepository.countByConcertId(concert.getId()));
        System.out.println("DB 참가자 수 " + concert.getCurrentParticipants() + "/" + concert.getMaxParticipants());
        System.out.println("increase 메서드 호출 횟수 : " + successCount);
    }
}