package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联合现场复核代表签收记录：每个受邀单位一条。
 * 拒绝签收必须说明理由并上传证据，供市级协调人员判断是否强制认领。
 */
@Data
@Entity
@Table(name = "review_signature",
        uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "owner_id"}))
public class ReviewSignature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(nullable = false)
    private Long incidentId;

    /** 受邀权属单位用户 id（代表） */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /** 见 Constants.SignStatus：PENDING / SIGNED / REFUSED */
    @Column(nullable = false, length = 16)
    private String status;

    /** 代表姓名（签字时填写） */
    @Column(length = 64)
    private String signerName;

    /** 签字图片/证据图片 URL */
    @Column(columnDefinition = "TEXT")
    private String signImage;

    /** 拒绝签收理由 */
    @Column(columnDefinition = "TEXT")
    private String refuseReason;

    /** 拒绝签收上传的证据材料（图片 URL JSON 数组） */
    @Column(columnDefinition = "TEXT")
    private String refuseEvidence;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime signedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}
