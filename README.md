# Lock
 Comparison and implementation of optimistic and pessimistic locks

## 설명
티켓 예매에서 발생하는 동시성 문제를 각각의 락 기법을 통해 해결하고 이를 비교
[ConcertServiceTest.java](src/test/java/com/jeong/test/ConcertServiceTest.java)
# 직렬. 단순 for문
> ======= 싱글 스레드 테스트 결과 =======
> 
> 콘서트
> 
> 시도한 사람 : 200
> 
> DB 참가자 수 100/100
> 
> increase 메서드 호출 횟수 : 100





## transcational (REPETABLE-READ)
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


## 트랜잭션에서의 데드락 분석
외래키를 사용하여 Concert의 참여 명단에 넣을 경우 발생하는 문제.
