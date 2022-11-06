package com.example.dockerjwt.user;

import com.example.dockerjwt.security.JWTToken;
import com.example.dockerjwt.security.JWTUtil;
import com.example.dockerjwt.user.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JWTUtil jwtUtil;

    public static final String USER_1_MAIL = "admin@gmail.com";
    public static final String USER_2_MAIL = "user@gmail.com";
    public static final String USER_PASSWORD = "password";
    public static final String VALID_TOKEN = "valid_token";
    public static final String USER_1_UUID = String.valueOf(UUID.randomUUID());
    public static final String USER_2_UUID = String.valueOf(UUID.randomUUID());
    public static final User USER_2 = new User(USER_2_UUID, USER_2_MAIL, USER_PASSWORD, "ROLE_USER");
    public static final User USER_2_CHANGED_PASSWORD = new User(USER_2_UUID, USER_2_MAIL, "new_password", "ROLE_USER");


    @Test
    void signupUser() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(USER_1_MAIL);
        loginRequest.setPassword(USER_PASSWORD);
        User user = new User(loginRequest.getEmail(), loginRequest.getPassword(), UserRole.ROLE_USER.toString());
        user.setId(USER_1_UUID);
        when(jwtUtil.generateToken(Mockito.anyString())).thenReturn(VALID_TOKEN);
        when(userRepository.addUser(any(User.class))).thenReturn(Optional.of(user));
        JWTToken result = userService.signup(loginRequest);
        assertEquals(USER_1_UUID, result.getUserId());
        assertEquals(VALID_TOKEN, result.getAccessToken());
    }

    @Test
    void loginUser() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(USER_1_MAIL);
        loginRequest.setPassword(USER_PASSWORD);
        loginRequest.setRole(UserRole.ROLE_USER.toString());
        User user = new User(loginRequest.getEmail(), loginRequest.getPassword(), UserRole.ROLE_USER.toString());
        user.setId(USER_1_UUID);
        when(jwtUtil.generateToken(Mockito.anyString())).thenReturn(VALID_TOKEN);
        when(userRepository.getUserByEmail(USER_1_MAIL)).thenReturn(Optional.of(user));
        JWTToken result = userService.login(loginRequest);
        assertEquals(USER_1_UUID, result.getUserId());
        assertEquals(VALID_TOKEN, result.getAccessToken());
    }

    @Test
    void changeUserPassword() {
        when(jwtUtil.generateToken(Mockito.anyString())).thenReturn(VALID_TOKEN);
        when(userRepository.saveUser(any(User.class))).thenReturn(USER_2_CHANGED_PASSWORD);
        JWTToken result = userService.changeUserPassword(USER_2, "new_password");
        assertEquals(USER_2_UUID, result.getUserId());
        assertEquals(VALID_TOKEN, result.getAccessToken());
    }
}
