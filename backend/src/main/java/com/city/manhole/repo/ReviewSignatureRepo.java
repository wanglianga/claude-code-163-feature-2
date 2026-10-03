package com.city.manhole.repo;

import com.city.manhole.model.ReviewSignature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewSignatureRepo extends JpaRepository<ReviewSignature, Long> {
    List<ReviewSignature> findByReviewIdOrderByCreatedAtAsc(Long reviewId);
    List<ReviewSignature> findByOwnerIdAndStatusOrderByCreatedAtDesc(Long ownerId, String status);
    long countByReviewIdAndStatus(Long reviewId, String status);
}
