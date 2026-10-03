package com.city.manhole.common;

/** 平台全部状态与角色常量 */
public final class Constants {
    private Constants() {}

    /** 角色 */
    public static final class Role {
        public static final String CITIZEN = "CITIZEN";           // 市民
        public static final String STREET_DUTY = "STREET_DUTY";   // 街道值班员
        public static final String OWNER_UNIT = "OWNER_UNIT";     // 权属单位
        public static final String REPAIR_TEAM = "REPAIR_TEAM";   // 维修队
        public static final String INSPECTOR = "INSPECTOR";       // 城管巡查员
        public static final String COMPENSATOR = "COMPENSATOR";   // 赔付专员
        public static final String ADMIN = "ADMIN";               // 城管管理员
    }

    /** 事件主流程状态 */
    public static final class IncidentStatus {
        public static final String REPORTED = "REPORTED";               // 已上报，待应急处置
        public static final String SECURED = "SECURED";                 // 已临时围护，待权属确认
        public static final String OWNER_CONFIRMED = "OWNER_CONFIRMED"; // 权属已确认，待派单维修
        public static final String REPAIRING = "REPAIRING";             // 维修中
        public static final String REPAIRED = "REPAIRED";               // 维修完成，待验收
        public static final String ACCEPTED = "ACCEPTED";               // 验收通过，待费用复核
        public static final String CLOSED = "CLOSED";                   // 已结案
    }

    /** 应急处置线状态（只看现场风险与到场时效） */
    public static final class EmergencyStatus {
        public static final String PENDING = "PENDING";       // 待派单
        public static final String DISPATCHED = "DISPATCHED"; // 已派单，待到场
        public static final String SECURED = "SECURED";       // 已完成临时围护
    }

    /** 权属追责线状态 */
    public static final class OwnerStatus {
        public static final String UNCONFIRMED = "UNCONFIRMED"; // 未确认
        public static final String INVESTIGATING = "INVESTIGATING"; // 排查中
        public static final String CONFIRMED = "CONFIRMED";     // 已确认/认领
        public static final String PENDING = "PENDING";         // 拒绝认领（各单位互相推诿），责任待定
        public static final String JOINT_REVIEW = "JOINT_REVIEW"; // 联合现场复核中（街道组织各单位现场核验）
        public static final String CITY_POOL = "CITY_POOL";     // 市级协调池（复核仍无法确认权属）
    }

    /** 联合现场复核状态 */
    public static final class ReviewStatus {
        public static final String IN_PROGRESS = "IN_PROGRESS"; // 已发起，待各单位代表签字
        public static final String ESCALATED = "ESCALATED";     // 复核仍无法确认，已进入市级协调池
        public static final String CONFIRMED = "CONFIRMED";     // 复核确认权属（生成权属结论）
    }

    /** 代表签字状态（每个被邀请单位一条） */
    public static final class SignStatus {
        public static final String PENDING = "PENDING";   // 待签收
        public static final String SIGNED = "SIGNED";     // 已签字认可
        public static final String REFUSED = "REFUSED";   // 拒绝签收（须说明理由并上传证据）
    }

    /** 维修工单状态 */
    public static final class RepairStatus {
        public static final String ASSIGNED = "ASSIGNED";   // 已派发，待接单
        public static final String ACCEPTED = "ACCEPTED";   // 已接单，维修中
        public static final String FINISHED = "FINISHED";   // 维修完成，待验收
        public static final String RETURNED = "RETURNED";   // 验收不通过，退回重修
    }

    /** 赔付案件状态 */
    public static final class CompStatus {
        public static final String FILED = "FILED";         // 已立案，待审核
        public static final String APPROVED = "APPROVED";   // 已核定，待赔付
        public static final String REJECTED = "REJECTED";   // 已驳回
        public static final String PAID = "PAID";           // 已赔付
    }

    /** 费用分摊状态 */
    public static final class CostStatus {
        public static final String PENDING = "PENDING";       // 待复核分摊
        public static final String ALLOCATED = "ALLOCATED";   // 已分摊结案
    }

    /** 赔付责任认定 */
    public static final class Responsibility {
        public static final String OWNER = "OWNER";             // 权属单位全责
        public static final String STREET = "STREET";           // 街道（围护不及时）责任
        public static final String SHARED = "SHARED";           // 多方分摊
        public static final String UNDETERMINED = "UNDETERMINED"; // 无法认定
    }
}
