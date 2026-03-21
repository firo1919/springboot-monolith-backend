package com.firomsa.monolith.v1.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.firomsa.monolith.config.BootstrapConfig;
import com.firomsa.monolith.exception.AuthenticationException;
import com.firomsa.monolith.exception.InvalidOtpException;
import com.firomsa.monolith.exception.ResourceNotFoundException;
import com.firomsa.monolith.exception.UserAlreadyExistsException;
import com.firomsa.monolith.model.ConfirmationOTP;
import com.firomsa.monolith.model.RefreshToken;
import com.firomsa.monolith.model.Role;
import com.firomsa.monolith.model.Roles;
import com.firomsa.monolith.model.User;
import com.firomsa.monolith.repository.ConfirmationOtpRepository;
import com.firomsa.monolith.repository.RefreshTokenRepository;
import com.firomsa.monolith.repository.RoleRepository;
import com.firomsa.monolith.repository.UserRepository;
import com.firomsa.monolith.v1.dto.ConfirmOtpRequestDTO;
import com.firomsa.monolith.v1.dto.ConfirmOtpResponseDTO;
import com.firomsa.monolith.v1.dto.LoginRequestDTO;
import com.firomsa.monolith.v1.dto.LoginResponseDTO;
import com.firomsa.monolith.v1.dto.LogoutRequestDTO;
import com.firomsa.monolith.v1.dto.LogoutResponseDTO;
import com.firomsa.monolith.v1.dto.RefreshTokenRequestDTO;
import com.firomsa.monolith.v1.dto.RegisterAdminRequestDTO;
import com.firomsa.monolith.v1.dto.RegisterRequestDTO;
import com.firomsa.monolith.v1.dto.RegisterResponseDTO;
import com.firomsa.monolith.v1.dto.ResendOtpRequestDTO;
import com.firomsa.monolith.v1.dto.ResendOtpResponseDTO;
import com.firomsa.monolith.v1.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final UserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final ConfirmationOtpRepository confirmationOtpRepository;
    private final JWTAuthService jwtAuthService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BootstrapConfig bootstrapConfig;
    private final int OTP_DURATION = 6;
    private final JwtDecoder jwtDecoder;

    @Transactional
    public RegisterResponseDTO create(RegisterRequestDTO registerRequestDTO) {
        if (userRepository.findByUsername(registerRequestDTO.username()).isPresent()) {
            throw new UserAlreadyExistsException(registerRequestDTO.username());
        }

        if (userRepository.findByEmail(registerRequestDTO.email()).isPresent()) {
            throw new UserAlreadyExistsException(registerRequestDTO.email());
        }

        Role role = roleRepository.findByName(registerRequestDTO.role()).orElseThrow(
                () -> new ResourceNotFoundException("Role: " + registerRequestDTO.role().name()));

        User user = userMapper.toModel(registerRequestDTO);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(registerRequestDTO.password()));

        var registeredUser = userRepository.save(user);
        var otp = generateOtp();
        confirmationOtpRepository.save(ConfirmationOTP.builder().otp(otp).user(registeredUser)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_DURATION)).build());
        emailService.sendOtp(otp, user.getEmail());
        var response = new RegisterResponseDTO(userMapper.toDTO(registeredUser),
                "You have successfully registered, confirm the OTP sent to your email");
        return response;
    }

    @Transactional
    public RegisterResponseDTO createAdmin(RegisterAdminRequestDTO registerAdminRequestDTO) {
        // Validate bootstrap token configuration first to avoid revealing deployment state
        if (bootstrapConfig.getToken() == null || bootstrapConfig.getToken().isBlank()) {
            throw new AuthenticationException(
                    "Bootstrap token is not configured. Please set APP_BOOTSTRAP_TOKEN environment variable to enable admin registration.");
        }

        // Validate that request contains a bootstrap token
        if (registerAdminRequestDTO.bootstrapToken() == null
                || registerAdminRequestDTO.bootstrapToken().isBlank()) {
            throw new AuthenticationException("Authentication failed");
        }

        // Use constant-time comparison to prevent timing attacks
        if (!constantTimeEquals(bootstrapConfig.getToken(),
                registerAdminRequestDTO.bootstrapToken())) {
            throw new AuthenticationException("Authentication failed");
        }

        // Check if admin already exists
        if (userRepository.count() > 0) {
            throw new AuthenticationException(
                    "Only one admin can be registered, if you want to create more admins please ask the existing admin to create them");
        }

        Role role = roleRepository.findByName(Roles.ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Role: ADMIN"));

        RegisterRequestDTO registerRequestDTO = new RegisterRequestDTO(
                registerAdminRequestDTO.firstName(), registerAdminRequestDTO.lastName(),
                registerAdminRequestDTO.username(), registerAdminRequestDTO.password(),
                registerAdminRequestDTO.email(), Roles.ADMIN, registerAdminRequestDTO.phone());

        User user = userMapper.toModel(registerRequestDTO);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(registerRequestDTO.password()));

        var registeredUser = userRepository.save(user);
        var otp = generateOtp();
        confirmationOtpRepository.save(ConfirmationOTP.builder().otp(otp).user(registeredUser)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_DURATION)).build());
        emailService.sendOtp(otp, user.getEmail());
        var response = new RegisterResponseDTO(userMapper.toDTO(registeredUser),
                "You have successfully registered, confirm the OTP sent to your email");
        return response;
    }

    @Transactional
    public ConfirmOtpResponseDTO confirmOtp(ConfirmOtpRequestDTO confirmOtpRequestDTO) {
        User user = userRepository.findByEmail(confirmOtpRequestDTO.email())
                .orElseThrow(() -> new ResourceNotFoundException(confirmOtpRequestDTO.email()));
        var otp = confirmationOtpRepository
                .findByOtpAndExpiresAtAfterAndConfirmedFalse(confirmOtpRequestDTO.otp(),
                        LocalDateTime.now())
                .orElseThrow(() -> new InvalidOtpException(
                        "Wrong otp, please use the correct OTP code or ask for a resend"));

        user.setEnabled(true);
        otp.setConfirmed(true);
        userRepository.save(user);
        confirmationOtpRepository.save(otp);
        confirmationOtpRepository.deleteAllByUser(user);
        return new ConfirmOtpResponseDTO(
                "Successfully confirmed OTP, please login using your email and password");
    }

    public String generateOtp() {
        var random = new SecureRandom();
        var numbers = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            numbers.append(random.nextInt(10));
        }

        return numbers.toString();
    }

    public ResendOtpResponseDTO resendOtp(ResendOtpRequestDTO resendOtpRequestDTO) {
        User user = userRepository.findByEmail(resendOtpRequestDTO.email())
                .orElseThrow(() -> new ResourceNotFoundException(resendOtpRequestDTO.email()));
        var otp = generateOtp();
        confirmationOtpRepository.save(ConfirmationOTP.builder().otp(otp).user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_DURATION)).build());
        emailService.sendOtp(otp, user.getEmail());
        return new ResendOtpResponseDTO("Successfully resent OTP, check your inbox");
    }

    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                        loginRequestDTO.email(), loginRequestDTO.password()));
        String accessToken = jwtAuthService.generateToken(authentication);
        String refreshToken = jwtAuthService.generateRefreshToken(authentication);
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(authentication.getName()));
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setToken(refreshToken);

        refreshTokenRepository.save(refreshTokenEntity);

        return new LoginResponseDTO(user.getRole().getName(), accessToken, refreshToken,
                user.getUsername(), user.getEmail());
    }

    @Transactional
    public LoginResponseDTO refreshAccessToken(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        User user = userRepository.findByEmail(refreshTokenRequestDTO.email())
                .orElseThrow(() -> new ResourceNotFoundException(refreshTokenRequestDTO.email()));

        Jwt jwt = jwtDecoder.decode(refreshTokenRequestDTO.refreshToken());
        if (!"REFRESH".equals(jwt.getClaim("type"))) {
            throw new AuthenticationException("Invalid token type");
        }

        refreshTokenRepository.findByTokenAndUser(refreshTokenRequestDTO.refreshToken(), user)
                .orElseThrow(() -> new AuthenticationException(
                        "Refresh token is invalid, please login"));

        String username = jwt.getSubject();
        if (!user.getEmail().equals(username)) {
            throw new AuthenticationException("Token subject does not match provided user");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());
        String newAccessToken = jwtAuthService.generateToken(authentication);

        return new LoginResponseDTO(user.getRole().getName(), newAccessToken,
                refreshTokenRequestDTO.refreshToken(), user.getUsername(), user.getEmail());
    }

    @Transactional
    public LogoutResponseDTO logoutUser(LogoutRequestDTO logoutRequestDTO) {
        User user = userRepository.findByEmail(logoutRequestDTO.email())
                .orElseThrow(() -> new ResourceNotFoundException(logoutRequestDTO.email()));
        Jwt jwt = jwtDecoder.decode(logoutRequestDTO.refreshToken());
        if (!"REFRESH".equals(jwt.getClaim("type"))) {
            throw new AuthenticationException("Invalid token type");
        }
        var token = refreshTokenRepository.findByTokenAndUser(logoutRequestDTO.refreshToken(), user)
                .orElseThrow(() -> new AuthenticationException(
                        "Refresh token is invalid, please login"));

        refreshTokenRepository.delete(token);
        return new LogoutResponseDTO("Successfully logged out");
    }

    private boolean constantTimeEquals(String expected, String actual) {
        // Convert nulls to empty strings to maintain constant-time behavior
        String expectedStr = (expected == null) ? "" : expected;
        String actualStr = (actual == null) ? "" : actual;

        byte[] expectedBytes = expectedStr.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actualStr.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
