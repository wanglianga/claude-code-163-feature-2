package com.city.manhole.repo;

import com.city.manhole.model.ManholeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManholeProfileRepo extends JpaRepository<ManholeProfile, Long> {
    Optional<ManholeProfile> findByManholeCode(String manholeCode);
    Optional<ManholeProfile> findFirstByAddressContainingOrderByIdAsc(String address);
}
