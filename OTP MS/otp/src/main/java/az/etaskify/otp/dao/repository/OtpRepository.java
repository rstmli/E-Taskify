package az.etaskify.otp.dao.repository;

import az.etaskify.otp.dao.entity.OtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, Long> {
    Optional<OtpEntity> findByEmail(String email);
}
