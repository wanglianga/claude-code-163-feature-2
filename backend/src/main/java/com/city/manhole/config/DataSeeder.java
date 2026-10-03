package com.city.manhole.config;

import com.city.manhole.common.Constants;
import com.city.manhole.model.*;
import com.city.manhole.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 首次启动时初始化演示数据：各角色账号 + 覆盖全流程各状态的事件。
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepo userRepo;
    private final IncidentRepo incidentRepo;
    private final EmergencyMeasureRepo emergencyRepo;
    private final InvestigationRepo investigationRepo;
    private final OwnerClaimRepo claimRepo;
    private final RepairOrderRepo repairOrderRepo;
    private final AcceptanceRepo acceptanceRepo;
    private final CompensationRepo compensationRepo;
    private final CostAllocationRepo costRepo;
    private final InspectionRouteRepo routeRepo;
    private final HandoverRuleRepo ruleRepo;
    private final JointReviewRepo jointReviewRepo;
    private final ReviewSignatureRepo reviewSignatureRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${app.upload-dir}")
    private String uploadDir;

    private User admin, street, citizen, citizen2, gas, power, telecom, municipal, drainage, repair1, repair2, inspector, comp;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() > 0) return;
        seedUsers();
        seedIncidents();
        seedRoutesAndRules();
        System.out.println("===== 演示数据初始化完成 =====");
    }

    private void seedUsers() {
        admin = user("admin", "admin123", "城管管理员", Constants.Role.ADMIN, "城市管理局", "0311-12345");
        street = user("street", "street123", "值班员-王强", Constants.Role.STREET_DUTY, "朝阳街道值班室", "13800000001");
        citizen = user("citizen", "citizen123", "市民张三", Constants.Role.CITIZEN, "市民", "13800000002");
        citizen2 = user("citizen2", "citizen123", "市民李四", Constants.Role.CITIZEN, "市民", "13800000003");
        gas = user("gas", "owner123", "燃气权属专员", Constants.Role.OWNER_UNIT, "城市燃气集团", "13800000004");
        power = user("power", "owner123", "电力权属专员", Constants.Role.OWNER_UNIT, "国网电力公司", "13800000005");
        telecom = user("telecom", "owner123", "通信权属专员", Constants.Role.OWNER_UNIT, "联合通信公司", "13800000006");
        municipal = user("municipal", "owner123", "市政权属专员", Constants.Role.OWNER_UNIT, "市政工程公司", "13800000007");
        drainage = user("drainage", "owner123", "排水权属专员", Constants.Role.OWNER_UNIT, "城市排水集团", "13800000012");
        repair1 = user("repair1", "repair123", "维修一队长", Constants.Role.REPAIR_TEAM, "市政维修一队", "13800000008");
        repair2 = user("repair2", "repair123", "维修二队长", Constants.Role.REPAIR_TEAM, "燃气专业维修队", "13800000009");
        inspector = user("inspector", "inspect123", "巡查员-赵敏", Constants.Role.INSPECTOR, "城管执法局巡查队", "13800000010");
        comp = user("comp", "comp123", "赔付专员-钱芳", Constants.Role.COMPENSATOR, "保险理赔中心", "13800000011");
    }

    private User user(String username, String password, String name, String role, String org, String phone) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setName(name);
        u.setRole(role);
        u.setOrgName(org);
        u.setPhone(phone);
        return userRepo.save(u);
    }

    // ------------------------------------------------------------------

    private void seedIncidents() {
        // 1. 全流程结案：燃气井盖缺失，车辆受损，赔付已支付，费用已分摊
        Incident i1 = base("MH20260708-0001", citizen, "缺失", "人民路与建设大街交口", "主干道", "高",
                true, "私家车前轮陷入井口，轮毂变形", 80);
        i1.setStatus(Constants.IncidentStatus.CLOSED);
        i1.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i1.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i1.setOwnerId(gas.getId());
        i1.setClosedAt(i1.getCreatedAt().plusDays(6));
        incidentRepo.save(i1);
        emergency(i1, 15, 70, 100, "警示锥,临时盖板,夜间反光标识", "800");
        investigation(i1, "井圈铸有\"燃\"字及编号RQ-2209", "燃气中压管线从此经过", "2024年燃气改造施工记录",
                "商户反映上周燃气公司巡检", "台账登记为燃气集团设施", "CONFIRMED", gas, 79);
        claim(i1, gas, "排查指定", 79);
        RepairOrder o1 = order(i1, gas, repair2, 79, 30, "井室完好，井壁无渗漏", "Φ700 重型球墨铸铁井盖",
                false, false, null, "更换新井盖并加固井圈", "4500");
        acceptance(i1, true, true, true, true, "PASSED", "修复质量合格", 77);
        compensation(i1, "车辆损失", "12000", "交警事故认定书、维修发票",
                "上报后 70 分钟完成围护，权属 1 日内确认，维修 30 小时完成；事故发生于围护前，权属单位承担全责。",
                Constants.Responsibility.OWNER, 100, 0, "8600", Constants.CompStatus.PAID, 78);
        cost(i1, "800", "4500", "8600", Constants.CostStatus.ALLOCATED,
                "[{\"party\":\"权属单位-城市燃气集团\",\"amount\":13900,\"reason\":\"权属确认，承担应急、维修及赔付全部费用\"}]",
                "权属清晰，全部由燃气集团承担，街道垫付应急费予以返还。", 76);

        // 2. 验收通过待费用复核：市政井盖松动
        Incident i2 = base("MH20260906-0001", citizen2, "松动", "和平路商业街", "次干道", "中",
                false, null, 20);
        i2.setStatus(Constants.IncidentStatus.ACCEPTED);
        i2.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i2.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i2.setOwnerId(municipal.getId());
        incidentRepo.save(i2);
        emergency(i2, 20, 150, 180, "警示锥,临时盖板", "300");
        investigation(i2, "井盖铸字\"市政\"", "雨水管线", "无近期施工", "商户未注意", "台账属市政公司",
                "CONFIRMED", municipal, 19);
        claim(i2, municipal, "排查指定", 19);
        order(i2, municipal, repair1, 19, 20, "井室轻微沉降", "Φ600 普通铸铁井盖",
                false, false, null, "更换密封胶条，复平井盖", "2800");
        acceptance(i2, true, true, true, true, "PASSED", "平整度合格", 18);
        cost(i2, "300", "2800", "0", Constants.CostStatus.PENDING,
                "[{\"party\":\"权属单位-市政工程公司\",\"amount\":3100,\"reason\":\"权属已确认，由权属单位承担全部费用\"}]",
                null, 0);

        // 3. 维修完成待验收：电力井盖塌陷
        Incident i3 = base("MH20260920-0001", citizen, "塌陷", "解放大道辅路", "主干道", "高",
                false, null, 6);
        i3.setStatus(Constants.IncidentStatus.REPAIRED);
        i3.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i3.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i3.setOwnerId(power.getId());
        incidentRepo.save(i3);
        emergency(i3, 10, 90, 120, "警示锥,临时盖板,夜间反光标识", "600");
        investigation(i3, "井盖标有\"电\"字", "电缆沟并行", "上月电力增容施工", "商户看到电力作业车", "台账属电力公司",
                "CONFIRMED", power, 5);
        claim(i3, power, "排查指定", 5);
        order(i3, power, repair1, 5, 26, "井室坍塌需重砌", "Φ800 重型防盗井盖",
                true, true, "电力", "重砌井室、更换井盖，已恢复路面", "6000");

        // 4. 维修中：通信井盖异响
        Incident i4 = base("MH20260922-0001", citizen2, "异响", "文化巷", "街巷", "低",
                false, null, 4);
        i4.setStatus(Constants.IncidentStatus.REPAIRING);
        i4.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i4.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i4.setOwnerId(telecom.getId());
        incidentRepo.save(i4);
        emergency(i4, 30, 300, 330, "警示锥", "100");
        investigation(i4, "井盖铸\"通信\"", "通信光缆井", "无", "商户反映夜间异响明显", "台账属通信公司",
                "CONFIRMED", telecom, 3);
        claim(i4, telecom, "排查指定", 3);
        RepairOrder o4 = new RepairOrder();
        o4.setIncidentId(i4.getId());
        o4.setOwnerId(telecom.getId());
        o4.setTeamId(repair1.getId());
        o4.setAssignedAt(i4.getCreatedAt().plusDays(1));
        o4.setAcceptedAt(i4.getCreatedAt().plusDays(1).plusHours(3));
        o4.setStatus(Constants.RepairStatus.ACCEPTED);
        repairOrderRepo.save(o4);

        // 5. 责任待定：滨河路井盖缺失，行人摔伤，街道先行处置垫付
        Incident i5 = base("MH20260923-0001", citizen, "缺失", "滨河路", "次干道", "高",
                true, "行人夜间跌入井口，腿部骨折送医", 3);
        i5.setStatus(Constants.IncidentStatus.SECURED);
        i5.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i5.setOwnerStatus(Constants.OwnerStatus.PENDING);
        i5.setResponsibilityPending(true);
        i5.setEverPending(true);
        incidentRepo.save(i5);
        emergency(i5, 8, 55, 80, "警示锥,临时盖板,夜间反光标识", "900");
        investigation(i5, "井口无盖，井圈标识磨损无法辨认", "管线图显示燃气与通信管线交汇",
                "近三年无施工登记", "商户称井盖已缺失两周", "台账无该井登记",
                "PENDING", null, 2);
        CompensationCase c5 = new CompensationCase();
        c5.setIncidentId(i5.getId());
        c5.setClaimantName("市民张三");
        c5.setClaimantPhone("13800000002");
        c5.setDamageType("人身伤害");
        c5.setClaimAmount(new BigDecimal("50000"));
        c5.setAccidentProof("医院诊断证明、急诊病历、现场照片");
        c5.setStatus(Constants.CompStatus.FILED);
        c5.setCreatedAt(i5.getCreatedAt().plusHours(2));
        compensationRepo.save(c5);

        // 6. 应急完成排查中：中山路井盖松动
        Incident i6 = base("MH20260924-0001", citizen2, "松动", "中山路", "主干道", "中",
                false, null, 2);
        i6.setStatus(Constants.IncidentStatus.SECURED);
        i6.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i6.setOwnerStatus(Constants.OwnerStatus.INVESTIGATING);
        incidentRepo.save(i6);
        emergency(i6, 12, 100, 130, "警示锥,临时盖板", "350");

        // 7. 刚上报待应急：胜利街井盖塌陷
        Incident i7 = base("MH20260926-0001", citizen, "塌陷", "胜利街", "支路", "高",
                false, null, 0);
        i7.setStatus(Constants.IncidentStatus.REPORTED);
        i7.setEmergencyStatus(Constants.EmergencyStatus.PENDING);
        i7.setOwnerStatus(Constants.OwnerStatus.UNCONFIRMED);
        i7.setCreatedAt(LocalDateTime.now().minusHours(5));
        i7.setUpdatedAt(i7.getCreatedAt());
        incidentRepo.save(i7);

        // 8. 曾责任待定后被通信认领并结案（权属争议样例）
        Incident i8 = base("MH20260728-0001", citizen2, "缺失", "建设大街北段", "主干道", "中",
                true, "电动车避让不及侧翻，骑手擦伤", 60);
        i8.setStatus(Constants.IncidentStatus.CLOSED);
        i8.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i8.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i8.setOwnerId(telecom.getId());
        i8.setEverPending(true);
        i8.setClosedAt(i8.getCreatedAt().plusDays(9));
        incidentRepo.save(i8);
        emergency(i8, 25, 130, 160, "警示锥,临时盖板", "450");
        investigation(i8, "井圈无标识", "通信与雨水管线交汇", "移交记录缺失", "商户说法不一", "台账未登记",
                "PENDING", null, 59);
        investigation(i8, "复核井室内部发现通信光缆标识牌", "确认为通信管井", "2019 年通信扩容施工",
                "老商户确认为通信井", "补充台账", "CONFIRMED", telecom, 55);
        claim(i8, telecom, "主动认领", 56);
        order(i8, telecom, repair1, 55, 40, "井室完好", "Φ650 复合井盖",
                false, true, "通信", "更换井盖", "2200");
        acceptance(i8, true, true, true, true, "PASSED", "合格", 52);
        compensation(i8, "人身伤害", "6000", "门诊病历、误工证明",
                "围护 130 分钟到场略超时限；权属排查因移交记录缺失一度待定，通信公司认领后维修及时；双方均有责任。",
                Constants.Responsibility.SHARED, 70, 30, "3200", Constants.CompStatus.APPROVED, 51);
        cost(i8, "450", "2200", "3200", Constants.CostStatus.ALLOCATED,
                "[{\"party\":\"权属单位-联合通信公司\",\"amount\":4090,\"reason\":\"承担 70% 赔付及全部维修费\"},{\"party\":\"街道\",\"amount\":1760,\"reason\":\"围护略超时，承担 30% 赔付及应急费\"}]",
                "按 7:3 分摊赔付金，维修费由通信公司承担。", 50);

        // 9. 维修超时结案：市政井盖塌陷 72 小时才修完
        Incident i9 = base("MH20260812-0001", citizen, "塌陷", "和平路东段", "次干道", "高",
                false, null, 45);
        i9.setStatus(Constants.IncidentStatus.CLOSED);
        i9.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i9.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i9.setOwnerId(municipal.getId());
        i9.setClosedAt(i9.getCreatedAt().plusDays(5));
        incidentRepo.save(i9);
        emergency(i9, 40, 200, 230, "警示锥,临时盖板,夜间反光标识", "500");
        investigation(i9, "铸字\"市政\"", "雨水主管", "道路大修后移交", "商户反映多次报修", "台账属市政",
                "CONFIRMED", municipal, 44);
        claim(i9, municipal, "排查指定", 44);
        order(i9, municipal, repair1, 44, 72, "井室破损严重", "Φ700 重型井盖",
                true, false, null, "封路重砌井室，耗时较长", "6000");
        acceptance(i9, true, true, true, true, "PASSED", "修复合格但维修超时", 41);
        cost(i9, "500", "6000", "0", Constants.CostStatus.ALLOCATED,
                "[{\"party\":\"权属单位-市政工程公司\",\"amount\":6500,\"reason\":\"权属确认且维修超时，承担全部费用\"}]",
                "维修超时 24 小时，纳入市政公司考核。", 40);

        // 10. 验收通过待复核：人民路西段井盖异响
        Incident i10 = base("MH20260911-0001", citizen2, "异响", "人民路西段", "支路", "低",
                false, null, 15);
        i10.setStatus(Constants.IncidentStatus.ACCEPTED);
        i10.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i10.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        i10.setOwnerId(municipal.getId());
        incidentRepo.save(i10);
        emergency(i10, 35, 260, 290, "警示锥", "150");
        investigation(i10, "铸字\"市政\"", "污水管线", "无", "商户反映车辆碾压异响", "台账属市政",
                "CONFIRMED", municipal, 14);
        claim(i10, municipal, "排查指定", 14);
        order(i10, municipal, repair1, 14, 12, "井室完好，胶条老化", "Φ600 铸铁井盖",
                false, false, null, "更换减震胶条", "900");
        acceptance(i10, true, true, true, true, "PASSED", "异响消除", 13);
        cost(i10, "150", "900", "0", Constants.CostStatus.PENDING,
                "[{\"party\":\"权属单位-市政工程公司\",\"amount\":1050,\"reason\":\"权属已确认，由权属单位承担全部费用\"}]",
                null, 0);

        // 11. 燃气/通信/排水均否认归属：联合现场复核进行中，通信已拒签并上传证据
        Incident i11 = base("MH20260928-0001", citizen, "缺失", "裕华东路与槐安路交口", "主干道", "高",
                false, null, 1);
        i11.setStatus(Constants.IncidentStatus.SECURED);
        i11.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i11.setOwnerStatus(Constants.OwnerStatus.JOINT_REVIEW);
        i11.setResponsibilityPending(true);
        i11.setEverPending(true);
        incidentRepo.save(i11);
        emergency(i11, 9, 60, 85, "警示锥,临时盖板,夜间反光标识", "760");
        investigation(i11, "井圈铸字磨损，仅见编号 YH-0417", "燃气、通信、排水三条管线在交口交汇",
                "2023 年道路大修许可（市政工程公司承办）", "周边商户对归属说法不一", "三家单位台账均无登记",
                "PENDING", null, 1);
        JointReview r11 = new JointReview();
        r11.setIncidentId(i11.getId());
        r11.setInitiatedById(street.getId());
        r11.setManholeCode("YH-0417（井圈铸字编号，字迹磨损）");
        r11.setPipelineMap("地下管线图：燃气中压管（深 1.2m）、通信光缆（深 0.8m）、排水雨水管（深 2.1m）三线交汇，井室深 1.9m");
        r11.setLatestPermit("最近一次施工许可：城施许字〔2023〕第 118 号，裕华东路道路大修，承办单位市政工程公司");
        r11.setRepairHistory("历史维修：2021-05 更换过井盖（承办单位登记缺失）；2019-09 通信扩容时曾开启该井");
        r11.setScheduledAt(LocalDateTime.now().plusDays(1).withHour(9).withMinute(30).withSecond(0).withNano(0));
        r11.setLocation("裕华东路与槐安路交口东北角现场");
        r11.setNote("燃气、通信、排水三家单位均否认归属，请代表携带本单位管线资料到场共同核验。");
        r11.setStatus(Constants.ReviewStatus.IN_PROGRESS);
        jointReviewRepo.save(r11);
        reviewSignature(r11, i11, gas, Constants.SignStatus.PENDING, null, null, null, null);
        reviewSignature(r11, i11, drainage, Constants.SignStatus.PENDING, null, null, null, null);
        reviewSignature(r11, i11, telecom, Constants.SignStatus.REFUSED, null, null,
                "现场比对我单位通信光缆走线图，该井内未敷设光缆，井室结构亦非通信标准井，非我单位设施。",
                List.of(evidenceImage("通信走线图比对"), evidenceImage("井室内部照片")));

        // 12. 复核仍无法确认：进入市级协调池，应急基金已垫付，三家均拒签有证据，等待市级强制认领
        Incident i12 = base("MH20260925-0001", citizen2, "塌陷", "南二环辅道", "次干道", "高",
                true, "电动车前轮陷入塌陷区域，骑手擦伤", 4);
        i12.setStatus(Constants.IncidentStatus.SECURED);
        i12.setEmergencyStatus(Constants.EmergencyStatus.SECURED);
        i12.setOwnerStatus(Constants.OwnerStatus.CITY_POOL);
        i12.setResponsibilityPending(true);
        i12.setEverPending(true);
        i12.setEverCityPool(true);
        incidentRepo.save(i12);
        emergency(i12, 11, 70, 95, "警示锥,临时盖板,夜间反光标识", "1200");
        investigation(i12, "井圈编号 NH-2208，无权属铸字", "燃气管与排水管相邻",
                "施工许可记录缺失", "商户称未见任何单位巡查", "台账无登记",
                "PENDING", null, 4);
        JointReview r12 = new JointReview();
        r12.setIncidentId(i12.getId());
        r12.setInitiatedById(street.getId());
        r12.setManholeCode("NH-2208");
        r12.setPipelineMap("地下管线图：燃气中压管与排水雨水箱涵相邻，走向与井位偏差均在 0.5m 内");
        r12.setLatestPermit("最近一次施工许可：未查询到（档案缺失）");
        r12.setRepairHistory("历史维修：仅 2018 年一条井盖更换工单，未登记承办单位");
        r12.setScheduledAt(i12.getCreatedAt().plusDays(1));
        r12.setLocation("南二环辅道现场");
        r12.setNote("三家单位现场核验后仍各执一词，无法形成一致结论。");
        r12.setStatus(Constants.ReviewStatus.ESCALATED);
        r12.setConclusion("UNRESOLVED");
        r12.setConclusionNote("现场井室同时邻近燃气与排水设施，各方证据均不能排他，复核无法确认权属，上报市级协调。");
        r12.setEscalationNote(r12.getConclusionNote());
        r12.setEscalatedAt(LocalDateTime.now().minusDays(2));
        r12.setFinishedAt(r12.getEscalatedAt());
        r12.setCostSuggestion("[{\"party\":\"市级应急基金（先行垫付，待追偿）\",\"ratio\":100,\"reason\":\"联合现场复核仍无法确认权属，应急基金先行垫付，待市级协调认定后追偿\"}]");
        r12.setFundAdvanceAmount(new BigDecimal("3600"));
        r12.setFundAdvanceAt(LocalDateTime.now().minusDays(2));
        r12.setFundAdvanceNote("应急围护及临时盖板费用 ¥1200、应急维修 ¥2400，待责任认定后追偿。");
        jointReviewRepo.save(r12);
        reviewSignature(r12, i12, gas, Constants.SignStatus.REFUSED, null, null,
                "井室内无燃气阀门、凝水缸等附属设施，测漏无燃气浓度，非燃气井。",
                List.of(evidenceImage("燃气测漏记录")));
        reviewSignature(r12, i12, telecom, Constants.SignStatus.REFUSED, null, null,
                "井内无光缆托架及余缆，非通信井。",
                List.of(evidenceImage("井室无光缆照片")));
        reviewSignature(r12, i12, drainage, Constants.SignStatus.REFUSED, null, null,
                "该井未接入雨水箱涵，井底为实土，非排水检查井。",
                List.of(evidenceImage("井底实拍")));
    }

    private Incident base(String code, User reporter, String issueType, String address, String roadType,
                          String danger, boolean hasDamage, String damageDesc, int daysAgo) {
        Incident inc = new Incident();
        inc.setCode(code);
        inc.setReporterId(reporter.getId());
        inc.setIssueType(issueType);
        inc.setAddress(address);
        inc.setRoadType(roadType);
        inc.setLongitude(114.48 + Math.random() * 0.1);
        inc.setLatitude(38.03 + Math.random() * 0.06);
        inc.setPhotos("[]");
        inc.setHasDamage(hasDamage);
        inc.setDamageDesc(damageDesc);
        inc.setDangerLevel(danger);
        inc.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        inc.setUpdatedAt(inc.getCreatedAt());
        return inc;
    }

    private void emergency(Incident inc, int dispatchMin, int arriveMin, int secureMin,
                           String measures, String cost) {
        EmergencyMeasure em = new EmergencyMeasure();
        em.setIncidentId(inc.getId());
        em.setOfficerId(street.getId());
        em.setDispatchedAt(inc.getCreatedAt().plusMinutes(dispatchMin));
        em.setArrivedAt(inc.getCreatedAt().plusMinutes(arriveMin));
        em.setSecuredAt(inc.getCreatedAt().plusMinutes(secureMin));
        em.setMeasures(measures);
        em.setCost(new BigDecimal(cost));
        em.setNote("现场已完成临时围护");
        em.setCreatedAt(em.getDispatchedAt());
        emergencyRepo.save(em);
    }

    private void investigation(Incident inc, String marking, String pipeline, String construction,
                               String merchant, String ledger, String conclusion, User owner, int daysAgo) {
        OwnershipInvestigation inv = new OwnershipInvestigation();
        inv.setIncidentId(inc.getId());
        inv.setInvestigatorId(street.getId());
        inv.setManholeMarking(marking);
        inv.setPipelineInfo(pipeline);
        inv.setConstructionHistory(construction);
        inv.setMerchantInfo(merchant);
        inv.setCityLedger(ledger);
        inv.setConclusion(conclusion);
        inv.setConfirmedOwnerId(owner == null ? null : owner.getId());
        inv.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        investigationRepo.save(inv);
    }

    private void claim(Incident inc, User owner, String type, int daysAgo) {
        OwnerClaim c = new OwnerClaim();
        c.setIncidentId(inc.getId());
        c.setOwnerId(owner.getId());
        c.setClaimType(type);
        c.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        claimRepo.save(c);
    }

    private void reviewSignature(JointReview r, Incident inc, User owner, String status, String signerName,
                                 String signImage, String refuseReason, List<String> evidence) {
        ReviewSignature s = new ReviewSignature();
        s.setReviewId(r.getId());
        s.setIncidentId(inc.getId());
        s.setOwnerId(owner.getId());
        s.setStatus(status);
        s.setSignerName(signerName);
        s.setSignImage(signImage);
        s.setRefuseReason(refuseReason);
        if (evidence != null) s.setRefuseEvidence(new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(evidence).toString());
        s.setCreatedAt(LocalDateTime.now().minusHours(6));
        s.setSignedAt(Constants.SignStatus.PENDING.equals(status) ? null : s.getCreatedAt());
        reviewSignatureRepo.save(s);
    }

    /** 生成一张演示用证据图片（白底黑字说明），返回 /uploads/xxx.png */
    private String evidenceImage(String label) {
        try {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(480, 240,
                    java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, 480, 240);
            g.setColor(new java.awt.Color(31, 58, 95));
            g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 22));
            g.drawString("现场核验证据材料", 24, 48);
            g.setColor(java.awt.Color.BLACK);
            g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 18));
            int y = 90;
            for (String line : label.split("(?<=\\G.{22})")) {
                g.drawString(line, 24, y);
                y += 30;
            }
            g.setColor(new java.awt.Color(180, 180, 180));
            g.drawRect(12, 12, 456, 216);
            g.dispose();
            File dir = new File(uploadDir).getAbsoluteFile();
            if (!dir.exists()) dir.mkdirs();
            String name = "seed-evidence-" + Integer.toHexString(label.hashCode()) + ".png";
            File f = new File(dir, name);
            if (!f.exists()) javax.imageio.ImageIO.write(img, "png", f);
            return "/uploads/" + name;
        } catch (Exception e) {
            return null;
        }
    }

    private RepairOrder order(Incident inc, User owner, User team, int assignDaysAgo, long repairHours,
                              String chamber, String spec, boolean closure, boolean coord, String coordUnits,
                              String note, String cost) {
        RepairOrder o = new RepairOrder();
        o.setIncidentId(inc.getId());
        o.setOwnerId(owner.getId());
        o.setTeamId(team.getId());
        o.setAssignedAt(LocalDateTime.now().minusDays(assignDaysAgo));
        o.setAcceptedAt(o.getAssignedAt().plusHours(2));
        o.setArrivedAt(o.getAssignedAt().plusHours(5));
        o.setChamberCondition(chamber);
        o.setCoverSpec(spec);
        o.setNeedRoadClosure(closure);
        o.setNeedCoord(coord);
        o.setCoordUnits(coordUnits);
        o.setRepairNote(note);
        o.setRepairCost(new BigDecimal(cost));
        o.setFinishedAt(o.getAssignedAt().plusHours(repairHours));
        o.setStatus(Constants.RepairStatus.FINISHED);
        return repairOrderRepo.save(o);
    }

    private void acceptance(Incident inc, boolean photo, boolean loc, boolean flat, boolean roll,
                            String result, String note, int daysAgo) {
        Acceptance a = new Acceptance();
        a.setIncidentId(inc.getId());
        a.setInspectorId(inspector.getId());
        a.setPhotoOk(photo);
        a.setLocationOk(loc);
        a.setFlatnessOk(flat);
        a.setRollingTestOk(roll);
        a.setResult(result);
        a.setNote(note);
        a.setInspectedAt(LocalDateTime.now().minusDays(daysAgo));
        acceptanceRepo.save(a);
    }

    private void compensation(Incident inc, String damageType, String claimAmount, String proof,
                              String analysis, String responsibility, int ownerRatio, int streetRatio,
                              String approved, String status, int decidedDaysAgo) {
        CompensationCase c = new CompensationCase();
        c.setIncidentId(inc.getId());
        c.setSpecialistId(comp.getId());
        c.setClaimantName("市民");
        c.setClaimantPhone("13800000002");
        c.setDamageType(damageType);
        c.setClaimAmount(new BigDecimal(claimAmount));
        c.setAccidentProof(proof);
        c.setAnalysis(analysis);
        c.setResponsibility(responsibility);
        c.setOwnerRatio(ownerRatio);
        c.setStreetRatio(streetRatio);
        c.setApprovedAmount(new BigDecimal(approved));
        c.setStatus(status);
        c.setCreatedAt(inc.getCreatedAt().plusHours(3));
        c.setDecidedAt(LocalDateTime.now().minusDays(decidedDaysAgo));
        if (Constants.CompStatus.PAID.equals(status)) {
            c.setPaidAt(c.getDecidedAt().plusDays(1));
        }
        compensationRepo.save(c);
    }

    private void cost(Incident inc, String emergency, String repair, String compAmount,
                      String status, String allocations, String reviewNote, int reviewedDaysAgo) {
        CostAllocation ca = new CostAllocation();
        ca.setIncidentId(inc.getId());
        ca.setEmergencyCost(new BigDecimal(emergency));
        ca.setRepairCost(new BigDecimal(repair));
        ca.setCompensationAmount(new BigDecimal(compAmount));
        ca.setTotalAmount(ca.getEmergencyCost().add(ca.getRepairCost()).add(ca.getCompensationAmount()));
        ca.setAllocations(allocations);
        ca.setStatus(status);
        ca.setReviewNote(reviewNote);
        ca.setCreatedAt(inc.getCreatedAt().plusDays(2));
        if (Constants.CostStatus.ALLOCATED.equals(status)) {
            ca.setReviewedBy(admin.getId());
            ca.setReviewedAt(LocalDateTime.now().minusDays(reviewedDaysAgo));
        }
        costRepo.save(ca);
    }

    private void seedRoutesAndRules() {
        InspectionRoute r1 = new InspectionRoute();
        r1.setName("巡检路线 A（高发路段加密）");
        r1.setSections("人民路,建设大街,和平路");
        r1.setFrequency("每日");
        r1.setNote("近 90 天高发路段，加密为每日巡检");
        routeRepo.save(r1);

        InspectionRoute r2 = new InspectionRoute();
        r2.setName("巡检路线 B（商业区）");
        r2.setSections("中山路,解放大道,和平路商业街");
        r2.setFrequency("隔日");
        r2.setNote("人流量大，重点关注松动与异响");
        routeRepo.save(r2);

        InspectionRoute r3 = new InspectionRoute();
        r3.setName("巡检路线 C（背街小巷）");
        r3.setSections("文化巷,滨河路,胜利街");
        r3.setFrequency("每周");
        r3.setNote("夜间照明不足路段，重点检查反光标识");
        routeRepo.save(r3);

        HandoverRule rule1 = new HandoverRule();
        rule1.setTitle("新建道路井盖设施移交时限");
        rule1.setContent("新建、改建道路竣工验收后 15 日内，施工单位须将井盖设施台账（位置、规格、权属、管线资料）移交市政工程公司并报城管局备案；逾期未移交期间发生的井盖事件，由施工单位承担责任。");
        ruleRepo.save(rule1);

        HandoverRule rule2 = new HandoverRule();
        rule2.setTitle("专业管线井盖协同核验");
        rule2.setContent("涉及燃气、电力、通信等专业井盖的施工，开工前须通知权属单位现场核验，施工结束后由权属单位签字确认井盖复位质量；未履行协同程序的，发生问题由施工单位与权属单位按 5:5 分摊。");
        ruleRepo.save(rule2);

        HandoverRule rule3 = new HandoverRule();
        rule3.setTitle("无主井盖先行处置与费用回补");
        rule3.setContent("权属无法即时确认的井盖险情，街道先行设置警示锥、临时盖板、夜间反光标识并垫付费用；权属确认后 30 日内由权属单位回补街道垫付费用，复核分摊结果以城管部门费用复核单为准。");
        ruleRepo.save(rule3);

        HandoverRule rule4 = new HandoverRule();
        rule4.setTitle("权属争议联合现场复核与市级协调");
        rule4.setContent("燃气、通信、排水等单位均否认井盖归属的，由街道发起联合现场复核，平台汇总井盖编号、地下管线图、最近一次施工许可、历史维修记录；复核结论须各单位代表签字，拒绝签收的须书面说明理由并上传证据。复核仍无法确认的，事件进入市级协调池，维修费用先由市级应急基金垫付；市级协调人员依据各方现场意见及拒签证据研判，可作出强制认领决定，应急基金垫付款按复核结论向责任单位追偿。");
        ruleRepo.save(rule4);
    }
}
