package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.common.AuthContext;
import com.city.manhole.common.Constants;
import com.city.manhole.dto.Req;
import com.city.manhole.model.EmergencyFundAdvance;
import com.city.manhole.model.JointReview;
import com.city.manhole.model.JointReviewSignoff;
import com.city.manhole.service.JointReviewService;
import com.city.manhole.service.ReviewQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 联合现场复核与市级协调：
 * 街道发起复核、各权属单位代表签字/拒签、街道出具结论、
 * 市级协调池强制认领、应急基金垫付与追偿。
 */
@RestController
@RequiredArgsConstructor
public class JointReviewController {
    private final JointReviewService jointReviewService;
    private final ReviewQueryService queryService;

    /** 街道发起联合现场复核 */
    @PostMapping("/api/incidents/{incidentId}/joint-reviews")
    public ApiResponse<JointReview> initiate(@PathVariable Long incidentId,
                                             @RequestBody Req.JointReviewInitReq req) {
        return ApiResponse.ok(jointReviewService.initiate(incidentId,
                req.scheduledAt(), req.siteLocation(), req.reason()));
    }

    /** 事件的复核记录（含各方签收情况、费用分摊建议、垫付单） */
    @GetMapping("/api/incidents/{incidentId}/joint-reviews")
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable Long incidentId) {
        return ApiResponse.ok(queryService.reviewsOf(incidentId));
    }

    /** 权属单位代表签字 / 拒签（拒签须理由+证据） */
    @PostMapping("/api/joint-reviews/{id}/sign")
    public ApiResponse<JointReviewSignoff> sign(@PathVariable Long id,
                                                @RequestBody Req.JointReviewSignReq req) {
        return ApiResponse.ok(jointReviewService.sign(id, req.agree(), req.representativeName(),
                req.opinion(), req.refuseReason(), req.evidencePhotos()));
    }

    /** 街道结束复核：确认权属（CONFIRMED）或仍无法确认上报市级（DISPUTED） */
    @PostMapping("/api/joint-reviews/{id}/conclude")
    public ApiResponse<JointReview> conclude(@PathVariable Long id,
                                             @RequestBody Req.JointReviewConcludeReq req) {
        return ApiResponse.ok(jointReviewService.conclude(id, req.conclusion(), req.confirmedOwnerId(),
                req.siteFindings(), req.costSuggestion()));
    }

    /** 市级协调池列表（仅市级协调人员/管理员） */
    @GetMapping("/api/city/pool")
    public ApiResponse<List<Map<String, Object>>> pool() {
        AuthContext.requireRole(Constants.Role.CITY_COORDINATOR, Constants.Role.ADMIN);
        return ApiResponse.ok(queryService.pool());
    }

    /** 应急基金垫付/追偿台账（仅市级协调人员/管理员） */
    @GetMapping("/api/city/advances")
    public ApiResponse<List<Map<String, Object>>> advances() {
        AuthContext.requireRole(Constants.Role.CITY_COORDINATOR, Constants.Role.ADMIN);
        return ApiResponse.ok(queryService.advances());
    }

    /** 市级协调人员强制认领 */
    @PostMapping("/api/joint-reviews/{id}/force-claim")
    public ApiResponse<JointReview> forceClaim(@PathVariable Long id,
                                               @RequestBody Req.ForceClaimReq req) {
        return ApiResponse.ok(jointReviewService.forceClaim(id, req.ownerId(), req.reason(),
                req.costSuggestion()));
    }

    /** 市级协调人员批准应急基金垫付 */
    @PostMapping("/api/joint-reviews/{id}/advance")
    public ApiResponse<EmergencyFundAdvance> advance(@PathVariable Long id,
                                                     @RequestBody Req.FundAdvanceReq req) {
        return ApiResponse.ok(jointReviewService.advanceFund(id, req.amount(), req.purpose()));
    }

    /** 按复核结论追偿回补应急基金 */
    @PostMapping("/api/emergency-advances/{id}/recover")
    public ApiResponse<EmergencyFundAdvance> recover(@PathVariable Long id,
                                                     @RequestBody(required = false) Req.FundRecoverReq req) {
        return ApiResponse.ok(jointReviewService.recover(id, req == null ? null : req.amount()));
    }
}
