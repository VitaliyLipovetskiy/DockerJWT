package com.example.dockerjwt.user;

import com.example.dockerjwt.security.JWTToken;
import com.example.dockerjwt.security.SecurityUser;
import com.example.dockerjwt.user.dto.LoginRequest;
import com.example.dockerjwt.user.dto.PasswordDto;
import com.example.dockerjwt.user.dto.UserTo;
import com.example.dockerjwt.validation.ValidationErrorBuilder;
import com.example.dockerjwt.validation.exceptions.ApplicationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@Slf4j
@RequestMapping(value = UserController.REST_URL, produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class UserController {

    static final String REST_URL = "/users";
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    private final ModelMapper modelMapper;

    public UserController(UserService userService, AuthenticationManager authenticationManager, ModelMapper modelMapper) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.modelMapper = modelMapper;
    }

    @Operation(summary = "Login user with email and password to obtain JWT access token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Created the user",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = JWTToken.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Wrong credentials",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content)})
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(HttpServletRequest request, @Valid @RequestBody LoginRequest loginRequest, Errors errors) {
        log.info("authenticate {}", loginRequest);
        if (errors.hasErrors()) {
            log.info("Validation error with request: " + request.getRequestURI());
            return ResponseEntity.badRequest().body(ValidationErrorBuilder.fromBindingErrors(errors));
        }
        UsernamePasswordAuthenticationToken authInputToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());
        try {
            authenticationManager.authenticate(authInputToken);
        } catch (BadCredentialsException ex) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED, "Wrong credentials");
        }
        return ResponseEntity.ok(userService.login(loginRequest));
    }

    @Operation(summary = "Sign up new user to work with API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created the user",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = JWTToken.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Wrong credentials",
                    content = @Content)})
    @PostMapping("/signup")
    @Validated
    public ResponseEntity<?> registerUser(HttpServletRequest request, @RequestBody @Valid LoginRequest loginRequest, Errors errors) {
        log.info("register {}", loginRequest);
        if (errors.hasErrors()) {
            log.info("Validation error with request: " + request.getRequestURI());
            return ResponseEntity.unprocessableEntity().body(ValidationErrorBuilder.fromBindingErrors(errors));
        }
        try {
            UserRole.valueOf(loginRequest.getRole());
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "Not found role");
        }
        return new ResponseEntity<>(userService.signup(loginRequest), HttpStatus.CREATED);
    }

    @Operation(summary = "Get information about current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the user",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserTo.class)) }),
            @ApiResponse(responseCode = "401", description = "Wrong credentials",
                    content = @Content)})
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof SecurityUser) {
                User user = ((SecurityUser) principal).getUser();
                return ResponseEntity.ok(convertToDto(user));
            }
        }
        throw new ApplicationException(HttpStatus.UNAUTHORIZED, "Wrong credentials");
    }

    @Operation(summary = "Change current user password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Changed current user password",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = JWTToken.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid password supplied",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Wrong credentials",
                    content = @Content)
    })
    @PatchMapping("/me")
    public ResponseEntity<?> changeCurrentUserPassword(
            HttpServletRequest request, @Valid @RequestBody PasswordDto passwordDto, Errors errors) {
        log.info("Change current user password");
        if (errors.hasErrors()) {
            log.info("Validation error with request: " + request.getRequestURI());
            return ResponseEntity.badRequest().body(ValidationErrorBuilder.fromBindingErrors(errors));
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof SecurityUser) {
                User user = ((SecurityUser) principal).getUser();
                if (!userService.checkIfValidOldPassword(user, passwordDto.getOldPassword())) {
                    throw new ApplicationException(HttpStatus.BAD_REQUEST, "Invalid password supplied");
                }
                return ResponseEntity.ok(userService.changeUserPassword(user, passwordDto.getNewPassword()));
            }
        }
        throw new ApplicationException(HttpStatus.UNAUTHORIZED, "Wrong credentials");
    }

    private UserTo convertToDto(User user) {
        return modelMapper.map(user, UserTo.class);
    }

}
