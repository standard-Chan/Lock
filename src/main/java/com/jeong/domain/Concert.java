package com.jeong.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Concert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private LocalDateTime concertDate;
    private int maxParticipants;
    private int currentParticipants;

    @Builder
    public Concert(String name, String description, LocalDateTime concertDate, int maxParticipants, int currentParticipants) {
        this.name = name;
        this.description = description;
        this.concertDate = concertDate;
        this.maxParticipants = maxParticipants;
        this.currentParticipants = currentParticipants;
    }

    public void increaseParticipants() {
        if ( this.currentParticipants >= this.maxParticipants ) {
            throw new RuntimeException("정원이 초과되었습니다.");
        }
        this.currentParticipants++;

    }
}
