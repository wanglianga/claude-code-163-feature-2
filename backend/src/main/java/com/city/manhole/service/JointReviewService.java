package com.city.manhole.service;

import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.common.Constants;
import com.city.manhole.model.*;
import com.city.manhole.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 权属单位互相推诿时的联合现场复核：
 * 街道发起 → 燃气/通信/排水等单位代表现场核验并签字（拒签须说明理由并上传证据）
 * → 出具权属结论与费用分摊建议；仍无法确认则进入市级协调池，
 * 应急基金先行垫付，市级协调人员依据各方意见决定是否强制认领，事后追偿。
 */
@Service
@RequiredArgsConstructor
public class JointReviewService {
    private final JointReviewRepo reviewRepo;
    private final JointReviewSignoffRepo signoffRepo;
    private final EmergencyFundAdvanceRepo advanceRepo;
    private final EmergencyMeasureRepo emergencyRepo;
    private final OwnerClaimRepo claimRepo;
    private final UserRepo userRepo;
    private final IncidentService incidentService;
    private final ReviewQueryService queryService;
    private final NotificationService notificationService;

    /** 街道发起联合现场复核，通知全部权属单位派代表到场 */
    @Transactional
    public JointReview initiate(Long incidentId, LocalDateTime scheduledAt, String siteLocation, String reason) {
        User me = AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.ADMIN);
        Incident inc = incidentService.getOrThrow(incidentId);
        if (Constants.OwnerStatus.CONFIRMED.equals(inc.getOwnerStatus())) {
            throw new BizException("权属已确认，无需联合复核");
        }
        if (Constants.OwnerStatus.JOINT_REVIEW.equals(inc.getOwnerStatus())) {
            throw new BizException("该事件已在联合现场复核中");
        }
        if (Constants.OwnerStatus.ESCALATED.equals(inc.getOwnerStatus())) {
            throw new BizException("该事件已进入市级协调池，请由市级协调人员处理");
        }
        if (!Constants.EmergencyStatus.SECURED.equals(inc.getEmergencyStatus())) {
            throw new BizException("请先完成现场临时围护，再发起联合现场复核");
        }
        if (!Constants.OwnerStatus.INVESTIGATING.equals(inc.getOwnerStatus())
                && !Constants.OwnerStatus.PENDING.equals(inc.getOwnerStatus())) {
            throw new BizException("当前权属状态不能发起联合复核");
        }
        if (scheduledAt == null) throw new BizException("请选择现场复核时间");
        if (reason == null || reason.isBlank()) throw new BizException("请填写发起联合复核的事由");

        JointReview r = new JointReview();
        r.setIncidentId(incidentId);
        r.setInitiatorId(me.getId());
        r.setScheduledAt(scheduledAt);
        r.setSiteLocation(siteLocation);
        r.setReason(reason.trim());
        r.setStatus(Constants.JointReviewStatus.INITIATED);
        reviewRepo.save(r);

        // 通知所有权属单位（燃气/电力/通信/市政排水）派代表到场签字
        List<User> owners = userRepo.findByRole(Constants.Role.OWNER_UNIT);
        if (owners.isEmpty()) throw new BizException("系统中尚无权属单位账号，无法发起联合复核");
        for (User u : owners) {
            JointReviewSignoff s = new JointReviewSignoff();
            s.setReviewId(r.getId());
            s.setOwnerId(u.getId());
            s.setUnitType(ReviewQueryService.unitTypeOf(u.getOrgName()));
            s.setStatus(Constants.SignStatus.PENDING);
            signoffRepo.save(s);
            notificationService.notify(u.getId(), inc.getId(),
                    "请参加联合现场复核 " + inc.getCode(),
                    "现场复核时间：" + scheduledAt + "，地点：" + (siteLocation == null ? inc.getAddress() : siteLocation)
                            + "。请派代表到场核验并在复核记录上签字；如不认可归属，须说明理由并上传证据。");
        }

        inc.setOwnerStatus(Constants.OwnerStatus.JOINT_REVIEW);
        incidentService.save(inc);
        notificationService.notifyRole(Constants.Role.CITY_COORDINATOR, inc.getId(),
                "联合现场复核已发起 " + inc.getCode(), reason);
        return r;
    }

    /** 权属单位代表签收：签字认可，或拒绝签收（必须填理由 + 上传证据） */
    @Transactional
    public JointReviewSignoff sign(Long reviewId, Boolean agree, String representativeName,
                                   String opinion, String refuseReason, List<String> evidencePhotos) {
        User me = AuthContext.requireRole(Constants.Role.OWNER_UNIT);
        JointReview r = reviewRepo.findById(reviewId).orElseThrow(() -> new BizException("复核记录不存在"));
        if (!Constants.JointReviewStatus.INITIATED.equals(r.getStatus())) {
            throw new BizException("该复核已结束，不能再签收");
        }
        JointReviewSignoff s = signoffRepo.findByReviewIdAndOwnerId(reviewId, me.getId())
                .orElseThrow(() -> new BizException("贵单位未被邀请参加本次联合复核"));
        if (!Constants.SignStatus.PENDING.equals(s.getStatus())) {
            throw new BizException("贵单位已完成签收");
        }
        String repName = representativeName == null || representativeName.isBlank() ? me.getName() : representativeName.trim();
        s.setRepresentativeName(repName);
        s.setOpinion(opinion);

        if (Boolean.FALSE.equals(agree)) {
            if (refuseReason == null || refuseReason.isBlank()) {
                throw new BizException("拒绝签收必须说明理由");
            }
            if (evidencePhotos == null || evidencePhotos.isEmpty()) {
                throw new BizException("拒绝签收必须上传佐证证据（现场照片/图纸/台账等）");
            }
            s.setStatus(Constants.SignStatus.REFUSED);
            s.setRefuseReason(refuseReason.trim());
            s.setEvidencePhotos(incidentService.writeJson(evidencePhotos));
            notificationService.notifyRole(Constants.Role.STREET_DUTY, r.getIncidentId(),
                    "单位拒签 " + me.getOrgName(), "拒签理由：" + refuseReason);
        } else {
            s.setStatus(Constants.SignStatus.SIGNED);
            s.setSignedAt(LocalDateTime.now());
            notificationService.notifyRole(Constants.Role.STREET_DUTY, r.getIncidentId(),
                    "单位代表已签字 " + me.getOrgName(), "代表：" + repName + (opinion == null ? "" : "；意见：" + opinion));
        }
        return signoffRepo.save(s);
    }

    /**
     * 街道结束联合复核：
     * CONFIRMED 各单位代表签字确认权属（被确认单位必须已签字）；
     * DISPUTED  仍无法确认 → 事件进入市级协调池。
     */
    @Transactional
    public JointReview conclude(Long reviewId, String conclusion, Long confirmedOwnerId,
                                String siteFindings, List<Map<String, Object>> costSuggestion) {
        AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.ADMIN);
        JointReview r = reviewRepo.findById(reviewId).orElseThrow(() -> new BizException("复核记录不存在"));
        if (!Constants.JointReviewStatus.INITIATED.equals(r.getStatus())) {
            throw new BizException("该复核已结束");
        }
        List<JointReviewSignoff> signoffs = signoffRepo.findByReviewIdOrderByIdAsc(r.getId());
        if (signoffs.stream().anyMatch(s -> Constants.SignStatus.PENDING.equals(s.getStatus()))) {
            throw new BizException("仍有单位代表未签字/未表态，各方签收完成后才能出具复核结论");
        }
        r.setSiteFindings(siteFindings);
        LocalDateTime now = LocalDateTime.now();

        if ("CONFIRMED".equals(conclusion)) {
            if (confirmedOwnerId == null) throw new BizException("确认权属时必须选择权属单位");
            User owner = userRepo.findById(confirmedOwnerId)
                    .filter(u -> Constants.Role.OWNER_UNIT.equals(u.getRole()))
                    .orElseThrow(() -> new BizException("权属单位不存在"));
            JointReviewSignoff target = signoffs.stream()
                    .filter(s -> s.getOwnerId().equals(confirmedOwnerId)).findFirst()
                    .orElseThrow(() -> new BizException("该单位未参加本次联合复核"));
            if (!Constants.SignStatus.SIGNED.equals(target.getStatus())) {
                throw new BizException("被认定单位拒绝签收，街道不能径行确认，应作“仍无法确认”上报市级协调池");
            }
            r.setConclusion("CONFIRMED");
            r.setConfirmedOwnerId(confirmedOwnerId);
            r.setStatus(Constants.JointReviewStatus.CONCLUDED);
            r.setConcludedAt(now);
            r.setCostSuggestion(incidentService.writeJson(
                    costSuggestion != null && !costSuggestion.isEmpty()
                            ? costSuggestion : defaultSuggestion(owner, "联合现场复核各单位代表签字确认权属")));
            reviewRepo.save(r);
            applyOwner(r, owner, "联合复核确认");
        } else if ("DISPUTED".equals(conclusion)) {
            r.setConclusion("DISPUTED");
            r.setStatus(Constants.JointReviewStatus.ESCALATED);
            r.setEscalatedAt(now);
            r.setConcludedAt(now);
            r.setCostSuggestion(incidentService.writeJson(
                    costSuggestion != null && !costSuggestion.isEmpty()
                            ? costSuggestion : List.of(Map.of(
                            "party", "应急基金（先行垫付）",
                            "amount", currentCost(r.getIncidentId()),
                            "reason", "各单位代表到场后仍无法确认权属，费用先由应急基金垫付，待市级协调结论追偿"))));
            reviewRepo.save(r);

            Incident inc = incidentService.getOrThrow(r.getIncidentId());
            inc.setOwnerStatus(Constants.OwnerStatus.ESCALATED);
            incidentService.save(inc);
            long refused = signoffs.stream().filter(s -> Constants.SignStatus.REFUSED.equals(s.getStatus())).count();
            notificationService.notifyRole(Constants.Role.CITY_COORDINATOR, inc.getId(),
                    "事件进入市级协调池 " + inc.getCode(),
                    "联合复核仍无法确认权属（" + signoffs.size() + " 家单位到场，" + refused
                            + " 家拒签并提交证据），请审阅各方意见后决定强制认领或继续协调；维修费用可由应急基金先行垫付。");
            notificationService.notifyRole(Constants.Role.ADMIN, inc.getId(),
                    "事件进入市级协调池 " + inc.getCode(), "等待市级协调结论与费用追偿安排。");
        } else {
            throw new BizException("复核结论不合法");
        }
        return r;
    }

    /** 市级协调人员审阅拒签理由与证据后，决定强制认领 */
    @Transactional
    public JointReview forceClaim(Long reviewId, Long ownerId, String reason, List<Map<String, Object>> costSuggestion) {
        User me = AuthContext.requireRole(Constants.Role.CITY_COORDINATOR);
        JointReview r = reviewRepo.findById(reviewId).orElseThrow(() -> new BizException("复核记录不存在"));
        if (!Constants.JointReviewStatus.ESCALATED.equals(r.getStatus())) {
            throw new BizException("只有市级协调池中的复核事件才能强制认领");
        }
        if (ownerId == null) throw new BizException("请选择强制认领单位");
        if (reason == null || reason.isBlank()) throw new BizException("强制认领必须说明决定理由");
        User owner = userRepo.findById(ownerId)
                .filter(u -> Constants.Role.OWNER_UNIT.equals(u.getRole()))
                .orElseThrow(() -> new BizException("权属单位不存在"));
        signoffRepo.findByReviewIdAndOwnerId(reviewId, ownerId)
                .orElseThrow(() -> new BizException("该单位未参加联合复核，不能强制其认领"));

        LocalDateTime now = LocalDateTime.now();
        r.setConfirmedOwnerId(ownerId);
        r.setConclusion("CONFIRMED");
        r.setStatus(Constants.JointReviewStatus.FORCED);
        r.setForcedBy(me.getId());
        r.setForcedAt(now);
        r.setForceReason(reason.trim());
        r.setCostSuggestion(incidentService.writeJson(
                costSuggestion != null && !costSuggestion.isEmpty()
                        ? costSuggestion : defaultSuggestion(owner, "市级协调强制认领，承担全部费用并返还应急基金垫付款")));
        reviewRepo.save(r);
        applyOwner(r, owner, "市级强制认领");

        // 已由应急基金垫付的，锁定追偿对象
        advanceRepo.findByIncidentId(r.getIncidentId()).ifPresent(a -> {
            a.setRecoverFromOwnerId(ownerId);
            advanceRepo.save(a);
        });
        notificationService.notify(owner.getId(), r.getIncidentId(),
                "市级协调强制认领通知 " + incidentService.getOrThrow(r.getIncidentId()).getCode(),
                "经市级协调审阅现场核验记录及各方证据，决定由贵单位强制认领该井盖。理由：" + reason
                        + "。应急基金垫付款将按复核结论向贵单位追偿。");
        return r;
    }

    /** 市级协调人员批准应急基金先行垫付维修费 */
    @Transactional
    public EmergencyFundAdvance advanceFund(Long reviewId, BigDecimal amount, String purpose) {
        User me = AuthContext.requireRole(Constants.Role.CITY_COORDINATOR);
        JointReview r = reviewRepo.findById(reviewId).orElseThrow(() -> new BizException("复核记录不存在"));
        if (!Constants.JointReviewStatus.ESCALATED.equals(r.getStatus())) {
            throw new BizException("只有市级协调池中的事件才能动用应急基金垫付");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("垫付金额必须大于 0");
        if (advanceRepo.findByIncidentId(r.getIncidentId()).isPresent()) {
            throw new BizException("该事件已存在应急基金垫付记录");
        }
        EmergencyFundAdvance a = new EmergencyFundAdvance();
        a.setIncidentId(r.getIncidentId());
        a.setReviewId(r.getId());
        a.setApprovedBy(me.getId());
        a.setAmount(amount);
        a.setPurpose(purpose);
        a.setStatus(Constants.RecoveryStatus.PENDING);
        advanceRepo.save(a);

        r.setAdvanceAmount(amount);
        reviewRepo.save(r);
        notificationService.notifyRole(Constants.Role.STREET_DUTY, r.getIncidentId(),
                "应急基金垫付已批准 " + incidentService.getOrThrow(r.getIncidentId()).getCode(),
                "垫付金额 ¥" + amount + "，用途：" + (purpose == null ? "应急维修" : purpose) + "；待权属结论出具后追偿。");
        return a;
    }

    /** 追偿到账，回补应急基金 */
    @Transactional
    public EmergencyFundAdvance recover(Long advanceId, BigDecimal amount) {
        AuthContext.requireRole(Constants.Role.CITY_COORDINATOR, Constants.Role.ADMIN);
        EmergencyFundAdvance a = advanceRepo.findById(advanceId)
                .orElseThrow(() -> new BizException("垫付记录不存在"));
        if (Constants.RecoveryStatus.RECOVERED.equals(a.getStatus())) {
            throw new BizException("该垫付款已追偿回补");
        }
        if (a.getRecoverFromOwnerId() == null) {
            throw new BizException("尚未确定追偿对象（须先出具复核/强制认领结论）");
        }
        BigDecimal amt = amount == null ? a.getAmount() : amount;
        if (amt.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("追偿金额必须大于 0");
        a.setRecoveredAmount(amt);
        a.setStatus(Constants.RecoveryStatus.RECOVERED);
        a.setRecoveredAt(LocalDateTime.now());
        return advanceRepo.save(a);
    }

    // ------------------------------------------------------------------

    /** 复核结论应用到事件：写入权属、认领记录，进入待派单状态 */
    private void applyOwner(JointReview r, User owner, String claimType) {
        Incident inc = incidentService.getOrThrow(r.getIncidentId());

        OwnerClaim claim = new OwnerClaim();
        claim.setIncidentId(inc.getId());
        claim.setOwnerId(owner.getId());
        claim.setClaimType(claimType);
        claim.setNote("联合复核结论：" + (r.getSiteFindings() == null ? "" : r.getSiteFindings()));
        claimRepo.save(claim);

        inc.setOwnerId(owner.getId());
        inc.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        inc.setResponsibilityPending(false);
        if (Constants.IncidentStatus.REPORTED.equals(inc.getStatus())
                || Constants.IncidentStatus.SECURED.equals(inc.getStatus())) {
            inc.setStatus(Constants.IncidentStatus.OWNER_CONFIRMED);
        }
        incidentService.save(inc);

        notificationService.notify(owner.getId(), inc.getId(),
                "权属已确认 " + inc.getCode(), "经联合复核该井盖确认为贵单位权属（" + claimType + "），请尽快派发维修工单。");
        notificationService.notifyRole(Constants.Role.STREET_DUTY, inc.getId(),
                "联合复核权属结论已出具 " + inc.getCode(), owner.getOrgName() + "（" + claimType + "），费用分摊建议已生成。");
    }

    private BigDecimal currentCost(Long incidentId) {
        return emergencyRepo.findByIncidentId(incidentId)
                .map(EmergencyMeasure::getCost).orElse(BigDecimal.ZERO);
    }

    private List<Map<String, Object>> defaultSuggestion(User owner, String reason) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("party", "权属单位-" + owner.getOrgName());
        m.put("amount", BigDecimal.ZERO);
        m.put("reason", reason + "；最终金额以验收后的费用复核单为准");
        list.add(m);
        return list;
    }
}
