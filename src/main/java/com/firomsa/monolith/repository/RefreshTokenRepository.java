package com.firomsa.monolith.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.firomsa.monolith.model.RefreshToken;
import com.firomsa.monolith.model.User;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    void deleteAllByUser(User user);

    Optional<RefreshToken> findByToken(String refreshToken);

    Optional<RefreshToken> findByTokenAndUser(String refreshToken, User user);
}
