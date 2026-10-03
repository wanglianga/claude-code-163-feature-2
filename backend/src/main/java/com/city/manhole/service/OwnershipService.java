package com.city.manhole.service;

import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.common.Constants;
import com.city.manhole.model.Incident;
import com.city.manhole.model.OwnerClaim;
import com.city.manhole.model.OwnershipInvestigation;
import com.city.manhole.model.User;
import com.city.manhole.repo.OwnerClaimRepo;
import com.city.manhole.repo.InvestigationRepo;
import com.city.manhole.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 权属追责线：排查（五类证据）→ 确认/责任待定 → 权属单位认领。
 */
@Service
@RequiredArgsConstructor
public class OwnershipService {
    private final InvestigationRepo investigationRepo;
    private final OwnerClaimRepo claimRepo;
    private final UserRepo userRepo;
    private final IncidentService incidentService;
    private final NotificationService notificationService;

    /**
     * 街道值班员提交权属排查结论。
     * conclusion = CONFIRMED（指定权属单位）或 PENDING（无法确认，责任待定，街道先行处置并垫付）。
     */
    @Transactional
    public OwnershipInvestigation investigate(Long incidentId, String manholeMarking, String pipelineInfo,
                                              String constructionHistory, String merchantInfo, String cityLedger,
                                              String conclusion, Long confirmedOwnerId, String note) {
        User me = AuthContext.requireRole(Constants.Role.STREET_DUTY, Constants.Role.ADMIN);
        Incident inc = incidentService.getOrThrow(incidentId);
        if (Constants.OwnerStatus.CONFIRMED.equals(inc.getOwnerStatus())) {
            throw new BizException("权属已确认，无需再排查");
        }
        if (Constants.OwnerStatus.JOINT_REVIEW.equals(inc.getOwnerStatus())
                || Constants.OwnerStatus.CITY_POOL.equals(inc.getOwnerStatus())) {
            throw new BizException("事件已进入联合复核/市级协调流程，不能再提交普通排查结论");
        }
        if (!Constants.OwnerStatus.INVESTIGATING.equals(inc.getOwnerStatus())
                && !Constants.OwnerStatus.PENDING.equals(inc.getOwnerStatus())) {
            throw new BizException("请先完成应急围护再开展权属排查");
        }

        OwnershipInvestigation inv = new OwnershipInvestigation();
        inv.setIncidentId(incidentId);
        inv.setInvestigatorId(me.getId());
        inv.setManholeMarking(manholeMarking);
        inv.setPipelineInfo(pipelineInfo);
        inv.setConstructionHistory(constructionHistory);
        inv.setMerchantInfo(merchantInfo);
        inv.setCityLedger(cityLedger);
        inv.setNote(note);

        if ("CONFIRMED".equals(conclusion)) {
            if (confirmedOwnerId == null) throw new BizException("确认权属时必须选择权属单位");
            User owner = userRepo.findById(confirmedOwnerId)
                    .filter(u -> Constants.Role.OWNER_UNIT.equals(u.getRole()))
                    .orElseThrow(() -> new BizException("权属单位不存在"));
            inv.setConclusion("CONFIRMED");
            inv.setConfirmedOwnerId(confirmedOwnerId);
            investigationRepo.save(inv);
            confirmOwner(inc, owner, "排查指定", note);
        } else if ("PENDING".equals(conclusion)) {
            inv.setConclusion("PENDING");
            investigationRepo.save(inv);
            // 责任待定：街道先行处置、垫付费用，归属后续复核
            inc.setOwnerStatus(Constants.OwnerStatus.PENDING);
            inc.setResponsibilityPending(true);
            inc.setEverPending(true);
            incidentService.save(inc);
            notificationService.notifyRole(Constants.Role.OWNER_UNIT, inc.getId(),
                    "待认领井盖事件 " + inc.getCode(),
                    inc.getAddress() + " 井盖" + inc.getIssueType() + "，权属暂未确认，请相关单位核实认领。");
            notificationService.notifyRole(Constants.Role.ADMIN, inc.getId(),
                    "权属责任待定 " + inc.getCode(), "街道已先行处置并垫付费用，待后续复核分摊。");
        } else {
            throw new BizException("排查结论不合法");
        }
        return inv;
    }

    /** 权属单位主动认领 */
    @Transactional
    public OwnerClaim claim(Long incidentId, String note) {
        User me = AuthContext.requireRole(Constants.Role.OWNER_UNIT);
        Incident inc = incidentService.getOrThrow(incidentId);
        if (Constants.OwnerStatus.CONFIRMED.equals(inc.getOwnerStatus())) {
            throw new BizException("该事件权属已确认");
        }
        if (Constants.OwnerStatus.JOINT_REVIEW.equals(inc.getOwnerStatus())) {
            throw new BizException("该事件正在联合现场复核，请由代表在复核单上签字确认");
        }
        if (Constants.OwnerStatus.CITY_POOL.equals(inc.getOwnerStatus())) {
            throw new BizException("该事件已进入市级协调池，权属由市级协调人员研判决定");
        }
        return confirmOwner(inc, me, "主动认领", note);
    }

    private OwnerClaim confirmOwner(Incident inc, User owner, String claimType, String note) {
        OwnerClaim claim = new OwnerClaim();
        claim.setIncidentId(inc.getId());
        claim.setOwnerId(owner.getId());
        claim.setClaimType(claimType);
        claim.setNote(note);
        claimRepo.save(claim);

        inc.setOwnerId(owner.getId());
        inc.setOwnerStatus(Constants.OwnerStatus.CONFIRMED);
        // 责任待定被认领后，保留 everPending 用于争议统计，清除待定标记
        inc.setResponsibilityPending(false);
        if (Constants.IncidentStatus.REPORTED.equals(inc.getStatus())
                || Constants.IncidentStatus.SECURED.equals(inc.getStatus())) {
            inc.setStatus(Constants.IncidentStatus.OWNER_CONFIRMED);
        }
        incidentService.save(inc);

        notificationService.notify(owner.getId(), inc.getId(),
                "权属已确认 " + inc.getCode(), "请尽快派发维修工单。");
        notificationService.notifyRole(Constants.Role.STREET_DUTY, inc.getId(),
                "权属已确认 " + inc.getCode(), owner.getOrgName() + "（" + claimType + "）");
        return claim;
    }
}
