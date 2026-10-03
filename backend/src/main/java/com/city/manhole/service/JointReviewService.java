package com.city.manhole.service;

import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.common.Constants;
import com.city.manhole.model.*;
import com.city.manhole.repo.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 联合现场复核线：各权属单位均否认归属时，街道发起联合现场复核，
 * 平台汇总井盖编号、地下管线图、最近一次施工许可、历史维修记录供现场核验，
 * 复核结论须由各单位代表签字；仍无法确认则进入市级协调池，
 * 维修费用先由市级应急基金垫付，后续按复核结论/强制认领决定追偿。
 */
@Service
@RequiredArgsConstructor
public class JointReviewService {
    private final JointReviewRepo reviewRepo;
    private final ReviewSignatureRepo signatureRepo;
    private final OwnerClaimRepo claimRepo;
    private final UserRepo userRepo;
    private final IncidentService incidentService;
    private final NotificationService notificationService;
    private final ObjectMapper mapper = new ObjectMapper();

    /** 街道发起联合现场复核，并汇总平台核验证据包 */
    @Transactional
    public JointReview initiate(Long incidentId, List<Long> ownerIds, String manholeCode, String pipelineMap,
                                String latestPermit, String repairHistory, String scheduledAt,
                                String location, String note) {
        User me = AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.ADMIN);
        Incident inc = incidentService.getOrThrow(incidentId);
        if (Constants.OwnerStatus.CONFIRMED.equals(inc.getOwnerStatus())) {
            throw new BizException("权属已确认，无需联合复核");
        }
        if (!Constants.EmergencyStatus.SECURED.equals(inc.getEmergencyStatus())) {
            throw new BizException("请先完成现场临时围护，再发起联合复核");
        }
        if (reviewRepo.findByIncidentId(incidentId).isPresent()) {
            throw new BizException("该事件已发起联合复核");
        }
        if (ownerIds == null || ownerIds.isEmpty()) {
            throw new BizException("请至少选择一个参加现场复核的权属单位");
        }
        List<User> owners = new ArrayList<>();
        for (Long oid : ownerIds.stream().distinct().toList()) {
            User u = userRepo.findById(oid == null ? -1 : oid)
                    .filter(x -> Constants.Role.OWNER_UNIT.equals(x.getRole()))
                    .orElseThrow(() -> new BizException("权属单位不存在"));
            owners.add(u);
        }

        JointReview r = new JointReview();
        r.setIncidentId(incidentId);
        r.setInitiatedById(me.getId());
        r.setManholeCode(manholeCode);
        r.setPipelineMap(pipelineMap);
        r.setLatestPermit(latestPermit);
        r.setRepairHistory(repairHistory);
        r.setScheduledAt(parseTime(scheduledAt));
        r.setLocation(location);
        r.setNote(note);
        r.setStatus(Constants.ReviewStatus.IN_PROGRESS);
        reviewRepo.save(r);

        for (User u : owners) {
            ReviewSignature s = new ReviewSignature();
            s.setReviewId(r.getId());
            s.setIncidentId(incidentId);
            s.setOwnerId(u.getId());
            s.setStatus(Constants.SignStatus.PENDING);
            signatureRepo.save(s);
            notificationService.notify(u.getId(), incidentId,
                    "邀请参加联合现场复核 " + inc.getCode(),
                    inc.getAddress() + " 井盖" + inc.getIssueType() + "权属存在争议，请派代表按时参加现场复核并签收结论。");
        }

        inc.setOwnerStatus(Constants.OwnerStatus.JOINT_REVIEW);
        inc.setResponsibilityPending(true);
        incidentService.save(inc);
        notificationService.notifyRole(Constants.Role.ADMIN, inc.getId(),
                "已发起联合现场复核 " + inc.getCode(),
                "街道已组织 " + owners.size() + " 家权属单位现场复核，请关注复核进展。");
        return r;
    }

    /** 受邀单位代表签字认可复核结论 */
    @Transactional
    public ReviewSignature sign(Long incidentId, String signerName, String signImage, String note) {
        User me = AuthContext.requireRole(Constants.Role.OWNER_UNIT);
        JointReview r = getReview(incidentId);
        if (!Constants.ReviewStatus.IN_PROGRESS.equals(r.getStatus())) {
            throw new BizException("复核已结束，不能再签字");
        }
        ReviewSignature s = mySignature(r, me);
        if (Constants.SignStatus.SIGNED.equals(s.getStatus())) {
            throw new BizException("贵单位已签字");
        }
        if (signerName == null || signerName.isBlank()) {
            throw new BizException("请填写签字代表姓名");
        }
        s.setStatus(Constants.SignStatus.SIGNED);
        s.setSignerName(signerName.trim());
        s.setSignImage(signImage);
        s.setNote(note);
        s.setRefuseReason(null);
        s.setRefuseEvidence(null);
        s.setSignedAt(LocalDateTime.now());
        signatureRepo.save(s);

        notificationService.notifyRole(Constants.Role.STREET_DUTY, incidentId,
                "联合复核代表已签字 " + incidentCode(incidentId),
                me.getOrgName() + " 代表 " + signerName + " 已签字认可。");
        if (allSigned(r)) {
            notificationService.notify(r.getInitiatedById(), incidentId,
                    "各单位代表已全部签字 " + incidentCode(incidentId),
                    "联合复核结论已由全部参会单位代表签字，请形成权属结论与费用分摊建议。");
        }
        return s;
    }

    /** 受邀单位拒绝签收：必须说明理由并上传证据 */
    @Transactional
    public ReviewSignature refuse(Long incidentId, String reason, List<String> evidence, String note) {
        User me = AuthContext.requireRole(Constants.Role.OWNER_UNIT);
        JointReview r = getReview(incidentId);
        if (!Constants.ReviewStatus.IN_PROGRESS.equals(r.getStatus())) {
            throw new BizException("复核已结束，不能再提交拒签");
        }
        ReviewSignature s = mySignature(r, me);
        if (reason == null || reason.isBlank()) {
            throw new BizException("拒绝签收必须说明理由");
        }
        if (evidence == null || evidence.isEmpty()) {
            throw new BizException("拒绝签收必须上传佐证材料（照片/图纸等证据）");
        }
        s.setStatus(Constants.SignStatus.REFUSED);
        s.setRefuseReason(reason.trim());
        s.setRefuseEvidence(writeJson(evidence));
        s.setNote(note);
        s.setSignedAt(LocalDateTime.now());
        signatureRepo.save(s);

        notificationService.notifyRole(Constants.Role.STREET_DUTY, incidentId,
                "联合复核出现拒签 " + incidentCode(incidentId),
                me.getOrgName() + " 拒绝签收并提交了理由与证据，若复核仍无法确认将上报市级协调池。");
        return s;
    }

    /**
     * 街道结束复核：CONFIRMED 须全部代表签字，生成权属结论与费用分摊建议；
     * UNRESOLVED 表示仍无法确认，事件进入市级协调池。
     */
    @Transactional
    public JointReview finish(Long incidentId, String conclusion, Long confirmedOwnerId,
                              List<Map<String, Object>> costSuggestion, String conclusionNote) {
        AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.ADMIN);
        Incident inc = incidentService.getOrThrow(incidentId);
        JointReview r = getReview(incidentId);
        if (!Constants.ReviewStatus.IN_PROGRESS.equals(r.getStatus())) {
            throw new BizException("该复核已结束");
        }
        List<ReviewSignature> sigs = signatureRepo.findByReviewIdOrderByCreatedAtAsc(r.getId());

        if ("CONFIRMED".equals(conclusion)) {
            if (confirmedOwnerId == null) throw new BizException("确认权属时必须指定权属单位");
            User owner = userRepo.findById(confirmedOwnerId)
                    .filter(u -> Constants.Role.OWNER_UNIT.equals(u.getRole()))
                    .orElseThrow(() -> new BizException("权属单位不存在"));
            if (sigs.stream().noneMatch(s -> s.getOwnerId().equals(owner.getId()))) {
                throw new BizException("只能在参加现场复核的单位中确认权属");
            }
            if (sigs.stream().anyMatch(s -> !Constants.SignStatus.SIGNED.equals(s.getStatus()))) {
                throw new BizException("存在未签字或拒绝签收的单位，不能形成确认结论；仍无法确认请上报市级协调池");
            }
            r.setConclusion("CONFIRMED");
            r.setConfirmedOwnerId(owner.getId());
            r.setCostSuggestion(writeJson(buildConfirmedSuggestion(sigs, owner, costSuggestion)));
            r.setConclusionNote(conclusionNote);
            r.setStatus(Constants.ReviewStatus.CONFIRMED);
            r.setFinishedAt(LocalDateTime.now());
            reviewRepo.save(r);
            confirmOwner(inc, owner, "联合复核确认", conclusionNote);

            notificationService.notify(owner.getId(), incidentId,
                    "联合复核确认贵单位权属 " + inc.getCode(),
                    "复核结论已由各单位代表签字确认，请尽快派发维修工单，费用将按分摊建议追偿。");
            notificationService.notifyRole(Constants.Role.ADMIN, incidentId,
                    "联合复核已确认权属 " + inc.getCode(), owner.getOrgName() + "，复核流程结束。");
            return r;
        } else if ("UNRESOLVED".equals(conclusion)) {
            return escalate(inc, r, sigs, conclusionNote);
        }
        throw new BizException("复核结论不合法");
    }

    /** 进入市级协调池：维修费用先由市级应急基金垫付，后续按复核结论追偿 */
    private JointReview escalate(Incident inc, JointReview r, List<ReviewSignature> sigs, String note) {
        LocalDateTime now = LocalDateTime.now();
        r.setConclusion("UNRESOLVED");
        r.setStatus(Constants.ReviewStatus.ESCALATED);
        r.setConclusionNote(note);
        r.setEscalationNote(note);
        r.setEscalatedAt(now);
        r.setFinishedAt(now);
        // 默认费用建议：市级应急基金先行垫付，待市级协调后向责任单位追偿
        r.setCostSuggestion(writeJson(List.of(Map.of(
                "party", "市级应急基金（先行垫付，待追偿）",
                "ratio", 100,
                "reason", "联合现场复核仍无法确认权属，应急基金先行垫付，待市级协调认定后追偿"))));
        reviewRepo.save(r);

        inc.setOwnerStatus(Constants.OwnerStatus.CITY_POOL);
        inc.setResponsibilityPending(true);
        inc.setEverCityPool(true);
        incidentService.save(inc);

        long refused = sigs.stream().filter(s -> Constants.SignStatus.REFUSED.equals(s.getStatus())).count();
        notificationService.notifyRole(Constants.Role.ADMIN, inc.getId(),
                "事件进入市级协调池 " + inc.getCode(),
                "联合现场复核仍无法确认权属（含 " + refused + " 家单位拒签材料），请市级协调人员研判；维修费用可先由应急基金垫付。");
        notificationService.notifyRole(Constants.Role.STREET_DUTY, inc.getId(),
                "已上报市级协调池 " + inc.getCode(), "街道处置进展已同步市级协调人员。");
        return r;
    }

    /** 市级协调人员登记应急基金垫付 */
    @Transactional
    public JointReview advanceFund(Long incidentId, BigDecimal amount, String note) {
        User me = AuthContext.requireRole(Constants.Role.ADMIN);
        Incident inc = incidentService.getOrThrow(incidentId);
        JointReview r = getReview(incidentId);
        if (!Constants.ReviewStatus.ESCALATED.equals(r.getStatus())) {
            throw new BizException("只有市级协调池中的事件才能动用应急基金垫付");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("垫付金额必须大于 0");
        }
        BigDecimal before = r.getFundAdvanceAmount() == null ? BigDecimal.ZERO : r.getFundAdvanceAmount();
        r.setFundAdvanceAmount(before.add(amount));
        r.setFundAdvanceAt(LocalDateTime.now());
        r.setFundAdvanceNote(note);
        reviewRepo.save(r);

        notificationService.notifyRole(Constants.Role.STREET_DUTY, incidentId,
                "应急基金已垫付 " + inc.getCode(), "市级应急基金垫付 ¥" + amount + "，后续按协调结论追偿。");
        return r;
    }

    /**
     * 市级协调人员强制认领：须已有拒签单位提交的书面理由与证据，经研判后指定责任单位。
     */
    @Transactional
    public JointReview forceClaim(Long incidentId, Long ownerId, String note) {
        User me = AuthContext.requireRole(Constants.Role.ADMIN);
        Incident inc = incidentService.getOrThrow(incidentId);
        JointReview r = getReview(incidentId);
        if (!Constants.ReviewStatus.ESCALATED.equals(r.getStatus())) {
            throw new BizException("只能对市级协调池中的事件作出强制认领决定");
        }
        List<ReviewSignature> sigs = signatureRepo.findByReviewIdOrderByCreatedAtAsc(r.getId());
        boolean hasRefuseMaterial = sigs.stream().anyMatch(s ->
                Constants.SignStatus.REFUSED.equals(s.getStatus())
                        && s.getRefuseReason() != null && !s.getRefuseReason().isBlank()
                        && !readJson(s.getRefuseEvidence()).isEmpty());
        if (!hasRefuseMaterial) {
            throw new BizException("缺少拒签单位的书面理由与证据材料，不能作出强制认领决定");
        }
        User owner = userRepo.findById(ownerId == null ? -1 : ownerId)
                .filter(u -> Constants.Role.OWNER_UNIT.equals(u.getRole()))
                .orElseThrow(() -> new BizException("权属单位不存在"));

        LocalDateTime now = LocalDateTime.now();
        r.setStatus(Constants.ReviewStatus.CONFIRMED);
        r.setConclusion("CONFIRMED");
        r.setConfirmedOwnerId(owner.getId());
        r.setForcedClaim(true);
        r.setForcedById(me.getId());
        r.setForcedAt(now);
        r.setForcedNote(note);
        BigDecimal advanced = r.getFundAdvanceAmount() == null ? BigDecimal.ZERO : r.getFundAdvanceAmount();
        String reason = "经市级协调依据各单位现场复核意见及拒签证据研判，认定为" + owner.getOrgName()
                + "设施；应急基金已垫付 ¥" + advanced + "，费用按复核结论向其追偿。";
        r.setCostSuggestion(writeJson(List.of(Map.of(
                "party", "权属单位-" + owner.getOrgName(),
                "ratio", 100,
                "reason", reason))));
        reviewRepo.save(r);
        confirmOwner(inc, owner, "市级强制认领", note);

        notificationService.notify(owner.getId(), incidentId,
                "市级强制认领决定 " + inc.getCode(),
                reason + (note == null ? "" : "（决定说明：" + note + "）"));
        notificationService.notifyRole(Constants.Role.STREET_DUTY, incidentId,
                "市级已作出强制认领 " + inc.getCode(),
                owner.getOrgName() + " 被认定为权属单位，请督促派单维修，应急基金垫付款按结论追偿。");
        return r;
    }

    // ---------- 查询 ----------

    /** 联合复核列表（按角色聚合签字进展） */
    public List<Map<String, Object>> listForMe() {
        User me = AuthContext.requireLogin();
        List<JointReview> reviews = switch (me.getRole()) {
            case Constants.Role.ADMIN, Constants.Role.STREET_DUTY -> reviewRepo.findAllByOrderByCreatedAtDesc();
            case Constants.Role.OWNER_UNIT -> reviewRepo.findAllByOrderByCreatedAtDesc().stream()
                    .filter(r -> signatureRepo.findByReviewIdOrderByCreatedAtAsc(r.getId()).stream()
                            .anyMatch(s -> s.getOwnerId().equals(me.getId())))
                    .toList();
            default -> List.of();
        };
        return reviews.stream().map(r -> assemble(r, me)).toList();
    }

    /** 事件详情中的复核聚合数据 */
    public Map<String, Object> detailOfIncident(Long incidentId) {
        JointReview r = reviewRepo.findByIncidentId(incidentId).orElse(null);
        if (r == null) return null;
        return assemble(r, AuthContext.requireLogin());
    }

    private Map<String, Object> assemble(JointReview r, User me) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("review", r);
        Incident inc = incidentService.getOrThrow(r.getIncidentId());
        m.put("incidentCode", inc.getCode());
        m.put("address", inc.getAddress());
        m.put("issueType", inc.getIssueType());
        m.put("dangerLevel", inc.getDangerLevel());
        userRepo.findById(r.getInitiatedById()).ifPresent(u -> {
            m.put("initiatorName", u.getName());
            m.put("initiatorOrg", u.getOrgName());
        });
        if (r.getConfirmedOwnerId() != null) {
            m.put("confirmedOwnerOrg", userRepo.findById(r.getConfirmedOwnerId())
                    .map(User::getOrgName).orElse(null));
        }
        if (r.getForcedById() != null) {
            m.put("forcedByName", userRepo.findById(r.getForcedById()).map(User::getName).orElse(null));
        }
        m.put("costSuggestionList", readSuggestion(r.getCostSuggestion()));

        List<Map<String, Object>> signs = new ArrayList<>();
        long signed = 0, refused = 0, pending = 0;
        for (ReviewSignature s : signatureRepo.findByReviewIdOrderByCreatedAtAsc(r.getId())) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("signature", s);
            sm.put("ownerOrg", userRepo.findById(s.getOwnerId()).map(User::getOrgName).orElse("未知"));
            sm.put("ownerName", userRepo.findById(s.getOwnerId()).map(User::getName).orElse("未知"));
            sm.put("evidenceList", readJson(s.getRefuseEvidence()));
            sm.put("mine", s.getOwnerId().equals(me.getId()));
            signs.add(sm);
            if (Constants.SignStatus.SIGNED.equals(s.getStatus())) signed++;
            else if (Constants.SignStatus.REFUSED.equals(s.getStatus())) refused++;
            else pending++;
        }
        m.put("signatures", signs);
        m.put("signedCount", signed);
        m.put("refusedCount", refused);
        m.put("pendingCount", pending);
        return m;
    }

    // ---------- 内部工具 ----------

    private JointReview getReview(Long incidentId) {
        return reviewRepo.findByIncidentId(incidentId)
                .orElseThrow(() -> new BizException("该事件尚未发起联合现场复核"));
    }

    private ReviewSignature mySignature(JointReview r, User me) {
        return signatureRepo.findByReviewIdOrderByCreatedAtAsc(r.getId()).stream()
                .filter(s -> s.getOwnerId().equals(me.getId()))
                .findFirst()
                .orElseThrow(() -> new BizException("贵单位未被邀请参加本次现场复核"));
    }

    private boolean allSigned(JointReview r) {
        List<ReviewSignature> sigs = signatureRepo.findByReviewIdOrderByCreatedAtAsc(r.getId());
        return !sigs.isEmpty() && sigs.stream().allMatch(s -> Constants.SignStatus.SIGNED.equals(s.getStatus()));
    }

    /** 确认结论的费用分摊建议：默认确认单位 100%；前端可传入多方比例（合计须 100） */
    private List<Map<String, Object>> buildConfirmedSuggestion(List<ReviewSignature> sigs, User owner,
                                                               List<Map<String, Object>> custom) {
        if (custom != null && !custom.isEmpty()) {
            int ratioSum = 0;
            for (Map<String, Object> a : custom) {
                Object party = a.get("party");
                if (party == null || party.toString().isBlank()) {
                    throw new BizException("分摊建议承担方不能为空");
                }
                Object ratio = a.get("ratio");
                if (ratio == null) throw new BizException("分摊建议须填写比例");
                int rr = Integer.parseInt(ratio.toString());
                if (rr < 0) throw new BizException("分摊比例不能为负");
                ratioSum += rr;
            }
            if (ratioSum != 100) throw new BizException("费用分摊建议比例合计须为 100%");
            return custom;
        }
        return List.of(Map.of(
                "party", "权属单位-" + owner.getOrgName(),
                "ratio", 100,
                "reason", "联合现场复核确认为" + owner.getOrgName() + "设施，各单位代表已签字"));
    }

    private void confirmOwner(Incident inc, User owner, String claimType, String note) {
        OwnerClaim claim = new OwnerClaim();
        claim.setIncidentId(inc.getId());
        claim.setOwnerId(owner.getId());
        claim.setClaimType(claimType);
        claim.setNote(note);
        claimRepo.save(claim);

        inc.setOwnerId(owner.getId());
        inc.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        inc.setResponsibilityPending(false);
        if (Constants.IncidentStatus.REPORTED.equals(inc.getStatus())
                || Constants.IncidentStatus.SECURED.equals(inc.getStatus())) {
            inc.setStatus(Constants.IncidentStatus.OWNER_CONFIRMED);
        }
        incidentService.save(inc);
    }

    private String incidentCode(Long incidentId) {
        return incidentService.getOrThrow(incidentId).getCode();
    }

    private static final DateTimeFormatter[] TIME_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    };

    private LocalDateTime parseTime(String text) {
        if (text == null || text.isBlank()) return null;
        for (DateTimeFormatter f : TIME_FORMATS) {
            try {
                return LocalDateTime.parse(text.trim(), f);
            } catch (Exception ignored) {
                // try next format
            }
        }
        throw new BizException("时间格式不正确（yyyy-MM-dd HH:mm）");
    }

    private String writeJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new BizException("JSON 序列化失败");
        }
    }

    private List<String> readJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readSuggestion(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
