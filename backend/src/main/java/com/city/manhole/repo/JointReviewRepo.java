package com.city.manhole.repo;

import com.city.manhole.model.JointReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JointReviewRepo extends JpaRepository<JointReview, Long> {
    List<JointReview> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
    Optional<JointReview> findFirstByIncidentIdOrderByCreatedAtDesc(Long incidentId);
    List<JointReview> findByStatusInOrderByEscalatedAtDesc(List<String> statuses);
    List<JointReview> findAllByOrderByCreatedAtDesc();
}
