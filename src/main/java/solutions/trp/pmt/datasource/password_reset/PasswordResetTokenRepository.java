package solutions.trp.pmt.datasource.password_reset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Integer> {
    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);
}
