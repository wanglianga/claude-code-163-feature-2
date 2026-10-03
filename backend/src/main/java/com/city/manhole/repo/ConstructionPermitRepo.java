package com.city.manhole.repo;

import com.city.manhole.model.ConstructionPermit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConstructionPermitRepo extends JpaRepository<ConstructionPermit, Long> {
    List<ConstructionPermit> findByProfileIdOrderByStartedAtDesc(Long profileId);
}
