package com.city.manhole.model;

import com.city.manhole.common.Constants;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 应急基金垫付记录：联合复核仍无法确认权属时，
 * 维修费用先由市级应急基金垫付，事后按复核/强制认领结论向责任单位追偿。
 */
@Data
@Entity
@Table(name = "emergency_fund_advance")
public class EmergencyFundAdvance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long incidentId;

    @Column(nullable = false)
    private Long reviewId;

    /** 垫付审批人（市级协调人员） */
    @Column(nullable = false)
    private Long approvedBy;

    /** 垫付金额（维修等费用） */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** 垫付用途说明 */
    @Column(columnDefinition = "TEXT")
    private String purpose;

    /** 见 Constants.RecoveryStatus：待追偿/已追偿回补 */
    @Column(nullable = false, length = 16)
    private String status = Constants.RecoveryStatus.PENDING;

    /** 追偿目标单位（出具结论后回填） */
    private Long recoverFromOwnerId;

    /** 实际追偿回补金额 */
    @Column(precision = 12, scale = 2)
    private BigDecimal recoveredAmount;

    private LocalDateTime recoveredAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
