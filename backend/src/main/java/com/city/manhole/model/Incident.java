package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 井盖事件：市民上报的缺失/松动/异响/塌陷问题。
 * 主流程状态 status 与两条线（应急处置 emergencyStatus、权属追责 ownerStatus）分离。
 */
@Data
@Entity
@Table(name = "incident")
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 事件编号 MHyyyyMMdd-xxxx */
    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private Long reporterId;

    /** 问题类型：缺失/松动/异响/塌陷 */
    @Column(nullable = false, length = 16)
    private String issueType;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double latitude;

    /** 详细地址/路段 */
    @Column(nullable = false, length = 256)
    private String address;

    /** 道路类型：主干道/次干道/支路/人行道/街巷 */
    @Column(nullable = false, length = 16)
    private String roadType;

    /** 现场照片 URL 的 JSON 数组 */
    @Column(columnDefinition = "TEXT")
    private String photos;

    /** 是否已造成人车损伤 */
    @Column(nullable = false)
    private Boolean hasDamage = false;

    /** 损伤情况说明 */
    @Column(columnDefinition = "TEXT")
    private String damageDesc;

    /** 现场危险程度：高/中/低 */
    @Column(nullable = false, length = 8)
    private String dangerLevel;

    /** 主流程状态，见 Constants.IncidentStatus */
    @Column(nullable = false, length = 32)
    private String status;

    /** 应急处置线状态，见 Constants.EmergencyStatus */
    @Column(nullable = false, length = 32)
    private String emergencyStatus;

    /** 权属追责线状态，见 Constants.OwnerStatus */
    @Column(nullable = false, length = 32)
    private String ownerStatus;

    /** 权属单位用户 id（确认/认领后写入） */
    private Long ownerId;

    /** 责任待定标记：无法确认归属时由街道先行处置并垫付费用 */
    @Column(nullable = false)
    private Boolean responsibilityPending = false;

    /** 是否经历过责任待定（用于权属争议统计） */
    @Column(nullable = false)
    private Boolean everPending = false;

    /** 关联井盖档案编号（manhole_profile.manhole_code），推诿复核时展示档案证据 */
    @Column(length = 32)
    private String manholeCode;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime closedAt;
}
