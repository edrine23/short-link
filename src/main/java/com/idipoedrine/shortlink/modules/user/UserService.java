package com.idipoedrine.shortlink.modules.user;

import com.idipoedrine.shortlink.common.exception.EmailAlreadyInUseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerNewUser(String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyInUseException("An account with this email already exists");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.USER)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user {}", saved.getId());
        return saved;
    }
}