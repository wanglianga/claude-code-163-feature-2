package com.city.manhole.repo;

import com.city.manhole.model.RepairRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairRecordRepo extends JpaRepository<RepairRecord, Long> {
    List<RepairRecord> findByProfileIdOrderByRepairedAtDesc(Long profileId);
}
