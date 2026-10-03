package com.city.manhole.repo;

import com.city.manhole.model.EmergencyFundAdvance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmergencyFundAdvanceRepo extends JpaRepository<EmergencyFundAdvance, Long> {
    Optional<EmergencyFundAdvance> findByIncidentId(Long incidentId);
    List<EmergencyFundAdvance> findAllByOrderByCreatedAtDesc();
}
