# Lock
 Comparison and implementation of optimistic and pessimistic locks



## 설명
티켓 예매에서 발생하는 동시성 문제를 각각의 락 기법을 통해 해결하고 이를 비교



## 1. 직렬. 단순 for문
[ConcertServiceTest.java](src/test/java/com/jeong/test/ConcertServiceTest.java)
> ======= 싱글 스레드 테스트 결과 =======
> 
> 콘서트
> 
> 시도한 사람 : 200
> 
> DB 참가자 수 100/100
> 
> increase 메서드 호출 횟수 : 100





## 2. transcational (REPETABLE-READ)
[ConcertServiceTest.java](src/test/java/com/jeong/test/ConcertServiceTest.java)
transactional만으로 병렬 처리 충돌을 막을 수 있는가?

> ======= 멀티 스레드 테스트 결과
>
> 콘서트
> 
> 시도한 사람 : 200
>
> DB 참가자 수 35/100 (이상적 답 : 100/100)
> 
> increase 메서드 호출 횟수 : 200

### 결과 분석 

실제 MYSQL에서는 병렬적으로 트랜잭션 내부에서 UPDATE SET을 해도 
크게 문제가 없이 반영 되는 것을 확인할 수 있다.
이는 다음 2가지 이유 때문인데, 
- MYSQL의 UPDATE는 자동으로 배타락을 생성함
- UPDATE시에 SELECT해서 최신 데이터를 가져와 UPDATE를 함. 

때문에 충돌 가능성이 낮기 때문이다. (가능성이 낮다는 것이지 충돌하지 않는 것이 아니다)

하지만 JPA에서는 무조건적으로 충돌이 발생한다.
이는 JPA에서 데이터를 영속성 컨텍스트에 저장한 후 사용하기 때문이다.
그렇기 때문에 최신 DB 데이터가 아닌 이전에 DB에서 가져온 데이터를 업데이트하고
끝날때, 영속성 컨텍스트의 데이터를 save하기 때문에
병렬적으로 처리할 경우 원래 값에 비해서 값이 작게 나올 수밖에 없다.


## 트랜잭션에서 발생하는 데드락을 해결하는 방법
데드락은 보통 S-LOCK, X-LOCK에서 많이 발생한다.

### 배경지식

- **S-LOCK**
    - 외래키를 사용하는 경우 발생
  
      MYSQL에서는 INSERT, UPDATE, DELETE 문이 실행되기 전에 해당 ROW에 대한
      S-LOCK을 얻고, COMMIT시에 반환한다.

- **X-LOCK**
    - UPDATE 문을 실행할 때 발생.
    MYSQL에서는 UPDATE문을 실행할 때, 자동으로 해당 ROW에 대한 X-LOCK을 얻는다.
    이후 COMMIT시에 반환된다. (FOR UPDATE로 획득도 가능하다)

[UsersConcertService.java](src/main/java/com/jeong/service/UsersConcertService.java)
[UsersConcertServiceTest.java](src/test/java/com/jeong/test/UsersConcertServiceTest.java)

위 코드의 `deadLockJoinConcert` 메서드는 병렬로 실행시에 데드락이 발생하는 코드이다.
핵심은 JPA의 쓰기 지연 때문인데 구체적으로 설명하자면 다음과 같다.

JPA 쓰기 지연 : save, update 등을 할 때, DB에 바로 반영하지 않고, flush 시점에 반영한다.

flush 시점에 쿼리는 JAVA 코드 순서와 상관 없이 반드시 다음 순서로 진행된다. 
  1. INSERT
  2. UPDATE
  3. DELETE

이제 실제 예시를 통해 데드락이 왜 발생 했고 어떻게 해결하는지를 알아보자.
```java
    /** 데드락 걸리는 코드 */
    @Transactional
    public void deadLockJoinConcert(Long usersId, Long concertId) {
        // 1. SELECT users -> 읽기, MVCC 스냅샷(REPEATABLE-READ), 락 없음
        Users participant = usersRepository.findById(usersId).orElseThrow(() -> new EntityNotFoundException("[ERROR] 유저를 찾을 수 없음"));
        Concert concert = concertRepository.findById(concertId).orElseThrow(() -> new EntityNotFoundException("[ERROR] 콘서트를 찾을 수 없음"));

        // 2. UsersConcert 객체 생성 
        // INSERT문은 실행하지 않는다. -> 메모리상 객체 생성
        UsersConcert usersConcert = UsersConcert
                .builder()
                .concert(concert)
                .users(participant)
                .build();

        // 3. concert 객체의 값 변경 (Dirty Checking 대상 등록됨)
        concert.increaseParticipants();

        // 4. usersConcert 영속성 컨텍스트에 등록된다.
        // INSERT문은 실행하지 않는다. (쓰기 지연)
        usersConcertRepository.save(usersConcert);

        return;
        // 5. 트랜잭션 종료 시점에 flush + commit 발생
        //    이 시점에 DB에 실제 쿼리가 날라가고, 락도 수행된다.
        //
        //    ① INSERT INTO users_concert → 외래키 무결성 확인 위해 concert/user row에 S-LOCK
        //    ② UPDATE concert SET ...    → concert row에 X-LOCK
        //    DELETE는 없으므로 수행 안함.
        
    }
```
위에서 flush, commit 시점에 병렬적으로 S-LOCK이 발생되는 경우,
다른 트랜잭션에서는 UPDATE문 실행이 불가능하다. X-LOCK을 얻을 수 없기 때문이다.

이로인해 S-LOCK에서 병렬적으로 실행되는 트랜잭션 내의 코드 진행이 멈추게 되고, 데드락이 발생한다.

따라서 다음과 같이 해결할 필요가 있다.

**INSERT, UPDATE 사이에 COMMIT을 수행하여 S-LOCK을 해제한다.**

```java
    @Transactional
    public void joinConcert(Long usersId, Long concertId) {
        Users participant = usersRepository.findById(usersId).orElseThrow(() -> new EntityNotFoundException("[ERROR] 유저를 찾을 수 없음"));
        Concert concert = concertRepository.findById(concertId).orElseThrow(() -> new EntityNotFoundException("[ERROR] 콘서트를 찾을 수 없음"));

        concert.increaseParticipants();

        // COMMIT : 변경사항 더티체킹으로 즉시 반영
        concertRepository.flush();

        UsersConcert usersConcert = UsersConcert
                .builder()
                .concert(concert)
                .users(participant)
                .build();

        usersConcertRepository.save(usersConcert);
    }
```

#### 코드 실행 결과
``` text
======= 멀티 스레드 테스트 결과 =======
    잔나비 콘서트
    시도한 사람 : 200
    생성된 사람 : 200
    DB 참가자 수 10/100
    increase 메서드 호출 횟수 : 200
```

실행 결과를 보면, 데드락은 발생하지 않았지만, 여전히 DB에 참가자 수가
제대로 반영되지 않은 모습을 보인다.

#### 왜 이럴까?

이유는 2번과 동일하다.
이를 해결하기 위해서는 명시적으로 락 기법을 사용할 필요가 있다.
MYSQL에서 쿼리 실행 시 지원하는 자동 LOCK으로는 한계가 있다.

#### 해결법
이를 해결하기 위해서는 트랜잭션의 시작과 동시에 LOCK을 걸어야한다.

"이 ROW를 수정할테니까 이 ROW 접근하지마!" 를 해야한다는 것이다.


## LOCK 기법

LOCK은 크게 2가지 기법이 있다.
- Pessimistic Lock (비관적 락)
- Optimistic Lock (낙관적 락)

말 그대로 '비관적 락'은 충돌이 발생할 것으로 예측(비관적으로 예측)하여 락을 명시적으로 거는 기법

'낙관적 락'은 충돌이 발생하지 않을 것이라고 예측하여 락이 아닌 충돌되는 상황에 코드를 처름부터 다시 시작시켜
데이터를 반영하는 기법이다. '락을 걸어서 막는다'라고는 볼 수 없지만... 락과 비슷한 기능을 한다고 볼 수 있다.





