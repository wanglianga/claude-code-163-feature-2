package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联合现场复核：燃气、通信、排水（市政）等单位均否认井盖归属时，
 * 由街道发起，各单位代表到场共同核验并在复核记录上签字；
 * 仍无法确认的，事件进入市级协调池，应急基金先行垫付维修费用。
 */
@Data
@Entity
@Table(name = "joint_review")
public class JointReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 最新一次复核（一个事件可多次发起，detail 只暴露最新一次及历史摘要） */
    @Column(nullable = false)
    private Long incidentId;

    /** 发起方：街道（值班员） */
    @Column(nullable = false)
    private Long initiatorId;

    /** 计划现场复核时间 */
    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    /** 实际完成时间（出具结论时写入） */
    private LocalDateTime concludedAt;

    /** 现场复核地点/方位说明 */
    @Column(length = 256)
    private String siteLocation;

    /** 街道发起说明（推诿情况、已联系单位等） */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /** 街道在复核中查明的事实记录 */
    @Column(columnDefinition = "TEXT")
    private String siteFindings;

    /**
     * 复核结论：
     * CONFIRMED 各方签字确认权属单位 /
     * DISPUTED 各单位代表均已表态但仍无法确认 → 进入市级协调池
     */
    @Column(length = 16)
    private String conclusion;

    /** 结论确认的权属单位用户 id（CONFIRMED 时必填） */
    private Long confirmedOwnerId;

    /** 费用分摊建议 JSON：[{"party":"权属单位-城市燃气集团","amount":13900,"reason":"..."}] */
    @Column(columnDefinition = "TEXT")
    private String costSuggestion;

    /** 应急基金垫付金额（结论为 DISPUTED 时记录） */
    @Column(precision = 12, scale = 2)
    private java.math.BigDecimal advanceAmount;

    /** 见 Constants.JointReviewStatus */
    @Column(nullable = false, length = 16)
    private String status;

    /** 进入协调池时间 */
    private LocalDateTime escalatedAt;

    /** 强制认领决定：市级协调人员 */
    private Long forcedBy;
    /** 强制认领决定时间 */
    private LocalDateTime forcedAt;
    /** 强制认领理由 */
    @Column(columnDefinition = "TEXT")
    private String forceReason;

    private LocalDateTime createdAt = LocalDateTime.now();
}
