package com.city.manhole.repo;

import com.city.manhole.model.JointReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JointReviewRepo extends JpaRepository<JointReview, Long> {
    Optional<JointReview> findByIncidentId(Long incidentId);
    List<JointReview> findAllByOrderByCreatedAtDesc();
    List<JointReview> findByStatusOrderByCreatedAtDesc(String status);
    List<JointReview> findByStatusInOrderByCreatedAtDesc(Iterable<String> statuses);
}
