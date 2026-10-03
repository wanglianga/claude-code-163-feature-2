package com.city.manhole.service;

import com.city.manhole.model.*;
import com.city.manhole.repo.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 联合复核只读视图装配：井盖档案证据（编号/管线图/施工许可/维修记录）、
 * 复核单及其代表签收记录、市级协调池、应急基金垫付单。
 * 不依赖 IncidentService，避免与 JointReviewService 构造器循环依赖。
 */
@Service
@RequiredArgsConstructor
public class ReviewQueryService {
    private final ManholeProfileRepo profileRepo;
    private final ConstructionPermitRepo permitRepo;
    private final RepairRecordRepo repairRecordRepo;
    private final JointReviewRepo reviewRepo;
    private final JointReviewSignoffRepo signoffRepo;
    private final EmergencyFundAdvanceRepo advanceRepo;
    private final IncidentRepo incidentRepo;
    private final UserRepo userRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    /** 定位事件关联的井盖档案：优先事件上的 manholeCode，其次按地址匹配 */
    public ManholeProfile profileOf(Incident inc) {
        if (inc.getManholeCode() != null && !inc.getManholeCode().isBlank()) {
            return profileRepo.findByManholeCode(inc.getManholeCode()).orElse(null);
        }
        if (inc.getAddress() != null) {
            return profileRepo.findFirstByAddressContainingOrderByIdAsc(inc.getAddress()).orElse(null);
        }
        return null;
    }

    /** 推诿复核证据包：井盖编号、地下管线图、最近一次施工许可、历史维修记录 */
    public Map<String, Object> evidence(Incident inc) {
        Map<String, Object> m = new LinkedHashMap<>();
        ManholeProfile p = profileOf(inc);
        m.put("profile", p);
        if (p == null) return m;
        m.put("pipelinePhotoList", readList(p.getPipelinePhotos()));
        if (p.getRegisteredOwnerId() != null) {
            m.put("registeredOwnerOrg",
                    userRepo.findById(p.getRegisteredOwnerId()).map(User::getOrgName).orElse(null));
        }
        List<ConstructionPermit> permits = permitRepo.findByProfileIdOrderByStartedAtDesc(p.getId());
        m.put("latestPermit", permits.isEmpty() ? null : permits.get(0));
        List<RepairRecord> records = repairRecordRepo.findByProfileIdOrderByRepairedAtDesc(p.getId());
        m.put("repairRecords", records.size() > 5 ? records.subList(0, 5) : records);
        return m;
    }

    public List<Map<String, Object>> reviewsOf(Long incidentId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JointReview r : reviewRepo.findByIncidentIdOrderByCreatedAtDesc(incidentId)) {
            list.add(reviewMap(r));
        }
        return list;
    }

    public Map<String, Object> reviewMap(JointReview r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("review", r);
        Incident inc = incidentRepo.findById(r.getIncidentId()).orElse(null);
        m.put("incidentCode", inc == null ? null : inc.getCode());
        m.put("incidentAddress", inc == null ? null : inc.getAddress());
        m.put("initiatorName", userRepo.findById(r.getInitiatorId()).map(User::getName).orElse("未知"));
        if (r.getConfirmedOwnerId() != null) {
            m.put("confirmedOwnerOrg",
                    userRepo.findById(r.getConfirmedOwnerId()).map(User::getOrgName).orElse("未知"));
        }
        if (r.getForcedBy() != null) {
            m.put("forcedByName", userRepo.findById(r.getForcedBy()).map(User::getName).orElse("未知"));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (JointReviewSignoff s : signoffRepo.findByReviewIdOrderByIdAsc(r.getId())) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("signoff", s);
            sm.put("ownerOrg", userRepo.findById(s.getOwnerId()).map(User::getOrgName).orElse("未知"));
            sm.put("evidenceList", readList(s.getEvidencePhotos()));
            rows.add(sm);
        }
        m.put("signoffs", rows);
        m.put("costSuggestionList", readObjectList(r.getCostSuggestion()));
        advanceRepo.findByIncidentId(r.getIncidentId()).ifPresent(a -> m.put("advance", a));
        return m;
    }

    /** 市级协调池：复核仍无法确认、已上报市级的事件 */
    public List<Map<String, Object>> pool() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (JointReview r : reviewRepo.findByStatusInOrderByEscalatedAtDesc(
                List.of(com.city.manhole.common.Constants.JointReviewStatus.ESCALATED))) {
            list.add(reviewMap(r));
        }
        return list;
    }

    public List<Map<String, Object>> advances() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (EmergencyFundAdvance a : advanceRepo.findAllByOrderByCreatedAtDesc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("advance", a);
            Incident inc = incidentRepo.findById(a.getIncidentId()).orElse(null);
            m.put("incidentCode", inc == null ? null : inc.getCode());
            m.put("incidentAddress", inc == null ? null : inc.getAddress());
            m.put("approvedByName", userRepo.findById(a.getApprovedBy()).map(User::getName).orElse("未知"));
            if (a.getRecoverFromOwnerId() != null) {
                m.put("recoverFromOrg",
                        userRepo.findById(a.getRecoverFromOwnerId()).map(User::getOrgName).orElse("未知"));
            }
            list.add(m);
        }
        return list;
    }

    /** 根据单位名称推断燃气/通信/电力/排水类型标签 */
    public static String unitTypeOf(String orgName) {
        if (orgName == null) return "其他";
        if (orgName.contains("燃气")) return "燃气";
        if (orgName.contains("通信") || orgName.contains("联通") || orgName.contains("电信") || orgName.contains("移动"))
            return "通信";
        if (orgName.contains("电力") || orgName.contains("供电") || orgName.contains("电网")) return "电力";
        if (orgName.contains("排水") || orgName.contains("污水") || orgName.contains("雨水")
                || orgName.contains("市政")) return "排水";
        return "其他";
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readObjectList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
