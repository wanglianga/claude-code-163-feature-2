package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.dto.Req;
import com.city.manhole.model.JointReview;
import com.city.manhole.model.ReviewSignature;
import com.city.manhole.service.JointReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 联合现场复核：发起 → 代表签字/拒签（理由+证据）→ 形成权属结论与费用分摊建议；
 * 仍无法确认进入市级协调池 → 应急基金垫付 → 市级协调人员研判后强制认领。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class JointReviewController {
    private final JointReviewService reviewService;

    /** 联合复核工作台列表（街道/市级：全部；权属单位：本单位被邀请的） */
    @GetMapping("/joint-reviews")
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(reviewService.listForMe());
    }

    /** 街道发起联合现场复核 */
    @PostMapping("/incidents/{incidentId}/joint-review")
    public ApiResponse<JointReview> initiate(@PathVariable Long incidentId,
                                             @RequestBody Req.ReviewInitiateReq req) {
        return ApiResponse.ok(reviewService.initiate(incidentId, req.ownerIds(), req.manholeCode(),
                req.pipelineMap(), req.latestPermit(), req.repairHistory(), req.scheduledAt(),
                req.location(), req.note()));
    }

    /** 权属单位代表签字 */
    @PostMapping("/incidents/{incidentId}/joint-review/sign")
    public ApiResponse<ReviewSignature> sign(@PathVariable Long incidentId,
                                             @RequestBody Req.ReviewSignReq req) {
        return ApiResponse.ok(reviewService.sign(incidentId, req.signerName(), req.signImage(), req.note()));
    }

    /** 权属单位代表拒绝签收（必须说明理由并上传证据） */
    @PostMapping("/incidents/{incidentId}/joint-review/refuse")
    public ApiResponse<ReviewSignature> refuse(@PathVariable Long incidentId,
                                               @RequestBody Req.ReviewRefuseReq req) {
        return ApiResponse.ok(reviewService.refuse(incidentId, req.reason(), req.evidence(), req.note()));
    }

    /** 街道结束复核：CONFIRMED（全部签字）生成权属结论与分摊建议 / UNRESOLVED 进入市级协调池 */
    @PostMapping("/incidents/{incidentId}/joint-review/finish")
    public ApiResponse<JointReview> finish(@PathVariable Long incidentId,
                                           @RequestBody Req.ReviewFinishReq req) {
        return ApiResponse.ok(reviewService.finish(incidentId, req.conclusion(), req.confirmedOwnerId(),
                req.costSuggestion(), req.conclusionNote()));
    }

    /** 市级协调人员登记应急基金垫付 */
    @PostMapping("/incidents/{incidentId}/joint-review/fund-advance")
    public ApiResponse<JointReview> fundAdvance(@PathVariable Long incidentId,
                                                @RequestBody Req.FundAdvanceReq req) {
        return ApiResponse.ok(reviewService.advanceFund(incidentId, req.amount(), req.note()));
    }

    /** 市级协调人员依据拒签理由与证据研判后强制认领 */
    @PostMapping("/incidents/{incidentId}/joint-review/force-claim")
    public ApiResponse<JointReview> forceClaim(@PathVariable Long incidentId,
                                               @RequestBody Req.ForceClaimReq req) {
        return ApiResponse.ok(reviewService.forceClaim(incidentId, req.ownerId(), req.note()));
    }
}
