package com.factoreal.backend.global.security.application;

import com.factoreal.backend.global.security.dao.UserRepository;
import com.factoreal.backend.global.security.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserRepoService {
    private final UserRepository userRepository;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
