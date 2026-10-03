package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 井盖档案：井盖编号、所属路段、井室内部标识，以及地下管线图资料。
 * 各权属单位互相推诿时，平台据此展示井盖编号与地下管线图。
 */
@Data
@Entity
@Table(name = "manhole_profile")
public class ManholeProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 井盖资产编号（井圈铸字/铭牌编号，如 RQ-2209） */
    @Column(nullable = false, unique = true, length = 32)
    private String manholeCode;

    @Column(nullable = false, length = 256)
    private String address;

    /** 井盖类型：燃气/电力/通信/雨水/污水/不明 */
    @Column(length = 16)
    private String coverType;

    /** 井室内部标识（开井后可见的铭牌、线缆吊牌等） */
    @Column(columnDefinition = "TEXT")
    private String chamberMarking;

    /** 地下管线图（文字描述） */
    @Column(columnDefinition = "TEXT")
    private String pipelineDiagram;

    /** 管线图附件图片 URL 的 JSON 数组 */
    @Column(columnDefinition = "TEXT")
    private String pipelinePhotos;

    /** 台账登记权属单位（可能缺失，即"无主"争议来源） */
    private Long registeredOwnerId;

    private LocalDateTime updatedAt = LocalDateTime.now();
}
