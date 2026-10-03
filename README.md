# 城市无主井盖认领巡检维修与赔付协同平台

市民发现井盖缺失、松动、异响、塌陷后在线上报，平台将事件推送给街道值班员先行临时围护（**应急处置线**，只看现场风险与到场时效），再依据井盖标识、地下管线、历史施工、周边商户、城管台账排查权属（**权属追责线**）。权属无法即时确认时，街道先行设置警示锥、临时盖板、夜间反光标识并垫付费用，事件标记**责任待定**；**燃气、通信、排水等单位互相推诿时，平台展示井盖编号、地下管线图、最近一次施工许可与历史维修记录，街道可发起联合现场复核，各单位代表现场核验并签字，拒签必须说明理由并上传证据；仍无法确认的事件进入市级协调池，维修费用先由应急基金垫付，市级协调人员审阅各方意见后可决定强制认领，事后按复核结论追偿。** 权属单位认领后派发维修工单，维修队补充到场时间、井室状况、井盖规格、封路与专业协同信息；维修完成后城管巡查员按照片、定位、路面平整度、车辆碾压测试验收；市民或车辆受损的，赔付专员依据上报时间、临时围护、权属确认、维修时长、事故证明定责赔付；最终费用由城管复核分摊，街道垫付费用按复核结果回补。平台沉淀高发路段、权属争议单位、维修超时、赔付金额统计，用于调整日常巡检路线与施工移交规则。

## 技术栈

- **后端**：Spring Boot 3.3（Java 17）、Spring Data JPA、PostgreSQL 15、BCrypt 密码加密、内存令牌认证
- **前端**：Vue 3 + Vite + Vue Router + Pinia + Element Plus + ECharts
- **数据库**：PostgreSQL（用户级集群，端口 5433，免密 trust，不依赖系统服务）

## 目录结构

```
├── backend/            # Spring Boot 后端（端口 8080）
│   ├── Dockerfile      # 多阶段构建（Maven 构建 + JRE 运行）
│   └── src/main/java/com/city/manhole/
│       ├── config/     # 认证拦截器、Web 配置、演示数据初始化
│       ├── controller/ # REST 接口（含联合复核/市级协调 JointReviewController）
│       ├── service/    # 业务与状态机（应急线/权属线/联合复核/市级协调分离）
│       ├── model/      # JPA 实体（事件/应急/排查/认领/复核/签字/基金垫付/井盖档案/工单/验收/赔付/分摊/通知/路线/规则）
│       └── repo/       # Spring Data 仓库
├── frontend/           # Vue 3 前端（端口 5173，代理 /api、/uploads 到 8080；Docker 用 Nginx 托管于 8081）
│   ├── Dockerfile      # 构建静态资源 + Nginx（nginx.conf 反代后端）
│   └── src/
│       ├── components/ # EvidencePanel（证据包）/ JointReviewPanel（联合复核全流程）
│       └── views/      # 登录/首页/上报/工作台/事件详情/市级协调池/赔付/分摊/统计/路线/规则/账号
├── docker-compose.yml  # 一键启动 db + backend + frontend
└── scripts/            # 本地开发启动脚本
```

## 本地启动

前置要求：JDK 17、Maven 3.6+、Node 18+、PostgreSQL 15（已安装即可，脚本自建用户级集群）。

```bash
# 方式一：一键启动（数据库 + 后端 + 前端）
bash scripts/start-all.sh

# 方式二：分步启动
bash scripts/db-start.sh          # 1. 启动 PostgreSQL（端口 5433，自动建库 manhole）
bash scripts/start-backend.sh     # 2. 启动后端  http://localhost:8080
bash scripts/start-frontend.sh    # 3. 启动前端  http://localhost:5173

# 方式三：Docker Compose 一键启动（需 Docker 20+ 与 Compose v2）
docker compose up --build
# 启动后：前端 http://localhost:8081 ，后端 http://localhost:8080 ，数据库容器 5432
```

启动后访问 **http://localhost:5173** 。首次启动后端会自动建表并写入演示数据（14 个账号、12 个覆盖各状态的事件、井盖档案/施工许可/维修台账、巡检路线与移交规则）。

停止数据库：`bash scripts/db-stop.sh`

> 说明：数据库连接配置在 `backend/src/main/resources/application.yml`（jdbc:postgresql://localhost:5433/manhole，用户 postgres，trust 免密）。如需使用已有 PostgreSQL 实例，修改该文件中的地址/账号即可。

## 测试账号

| 账号 | 密码 | 角色 | 职责 |
|---|---|---|---|
| citizen / citizen2 | citizen123 | 市民 | 上报井盖问题、查看进度 |
| street | street123 | 街道值班员 | 应急派单、临时围护、权属排查、发起联合现场复核 |
| gas / power / telecom / municipal / drainage | owner123 | 权属单位（燃气/电力/通信/市政/排水） | 参会签字或拒签、认领井盖、派发维修工单 |
| repair1 / repair2 | repair123 | 维修队（市政一队/燃气专业队） | 接单、填写维修信息、完成维修 |
| inspector | inspect123 | 城管巡查员 | 验收（照片/定位/平整度/碾压测试） |
| comp | comp123 | 赔付专员 | 赔付案件审核定责、标记赔付 |
| city | city123 | **市级协调人员** | 审阅拒签理由与证据、强制认领、应急基金垫付审批与追偿 |
| admin | admin123 | 城管管理员 | 统计分析、费用复核分摊、路线/规则/账号管理 |

市民也可在登录页自助注册（仅市民角色）。

## 业务流程（两条线）

```
应急处置线：上报 → 街道派单 → 到场 → 临时围护(警示锥/临时盖板/夜间反光标识, 记录费用)
权属追责线：围护后 → 五类证据排查 → 确认权属 / 责任待定(街道垫付)
          → 单位主动认领 / 互相推诿 → 联合现场复核(证据包+代表签字, 拒签须理由+证据)
          → 签字确认权属 / 仍无法确认 → 市级协调池(应急基金垫付 → 市级强制认领 → 追偿)
          → 派维修工单 → 接单/维修(井室状况/井盖规格/封路/专业协同) → 验收(4 项检查)
          → 费用复核分摊 → 结案
赔付线：    上报时有损伤自动立案 → 赔付专员定责(五要素分析) → 核定/驳回 → 赔付
```

- 事件主状态：`REPORTED → SECURED → OWNER_CONFIRMED → REPAIRING → REPAIRED → ACCEPTED → CLOSED`
- 应急线状态：`PENDING → DISPATCHED → SECURED`
- 权属线状态：`UNCONFIRMED → INVESTIGATING → CONFIRMED / PENDING → JOINT_REVIEW → ESCALATED → CONFIRMED`
- 联合复核状态：`INITIATED（待各方签收）→ CONCLUDED（签字确认权属）/ ESCALATED（入市级协调池）→ FORCED（市级强制认领终结）`
- 各单位代表签收：`PENDING → SIGNED（签字）/ REFUSED（拒签，必须填理由并上传证据）`；只有各方均已表态才能结束复核，被认定单位若拒签，街道不能径行确认，必须上报市级协调池。
- 应急基金：`PENDING（垫付待追偿）→ RECOVERED（按复核/强制认领结论追偿回补）`。
- 责任待定事件由街道先行处置并垫付，权属单位后续可认领，费用复核时按追责结论分摊（分摊合计必须等于总费用，否则系统拒绝）。
- 维修超时阈值 48 小时；应急到场时限：高危 120 / 中危 240 / 低危 480 分钟（`application.yml` 可调）。

### 推诿复核演示数据

| 事件 | 位置 | 初始状态 | 用途 |
|---|---|---|---|
| MH20260930-0001（井盖 YH-1187） | 裕华东路与建华大街交口 | 联合复核进行中，5 家单位代表待签字 | 街道组织复核 → 各方签字/拒签 → 出具权属结论 |
| MH20260928-0001（井盖 NH-2046） | 南二环辅路 K3+200 | 市级协调池，3 家签字 2 家拒签，基金已垫付 2700 元 | city 账号审阅证据 → 强制认领 → 追偿回补 |
| MH20260923-0001（井盖 BN-0731） | 滨河路 | 责任待定 | 查看证据包（编号/管线图/施工许可/维修记录）后发起复核 |

## 验证方式（均已实际执行）

1. **后端编译**：`cd backend && mvn compile` 通过。
2. **前端构建**：`cd frontend && npm run build` 通过。
3. **全流程 API 验证**（curl 实际执行）：市民上报(含损伤自动立赔付案) → 街道派单/围护 → 排查确认燃气 → 燃气派单 → 维修队接单/完成 → 巡查员验收通过 → 赔付定责并赔付 → 管理员复核分摊 → 事件 `CLOSED`，费用合计校验一致。
4. **责任待定流程验证**：滨河路事件（责任待定）→ 燃气主动认领 → 维修 → 验收 → 赔付按 8:2 分摊 → 复核结案；分摊金额不等于总费用时接口正确拒绝（实测 45100≠44100 被拒）。
5. **角色权限验证**：8 个角色账号逐一调用各自接口成功；越权访问（市政查看燃气事件、维修队查看他人工单、市民访问统计、街道访问市级协调池）均被拒绝，未登录返回 401。
6. **统计接口验证**：总览/高发路段(按路段归集)/权属争议/维修超时/赔付(按月+按责任)/应急时效均返回正确数据。
7. **文件上传验证**：上传 PNG 返回 `/uploads/xxx.png`，经后端与 Vite 代理访问均 200。
8. **推诿联合复核·签字确认路径**（MH20260930-0001，实测通过）：
   - 证据包正确展示井盖编号 `YH-1187`、地下管线图、最近施工许可 `JG-2025-0342`、历史维修记录；
   - 复核进行中权属单位直接认领被拒；拒签缺理由、缺证据分别被拒；重复签字被拒；
   - 燃气/电力/排水/市政签字、通信带证据拒签后，街道出具权属结论；被认定单位为拒签方时系统拒绝径行确认；确认给签字方燃气后事件权属转为 `CONFIRMED`，生成认领记录与费用分摊建议，燃气成功派发维修工单。
9. **推诿联合复核·市级协调路径**（MH20260928-0001，实测通过）：
   - `/api/city/pool` 正确返回入池事件（3 家签字、2 家拒签，应急基金已垫付 ¥2700，拒签证据图可访问）；
   - 街道、权属单位强制认领均被拒；缺强制理由、非参会单位被拒；非池内事件动用基金、重复垫付、未锁定追偿对象即追偿均被拒；
   - city 账号审阅拒签理由与证据后强制排水管理处认领 → 事件权属确认、垫付单锁定追偿对象、事件移出协调池 → ¥2700 追偿回补基金（重复追偿被拒）→ 排水处成功派单。
10. **全新事件全链路验证**（curl 实测）：上报 → 围护前发起复核被拒 → 围护/责任待定 → 发起复核（重复发起被拒）→ 部分单位表态时结束被拒 → 全部表态后仍无法确认入池 → 市级垫付 ¥1500 → 强制认领 → 追偿回补，全链路状态正确。

> 注：Docker Compose 配置已提供（`docker compose up --build`），本次交付环境无 Docker 守护进程，未在本机实际构建镜像；镜像构建步骤（Maven 多阶段 + Nginx）已在 Dockerfile 中固化。

人工界面验证建议路径：

- 用 **street** 打开「裕华东路」事件，可查看证据包并在《联合现场复核》卡片中组织复核；
- 用 **gas/telecom/drainage** 登录同一事件完成代表签字或拒签（拒签需填写理由并上传图片）；
- 用 **city** 登录，从「市级协调池」进入「南二环辅路」事件，审阅通信/排水的拒签理由与证据图，作出强制认领决定、查看基金垫付并确认追偿回补。
- 管理员在「统计分析」查看图表与治理建议，在「巡检路线」「移交规则」落实调整（规则库已新增"权属推诿联合现场复核与应急基金垫付"规则）。

## 主要接口

| 接口 | 说明 |
|---|---|
| `POST /api/auth/login` `POST /api/auth/register` | 登录 / 市民注册 |
| `POST /api/incidents` `GET /api/incidents` `GET /api/incidents/{id}` | 上报 / 角色范围列表 / 聚合详情 |
| `POST /api/incidents/{id}/emergency/dispatch` `/secure` | 应急派单 / 完成围护 |
| `POST /api/incidents/{id}/investigation` `/claim` | 权属排查 / 主动认领 |
| `POST /api/incidents/{id}/joint-reviews` `GET .../joint-reviews` | 街道发起联合现场复核 / 复核记录与签收情况 |
| `POST /api/joint-reviews/{id}/sign` `/conclude` | 单位代表签字·拒签（理由+证据） / 街道出具复核结论 |
| `GET /api/city/pool` `GET /api/city/advances` | 市级协调池 / 应急基金垫付追偿台账（市级协调人员） |
| `POST /api/joint-reviews/{id}/force-claim` `/advance` | 市级强制认领 / 应急基金垫付审批 |
| `POST /api/emergency-advances/{id}/recover` | 按复核结论追偿回补应急基金 |
| `POST /api/incidents/{id}/repair-order` `POST /api/repair-orders/{id}/accept` `/finish` | 派单 / 接单 / 完成维修 |
| `POST /api/incidents/{id}/acceptance` | 验收（四项检查） |
| `GET/PUT/POST /api/compensations...` | 赔付列表 / 补充 / 定责 / 赔付 |
| `GET /api/cost-allocations` `POST /api/cost-allocations/{id}/review` | 费用分摊复核 |
| `GET /api/stats/*` | overview / hotspots / disputes / repair-timeouts / compensation / emergency |
| `GET/POST/DELETE /api/admin/users` `/routes` `/rules` | 账号 / 巡检路线 / 移交规则 |
| `POST /api/files` `GET /uploads/*` | 照片上传与访问 |
