package solutions.trp.pmt.datasource.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBindingRepository extends JpaRepository<UserBindingEntity, Long> {
    Optional<UserBindingEntity> findByUserEntity_IdAndProvider(int userId, String provider);
    Optional<UserBindingEntity> findByUserEntity_IdAndProviderAndActiveTrue(int userId, String provider);
    Optional<UserBindingEntity> findByProviderAndRemoteAccountIdAndActiveTrue(String provider, String remoteAccountId);
    List<UserBindingEntity> findAllByActiveTrue();
}
