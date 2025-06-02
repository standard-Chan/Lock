package com.jeong.test;

import com.jeong.service.ConcertService;
import com.jeong.domain.Concert;
import com.jeong.repository.ConcertRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@SpringBootTest
class ConcertServiceTest {

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private ConcertService concertService;

    private Concert concert;

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
    }

    @Test
    @DisplayName("싱글 스레드에서 참가자 증가 테스트")
    public void increaseParticipantsInSingleThread() {

        // BeforeEach 영속성 컨텍스트에 저장된 concert 업데이트
        concert = concertRepository.findById(concert.getId()).orElseThrow();

        int participantsNumber = 200;
        int successCount = 0;

        for (int i=0; i<participantsNumber; i++) {
            try {
                concertService.increaseParticipants(concert.getId());
                successCount ++;
            } catch (Exception e) {
                log.info("[Error] : " + e.getMessage());
            }
        }

        // 영속성 컨텍스트에 저장된 concert 업데이트
        concert = concertRepository.findById(concert.getId()).orElseThrow();

        System.out.println("======= 싱글 스레드 테스트 결과 =======");
        System.out.println(concert.getName());
        System.out.println("시도한 사람 : " + participantsNumber);
        System.out.println("DB 참가자 수 " + concert.getCurrentParticipants() + "/" + concert.getMaxParticipants());
        System.out.println("increase 메서드 호출 횟수 : " + successCount);

    }

    @Test
    @DisplayName("REPEATABLE_READ 트랜잭션으로 병렬 동시성 테스트")
    public void increaseParticipantsInTransaction() throws InterruptedException {
        // BeforeEach 영속성 컨텍스트에 저장된 concert 업데이트
        concert = concertRepository.findById(concert.getId()).orElseThrow();

        int participantsNumber = 200;

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch countDownLatch = new CountDownLatch(participantsNumber);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i=0; i<participantsNumber; i++) {
            executor.submit(() -> {
               try {
                   concertService.increaseParticipants(concert.getId());
                   successCount.incrementAndGet();
               } catch (Exception e) {
                   log.error(e.getMessage());
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
        System.out.println("DB 참가자 수 " + concert.getCurrentParticipants() + "/" + concert.getMaxParticipants());
        System.out.println("increase 메서드 호출 횟수 : " + successCount);
    }
}