package com.ota.integration.repository;

import com.ota.integration.domain.DeadLetter;
import com.ota.integration.domain.DeadLetter.Status;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterRepository extends JpaRepository<DeadLetter, Long> {

    List<DeadLetter> findByStatusIn(List<Status> statuses);

    Optional<DeadLetter> findTopByProviderAndStatusIn(String provider, List<Status> statuses);
}
