package com.city.manhole.model;

import com.city.manhole.common.Constants;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联合复核参与单位代表签收记录：
 * 需签字认可复核结论；拒绝签收的必须说明理由并上传证据，
 * 市级协调人员据此决定是否强制认领。
 */
@Data
@Entity
@Table(name = "joint_review_signoff")
public class JointReviewSignoff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reviewId;

    /** 被通知参会的权属单位用户 id */
    @Column(nullable = false)
    private Long ownerId;

    /** 单位类型标签（燃气/通信/排水），冗余记录方便展示 */
    @Column(nullable = false, length = 16)
    private String unitType;

    /** 参会代表姓名（到场签到） */
    @Column(length = 64)
    private String representativeName;

    /** 见 Constants.SignStatus：待签收/已签字/拒签 */
    @Column(nullable = false, length = 16)
    private String status = Constants.SignStatus.PENDING;

    /** 拒签理由（拒绝签收时必填） */
    @Column(columnDefinition = "TEXT")
    private String refuseReason;

    /** 拒签证据图片 URL 的 JSON 数组（拒绝签收时必须上传） */
    @Column(columnDefinition = "TEXT")
    private String evidencePhotos;

    /** 对现场核验的单位意见 */
    @Column(columnDefinition = "TEXT")
    private String opinion;

    private LocalDateTime signedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}
