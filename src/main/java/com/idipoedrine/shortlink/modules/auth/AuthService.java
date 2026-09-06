package com.idipoedrine.shortlink.modules.auth;

import com.idipoedrine.shortlink.modules.auth.dto.AuthenticationResponse;
import com.idipoedrine.shortlink.modules.auth.dto.LoginRequest;
import com.idipoedrine.shortlink.modules.auth.dto.RegisterRequest;
import com.idipoedrine.shortlink.modules.auth.dto.RegisterResponse;
import com.idipoedrine.shortlink.modules.user.User;
import com.idipoedrine.shortlink.modules.user.UserService;
import com.idipoedrine.shortlink.security.JwtService;
import com.idipoedrine.shortlink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public RegisterResponse register(RegisterRequest request) {
        User user = userService.registerNewUser(request.email(), request.password());
        return new RegisterResponse(user.getId(), user.getEmail());
    }

    public AuthenticationResponse login(LoginRequest request) {
        // Delegates credential checking to the configured AuthenticationProvider
        // (DaoAuthenticationProvider -> CustomUserDetailsService + PasswordEncoder).
        // A bad password surfaces as AuthenticationException, caught centrally
        // in GlobalExceptionHandler and mapped to 401.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateAccessToken(principal.getUsername());
        return AuthenticationResponse.bearer(token);
    }
}