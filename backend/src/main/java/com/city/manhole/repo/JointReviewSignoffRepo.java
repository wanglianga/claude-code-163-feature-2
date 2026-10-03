package com.city.manhole.repo;

import com.city.manhole.model.JointReviewSignoff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JointReviewSignoffRepo extends JpaRepository<JointReviewSignoff, Long> {
    List<JointReviewSignoff> findByReviewIdOrderByIdAsc(Long reviewId);
    Optional<JointReviewSignoff> findByReviewIdAndOwnerId(Long reviewId, Long ownerId);
}
