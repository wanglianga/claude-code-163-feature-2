package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 联合现场复核：燃气、通信、排水（市政）等单位均否认井盖归属时，
 * 由街道发起、各单位代表到现场共同核验，复核结论须由各单位代表签字。
 * 仍无法确认的，事件进入市级协调池，维修费用先由市级应急基金垫付，再按复核结论追偿。
 */
@Data
@Entity
@Table(name = "joint_review")
public class JointReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long incidentId;

    /** 发起人（街道值班员） */
    @Column(nullable = false)
    private Long initiatedById;

    // ---------- 平台汇总展示的核验证据包 ----------
    /** 井盖编号（井圈铸字编号/设施编号） */
    @Column(length = 128)
    private String manholeCode;

    /** 地下管线图（图号/说明，或上传的图纸图片 URL） */
    @Column(columnDefinition = "TEXT")
    private String pipelineMap;

    /** 最近一次施工许可（许可编号、施工单位、许可时间） */
    @Column(columnDefinition = "TEXT")
    private String latestPermit;

    /** 历史维修记录 */
    @Column(columnDefinition = "TEXT")
    private String repairHistory;
    // ---------------------------------------------

    /** 计划现场复核时间 */
    private LocalDateTime scheduledAt;

    /** 现场复核地点 */
    @Column(length = 256)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String note;

    /** 见 Constants.ReviewStatus：IN_PROGRESS / ESCALATED / CONFIRMED */
    @Column(nullable = false, length = 16)
    private String status;

    /** 复核权属结论：CONFIRMED 确认 / UNRESOLVED 仍无法确认 */
    @Column(length = 16)
    private String conclusion;

    /** 复核确认的权属单位 */
    private Long confirmedOwnerId;

    /** 费用分摊建议 JSON：[{"party":"权属单位-城市燃气集团","ratio":100,"reason":"复核确认为燃气管井"}] */
    @Column(columnDefinition = "TEXT")
    private String costSuggestion;

    @Column(columnDefinition = "TEXT")
    private String conclusionNote;

    private LocalDateTime finishedAt;

    // ---------- 市级协调 ----------
    /** 进入市级协调池时间 */
    private LocalDateTime escalatedAt;

    @Column(columnDefinition = "TEXT")
    private String escalationNote;

    /** 市级应急基金已垫付金额 */
    @Column(precision = 12, scale = 2)
    private BigDecimal fundAdvanceAmount;

    private LocalDateTime fundAdvanceAt;

    @Column(columnDefinition = "TEXT")
    private String fundAdvanceNote;

    /** 是否经市级强制认领 */
    @Column(nullable = false)
    private Boolean forcedClaim = false;

    /** 强制认领决定人（市级协调人员） */
    private Long forcedById;

    private LocalDateTime forcedAt;

    @Column(columnDefinition = "TEXT")
    private String forcedNote;
    // --------------------------------

    private LocalDateTime createdAt = LocalDateTime.now();
}
