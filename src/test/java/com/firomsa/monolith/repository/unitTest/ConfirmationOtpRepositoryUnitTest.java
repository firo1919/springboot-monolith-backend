package com.firomsa.monolith.repository.unitTest;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import com.firomsa.monolith.model.ConfirmationOTP;
import com.firomsa.monolith.model.User;
import com.firomsa.monolith.repository.ConfirmationOtpRepository;
import com.firomsa.monolith.repository.UserRepository;

@DataJpaTest
public class ConfirmationOtpRepositoryUnitTest {
    @Autowired
    private ConfirmationOtpRepository confirmationOtpRepository;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // add a confirmation otp to the database for testing
        var user = User.builder().firstName("John").lastName("Doe").username("john_doe")
                .password("password123").phone("1234567890").email("john.doe@example.com").build();
        userRepository.save(user);
        ConfirmationOTP confirmationOtp = ConfirmationOTP.builder().otp("123456").user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(5)).build();
        confirmationOtpRepository.save(confirmationOtp);

    }

    @Test
    @DisplayName("should delete all confirmation otps for a given user")
    void shouldDeleteAllByUser() {
        // Arrange
        var user = userRepository.findByUsername("john_doe");
        // Act
        confirmationOtpRepository.deleteAllByUser(user.get());
        // Assert
        var confirmationOtps = user.get().getConfirmationOtps();
        assertThat(confirmationOtps).isEmpty();
    }

    @Test
    @DisplayName("should find confirmation otp by otp and expiresAt after and confirmed false")
    void shouldFindByOtpAndExpiresAtAfterAndConfirmedFalse() {
        // Act
        var confirmationOtp = confirmationOtpRepository
                .findByOtpAndExpiresAtAfterAndConfirmedFalse("123456", LocalDateTime.now());
        // Assert
        assertThat(confirmationOtp).isNotNull();
        assertThat(confirmationOtp.get().getOtp()).isEqualTo("123456");
    }

    @Test
    @DisplayName("should not find confirmation otp by otp and expiresAt after and confirmed false")
    void shouldNotFindByOtpAndExpiresAtAfterAndConfirmedFalse() {
        // Act
        var confirmationOtp = confirmationOtpRepository.findByOtpAndExpiresAtAfterAndConfirmedFalse(
                "123456", LocalDateTime.now().plusDays(1));
        // Assert
        assertThat(confirmationOtp).isEmpty();
    }

    @Test
    @DisplayName("should not find confirmation otp by otp and expiresAt after and confirmed true")
    void shouldNotFindByOtpAndExpiresAtAfterAndConfirmedTrue() {
        // Arrange
        var confirmationOtp = confirmationOtpRepository
                .findByOtpAndExpiresAtAfterAndConfirmedFalse("123456", LocalDateTime.now());
        confirmationOtp.get().setConfirmed(true);
        confirmationOtpRepository.save(confirmationOtp.get());
        // Act

        var newConfirmationOtp = confirmationOtpRepository
                .findByOtpAndExpiresAtAfterAndConfirmedFalse("123456", LocalDateTime.now());
        // Assert
        assertThat(newConfirmationOtp).isEmpty();
    }
}
