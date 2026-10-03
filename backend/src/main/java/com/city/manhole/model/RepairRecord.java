package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 井盖历史维修记录（台账）：维修日期、维修单位、维修内容与费用。
 */
@Data
@Entity
@Table(name = "repair_record")
public class RepairRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long profileId;

    @Column(nullable = false)
    private LocalDateTime repairedAt;

    /** 维修单位 */
    @Column(nullable = false, length = 128)
    private String repairOrg;

    /** 维修内容：更换井盖/重砌井室/更换胶条等 */
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    /** 当时认定的设施权属单位 */
    @Column(length = 128)
    private String ownerOrg;
}
