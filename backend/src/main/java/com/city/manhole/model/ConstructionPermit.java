package com.city.manhole.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 施工许可记录：该井盖点位最近一次占道/掘路施工许可。
 * 推诿复核时用于追溯最后施工单位与设施移交情况。
 */
@Data
@Entity
@Table(name = "construction_permit")
public class ConstructionPermit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long profileId;

    /** 许可编号 */
    @Column(nullable = false, length = 32)
    private String permitNo;

    /** 施工项目名称 */
    @Column(nullable = false, length = 256)
    private String projectName;

    /** 施工单位 */
    @Column(nullable = false, length = 128)
    private String constructor;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    /** 涉及管线单位，逗号分隔：燃气,通信 */
    @Column(length = 128)
    private String involvedUnits;

    /** 竣工验收/设施移交情况 */
    @Column(columnDefinition = "TEXT")
    private String handoverInfo;
}
