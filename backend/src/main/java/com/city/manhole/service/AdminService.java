package com.city.manhole.service;

import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.common.Constants;
import com.city.manhole.model.HandoverRule;
import com.city.manhole.model.InspectionRoute;
import com.city.manhole.model.User;
import com.city.manhole.repo.HandoverRuleRepo;
import com.city.manhole.repo.InspectionRouteRepo;
import com.city.manhole.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** 平台管理：账号、巡检路线、施工移交规则 */
@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepo userRepo;
    private final InspectionRouteRepo routeRepo;
    private final HandoverRuleRepo ruleRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private static final Set<String> CREATABLE_ROLES = Set.of(
            Constants.Role.CITIZEN, Constants.Role.STREET_DUTY, Constants.Role.OWNER_UNIT,
            Constants.Role.REPAIR_TEAM, Constants.Role.INSPECTOR, Constants.Role.COMPENSATOR,
            Constants.Role.ADMIN, Constants.Role.CITY_COORDINATOR);

    // ---------- 账号 ----------
    public List<User> users() {
        AuthContext.requireRole(Constants.Role.ADMIN);
        List<User> all = userRepo.findAll();
        all.forEach(u -> u.setPassword(null));
        return all;
    }

    @Transactional
    public User createUser(String username, String password, String name, String role, String orgName, String phone) {
        AuthContext.requireRole(Constants.Role.ADMIN);
        if (username == null || username.isBlank() || password == null || password.length() < 6) {
            throw new BizException("用户名不能为空，密码至少 6 位");
        }
        if (!CREATABLE_ROLES.contains(role)) throw new BizException("角色不合法");
        if (userRepo.findByUsername(username.trim()).isPresent()) throw new BizException("用户名已存在");
        User u = new User();
        u.setUsername(username.trim());
        u.setPassword(encoder.encode(password));
        u.setName(name == null || name.isBlank() ? username.trim() : name.trim());
        u.setRole(role);
        u.setOrgName(orgName);
        u.setPhone(phone);
        return userRepo.save(u);
    }

    // ---------- 巡检路线 ----------
    public List<InspectionRoute> routes() {
        AuthContext.requireLogin();
        return routeRepo.findAll();
    }

    @Transactional
    public InspectionRoute saveRoute(Long id, String name, String sections, String frequency, String note) {
        AuthContext.requireRole(Constants.Role.ADMIN);
        if (name == null || name.isBlank()) throw new BizException("路线名称不能为空");
        InspectionRoute r = id == null ? new InspectionRoute()
                : routeRepo.findById(id).orElseThrow(() -> new BizException("路线不存在"));
        r.setName(name.trim());
        r.setSections(sections);
        r.setFrequency(frequency);
        r.setNote(note);
        r.setUpdatedAt(LocalDateTime.now());
        return routeRepo.save(r);
    }

    @Transactional
    public void deleteRoute(Long id) {
        AuthContext.requireRole(Constants.Role.ADMIN);
        routeRepo.deleteById(id);
    }

    // ---------- 施工移交规则 ----------
    public List<HandoverRule> rules() {
        AuthContext.requireLogin();
        return ruleRepo.findAll();
    }

    @Transactional
    public HandoverRule saveRule(Long id, String title, String content) {
        AuthContext.requireRole(Constants.Role.ADMIN);
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            throw new BizException("规则标题与内容不能为空");
        }
        HandoverRule r = id == null ? new HandoverRule()
                : ruleRepo.findById(id).orElseThrow(() -> new BizException("规则不存在"));
        r.setTitle(title.trim());
        r.setContent(content.trim());
        r.setUpdatedAt(LocalDateTime.now());
        return ruleRepo.save(r);
    }

    @Transactional
    public void deleteRule(Long id) {
        AuthContext.requireRole(Constants.Role.ADMIN);
        ruleRepo.deleteById(id);
    }
}
