package com.example.dockerjwt.user;

import com.example.dockerjwt.security.JWTToken;
import com.example.dockerjwt.security.JWTUtil;
import com.example.dockerjwt.user.dto.LoginRequest;
import com.example.dockerjwt.validation.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JWTUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public JWTToken login(LoginRequest loginRequest) {
        User user = getByEmail(loginRequest.getEmail());
        String token = jwtUtil.generateToken(user.getId());
        return new JWTToken(user.getId(), token);
    }

    public JWTToken signup(LoginRequest loginRequest) {
        User user = new User(loginRequest.getEmail(), passwordEncoder.encode(loginRequest.getPassword()), loginRequest.getRole());
        User registratedUser = userRepository.addUser(user).orElseThrow(
                () -> new ApplicationException(HttpStatus.CONFLICT, "Unable to add user")
        );
        String token = jwtUtil.generateToken(registratedUser.getId());
        return new JWTToken(registratedUser.getId(), token);
    }

    public User getUserInfo(String userId) {
        return userRepository.getUser(userId).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, "Not found"));
    }

    public User getByEmail(String email) {
        return userRepository.getUserByEmail(email).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, "Not found"));
    }

    public JWTToken changeUserPassword(User user, String password) {
        user.setPassword(passwordEncoder.encode(password));
        userRepository.saveUser(user);
        String token = jwtUtil.generateToken(user.getId());
        return new JWTToken(user.getId(), token);
    }

    public boolean checkIfValidOldPassword(User user, String oldPassword) {
        return passwordEncoder.matches(oldPassword, user.getPassword());
    }
}
