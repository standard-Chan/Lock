package com.jeong.service;

import com.jeong.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsersService {

    private UsersRepository participantRepository;


}
