package com.firomsa.monolith.v1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.firomsa.monolith.config.BootstrapConfig;
import com.firomsa.monolith.exception.AuthenticationException;
import com.firomsa.monolith.exception.InvalidOtpException;
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
import com.firomsa.monolith.v1.dto.UserResponseDTO;
import com.firomsa.monolith.v1.mapper.UserMapper;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EmailService emailService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private ConfirmationOtpRepository confirmationOtpRepository;
    @Mock
    private JWTAuthService jwtAuthService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private BootstrapConfig bootstrapConfig;
    @Mock
    private JwtDecoder jwtDecoder;

    @InjectMocks
    private AuthService authService;

    private User user;
    private Role role;
    private RegisterRequestDTO registerRequestDTO;
    private UserResponseDTO userResponseDTO;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setName(Roles.EMPLOYEE);

        user = new User();
        user.setUsername("johndoe");
        user.setEmail("john@example.com");
        user.setPassword("encodedPassword");
        user.setRole(role);

        registerRequestDTO = new RegisterRequestDTO("John", "Doe", "johndoe", "password",
                "john@example.com", Roles.EMPLOYEE, "1234567890");

        userResponseDTO = new UserResponseDTO();
        userResponseDTO.setUsername("johndoe");
        userResponseDTO.setEmail("john@example.com");
    }

    @Test
    @DisplayName("Should register new user successfully")
    void create_ShouldRegisterUser() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName(Roles.EMPLOYEE)).thenReturn(Optional.of(role));
        when(userMapper.toModel(registerRequestDTO)).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(confirmationOtpRepository.save(any(ConfirmationOTP.class)))
                .thenReturn(new ConfirmationOTP());
        doNothing().when(emailService).sendOtp(anyString(), anyString());
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);

        // Act
        RegisterResponseDTO response = authService.create(registerRequestDTO);

        // Assert
        assertNotNull(response);
        assertEquals("You have successfully registered, confirm the OTP sent to your email",
                response.message());
        verify(userRepository, times(1)).save(user);
        verify(emailService, times(1)).sendOtp(anyString(), eq("john@example.com"));
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when username exists")
    void create_WhenUsernameExists_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername(registerRequestDTO.username()))
                .thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class,
                () -> authService.create(registerRequestDTO));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login user successfully")
    void login_ShouldReturnTokens() {
        // Arrange
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO("john@example.com", "password");
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtAuthService.generateToken(authentication)).thenReturn("accessToken");
        when(jwtAuthService.generateRefreshToken(authentication)).thenReturn("refreshToken");
        when(authentication.getName()).thenReturn("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any())).thenReturn(null);

        // Act
        LoginResponseDTO response = authService.login(loginRequestDTO);

        // Assert
        assertNotNull(response);
        assertEquals("accessToken", response.accessToken());
        assertEquals("refreshToken", response.refreshToken());
        assertEquals("johndoe", response.username());
        assertEquals("john@example.com", response.email());
        verify(authenticationManager, times(1)).authenticate(any());
        verify(refreshTokenRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should register admin successfully")
    void createAdmin_ShouldRegisterAdmin() {
        // Arrange
        RegisterAdminRequestDTO adminRequest = new RegisterAdminRequestDTO("Admin", "User", "admin",
                "password", "admin@example.com", "1234567890", "valid-token");
        when(bootstrapConfig.getToken()).thenReturn("valid-token");
        when(userRepository.count()).thenReturn(0L);

        Role adminRole = new Role();
        adminRole.setName(Roles.ADMIN);
        when(roleRepository.findByName(Roles.ADMIN)).thenReturn(Optional.of(adminRole));

        when(userMapper.toModel(any(RegisterRequestDTO.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(confirmationOtpRepository.save(any(ConfirmationOTP.class)))
                .thenReturn(new ConfirmationOTP());
        doNothing().when(emailService).sendOtp(anyString(), anyString());
        when(userMapper.toDTO(user)).thenReturn(userResponseDTO);

        // Act
        RegisterResponseDTO response = authService.createAdmin(adminRequest);

        // Assert
        assertNotNull(response);
        assertEquals("You have successfully registered, confirm the OTP sent to your email",
                response.message());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should throw AuthenticationException when bootstrap token is invalid")
    void createAdmin_WhenInvalidToken_ShouldThrowException() {
        // Arrange
        RegisterAdminRequestDTO adminRequest = new RegisterAdminRequestDTO("Admin", "User", "admin",
                "password", "admin@example.com", "1234567890", "invalid-token");
        when(bootstrapConfig.getToken()).thenReturn("valid-token");

        // Act & Assert
        assertThrows(AuthenticationException.class, () -> authService.createAdmin(adminRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should confirm OTP successfully")
    void confirmOtp_ShouldConfirmAndEnableUser() {
        // Arrange
        ConfirmOtpRequestDTO request = new ConfirmOtpRequestDTO("12345", "john@example.com");
        ConfirmationOTP otp = new ConfirmationOTP();
        otp.setOtp("12345");
        otp.setConfirmed(false);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(confirmationOtpRepository.findByOtpAndExpiresAtAfterAndConfirmedFalse(eq("12345"),
                any(LocalDateTime.class))).thenReturn(Optional.of(otp));

        // Act
        ConfirmOtpResponseDTO response = authService.confirmOtp(request);

        // Assert
        assertNotNull(response);
        assertEquals("Successfully confirmed OTP, please login using your email and password",
                response.message());
        verify(userRepository, times(1)).save(user);
        verify(confirmationOtpRepository, times(1)).save(otp);
        verify(confirmationOtpRepository, times(1)).deleteAllByUser(user);
    }

    @Test
    @DisplayName("Should throw InvalidOtpException when OTP is invalid or expired")
    void confirmOtp_WhenInvalidOtp_ShouldThrowException() {
        // Arrange
        ConfirmOtpRequestDTO request = new ConfirmOtpRequestDTO("wrong-otp", "john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(confirmationOtpRepository.findByOtpAndExpiresAtAfterAndConfirmedFalse(eq("wrong-otp"),
                any(LocalDateTime.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidOtpException.class, () -> authService.confirmOtp(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should resend OTP successfully")
    void resendOtp_ShouldGenerateAndSendNewOtp() {
        // Arrange
        ResendOtpRequestDTO request = new ResendOtpRequestDTO("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(confirmationOtpRepository.save(any(ConfirmationOTP.class)))
                .thenReturn(new ConfirmationOTP());
        doNothing().when(emailService).sendOtp(anyString(), anyString());

        // Act
        ResendOtpResponseDTO response = authService.resendOtp(request);

        // Assert
        assertNotNull(response);
        assertEquals("Successfully resent OTP, check your inbox", response.message());
        verify(confirmationOtpRepository, times(1)).save(any(ConfirmationOTP.class));
        verify(emailService, times(1)).sendOtp(anyString(), eq("john@example.com"));
    }

    @Test
    @DisplayName("Should refresh access token successfully")
    void refreshAccessToken_ShouldReturnNewToken() {
        // Arrange
        RefreshTokenRequestDTO request =
                new RefreshTokenRequestDTO("valid-refresh-token", "john@example.com");
        Jwt jwt = mock(Jwt.class);
        UserDetails userDetails = mock(UserDetails.class);
        RefreshToken refreshTokenEntity = new RefreshToken();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(jwtDecoder.decode("valid-refresh-token")).thenReturn(jwt);
        when(jwt.getClaim("type")).thenReturn("REFRESH");
        when(jwt.getSubject()).thenReturn("john@example.com");
        when(refreshTokenRepository.findByTokenAndUser("valid-refresh-token", user))
                .thenReturn(Optional.of(refreshTokenEntity));
        when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtAuthService.generateToken(any(Authentication.class)))
                .thenReturn("new-access-token");

        // Act
        LoginResponseDTO response = authService.refreshAccessToken(request);

        // Assert
        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals("valid-refresh-token", response.refreshToken());
        verify(jwtAuthService, times(1)).generateToken(any(Authentication.class));
    }

    @Test
    @DisplayName("Should logout user successfully")
    void logoutUser_ShouldDeleteRefreshToken() {
        // Arrange
        LogoutRequestDTO request = new LogoutRequestDTO("valid-refresh-token", "john@example.com");
        Jwt jwt = mock(Jwt.class);
        RefreshToken refreshTokenEntity = new RefreshToken();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(jwtDecoder.decode("valid-refresh-token")).thenReturn(jwt);
        when(jwt.getClaim("type")).thenReturn("REFRESH");
        when(refreshTokenRepository.findByTokenAndUser("valid-refresh-token", user))
                .thenReturn(Optional.of(refreshTokenEntity));

        // Act
        LogoutResponseDTO response = authService.logoutUser(request);

        // Assert
        assertNotNull(response);
        assertEquals("Successfully logged out", response.message());
        verify(refreshTokenRepository, times(1)).delete(refreshTokenEntity);
    }
}
