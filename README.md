# 城市无主井盖认领巡检维修与赔付协同平台

市民发现井盖缺失、松动、异响、塌陷后在线上报，平台将事件推送给街道值班员先行临时围护（**应急处置线**，只看现场风险与到场时效），再依据井盖标识、地下管线、历史施工、周边商户、城管台账排查权属（**权属追责线**）。权属无法即时确认时，街道先行设置警示锥、临时盖板、夜间反光标识并垫付费用，事件标记**责任待定**；燃气、通信、排水等权属单位互相推诿、均否认归属时，街道可发起**联合现场复核**：平台展示井盖编号、地下管线图、最近一次施工许可与历史维修记录，复核结论须由各单位代表签字（拒签须书面说明理由并上传证据）；复核仍无法确认的，事件进入**市级协调池**，维修费用先由市级应急基金垫付，市级协调人员依据各方意见及拒签证据研判后可强制认领，再按复核结论追偿。联合复核结束后系统生成权属结论和费用分摊建议。权属单位认领后派发维修工单，维修队补充到场时间、井室状况、井盖规格、封路与专业协同信息；维修完成后城管巡查员按照片、定位、路面平整度、车辆碾压测试验收；市民或车辆受损的，赔付专员依据上报时间、临时围护、权属确认、维修时长、事故证明定责赔付；最终费用由城管复核分摊，街道垫付费用按复核结果回补。平台沉淀高发路段、权属争议单位、维修超时、赔付金额、联合复核与市级协调统计，用于调整日常巡检路线与施工移交规则。

## 技术栈

- **后端**：Spring Boot 3.3（Java 17）、Spring Data JPA、PostgreSQL 15、BCrypt 密码加密、内存令牌认证
- **前端**：Vue 3 + Vite + Vue Router + Pinia + Element Plus + ECharts
- **数据库**：PostgreSQL（用户级集群，端口 5433，免密 trust，不依赖系统服务）
- **容器化**：根目录 `docker-compose.yml`（postgres15 + 后端 + 前端 Nginx），一键启动

## 目录结构

```
├── docker-compose.yml  # 一键启动（db + backend + frontend）
├── backend/            # Spring Boot 后端（端口 8080，含 Dockerfile）
│   └── src/main/java/com/city/manhole/
│       ├── config/     # 认证拦截器、Web 配置、演示数据初始化
│       ├── controller/ # REST 接口（含 JointReviewController 联合复核）
│       ├── service/    # 业务与状态机（应急线/权属线/联合复核线分离）
│       ├── model/      # JPA 实体（事件/应急/排查/认领/联合复核/签字/工单/验收/赔付/分摊/通知/路线/规则）
│       └── repo/       # Spring Data 仓库
├── frontend/           # Vue 3 前端（端口 5173，代理 /api、/uploads 到 8080，含 Dockerfile+Nginx）
│   └── src/views/      # 登录/首页/上报/工作台/事件详情/联合复核/赔付/分摊/统计/路线/规则/账号
└── scripts/            # 本地启动脚本（无需 Docker）
```

## 本地启动

### 方式零：Docker Compose 一键启动（推荐，无需本机 JDK/Node/PG）

```bash
docker compose up --build
# 前端(含 API 反代): http://localhost:5173   后端直连: http://localhost:8080
# 首次启动自动建表并写入演示数据；数据与上传文件保存在命名卷 manhole-pgdata / manhole-uploads
```

### 方式一/二：本机脚本启动

前置要求：JDK 17、Maven 3.6+、Node 18+、PostgreSQL 15（已安装即可，脚本自建用户级集群）。

```bash
# 方式一：一键启动（数据库 + 后端 + 前端）
bash scripts/start-all.sh

# 方式二：分步启动
bash scripts/db-start.sh          # 1. 启动 PostgreSQL（端口 5433，自动建库 manhole）
bash scripts/start-backend.sh     # 2. 启动后端  http://localhost:8080
bash scripts/start-frontend.sh    # 3. 启动前端  http://localhost:5173
```

启动后访问 **http://localhost:5173** 。首次启动后端会自动建表并写入演示数据（12 个账号、10 个覆盖各状态的事件、巡检路线与移交规则）。

停止数据库：`bash scripts/db-stop.sh`

> 说明：数据库连接配置在 `backend/src/main/resources/application.yml`（jdbc:postgresql://localhost:5433/manhole，用户 postgres，trust 免密）。如需使用已有 PostgreSQL 实例，修改该文件中的地址/账号即可。

## 测试账号

| 账号 | 密码 | 角色 | 职责 |
|---|---|---|---|
| citizen / citizen2 | citizen123 | 市民 | 上报井盖问题、查看进度 |
| street | street123 | 街道值班员 | 应急派单、临时围护、权属排查、发起联合现场复核 |
| gas / power / telecom / municipal | owner123 | 权属单位（燃气/电力/通信/市政） | 认领井盖、联合复核代表签字/拒签、派发维修工单 |
| drainage | owner123 | 权属单位（城市排水集团） | 联合复核受邀单位（燃气/通信/排水推诿场景） |
| repair1 / repair2 | repair123 | 维修队（市政一队/燃气专业队） | 接单、填写维修信息、完成维修 |
| inspector | inspect123 | 城管巡查员 | 验收（照片/定位/平整度/碾压测试） |
| comp | comp123 | 赔付专员 | 赔付案件审核定责、标记赔付 |
| admin | admin123 | 城管管理员（市级协调人员） | 统计分析、费用复核分摊、市级协调池研判、应急基金垫付与强制认领、路线/规则/账号管理 |

市民也可在登录页自助注册（仅市民角色）。

## 业务流程（两条线）

```
应急处置线：上报 → 街道派单 → 到场 → 临时围护(警示锥/临时盖板/夜间反光标识, 记录费用)
权属追责线：围护后 → 五类证据排查 → 确认权属 / 责任待定(街道垫付) → 权属单位认领
联合复核线：各单位均否认 → 街道发起联合现场复核(井盖编号/地下管线图/施工许可/历史维修)
          → 各单位代表签字 / 拒签(须书面理由+证据)
          → 全部签字：生成权属结论与费用分摊建议(比例合计100%)
          → 仍无法确认：进入市级协调池 → 市级应急基金先行垫付 → 市级研判强制认领(须有拒签材料) → 按结论追偿
维修验收线：权属确认 → 派维修工单 → 接单/维修(井室状况/井盖规格/封路/专业协同) → 验收(4 项检查)
          → 费用复核分摊 → 结案
赔付线：    上报时有损伤自动立案 → 赔付专员定责(五要素分析) → 核定/驳回 → 赔付
```

- 事件主状态：`REPORTED → SECURED → OWNER_CONFIRMED → REPAIRING → REPAIRED → ACCEPTED → CLOSED`
- 应急线状态：`PENDING → DISPATCHED → SECURED`
- 权属线状态：`UNCONFIRMED → INVESTIGATING → CONFIRMED / PENDING → JOINT_REVIEW → CONFIRMED / CITY_POOL`
- 联合复核状态：`IN_PROGRESS（代表签字中）→ CONFIRMED（形成权属结论）/ ESCALATED（进入市级协调池）`；代表签收：`PENDING / SIGNED / REFUSED（理由+证据）`
- 责任待定事件由街道先行处置并垫付，权属单位后续可认领，费用复核时按追责结论分摊（分摊合计必须等于总费用，否则系统拒绝）。
- 维修超时阈值 48 小时；应急到场时限：高危 120 / 中危 240 / 低危 480 分钟（`application.yml` 可调）。

## 验证方式（均已实际执行）

1. **后端编译**：`cd backend && mvn compile` 通过。
2. **前端构建**：`cd frontend && npm run build` 通过（含新增 JointReviews 页与事件详情复核卡片）。
3. **联合现场复核全流程 API 验证**（脚本 32 项断言全部通过）：
   市民上报 → 街道派单/围护 → 排查责任待定 → 发起联合复核（携带井盖编号/管线图/施工许可/维修历史）→
   燃气代表签字、排水拒签（无理由、无证据分别被系统拒绝；理由+证据通过）、通信签字 →
   存在拒签时"确认权属"被系统拒绝 → 仍无法确认进入**市级协调池**（ownerStatus=CITY_POOL）→
   街道动用应急基金被拒、市级登记两笔垫付累计 ¥3600 → 市级依据拒签证据**强制认领**电力（forcedClaim=true）→
   派单/维修/验收 → 费用分摊错误合计被拒、正确合计 ¥3800 复核结案（事件 CLOSED）。
4. **全部签字确认结论分支**（验证通过）：两家单位代表全部签字后形成权属结论，支持 80/20 多方分摊建议，
   比例合计非 100%、指定未参会单位、非市级协调池强制认领均被系统拒绝。
5. **强制认领前置校验**：无任何拒签书面理由与证据时，市级强制认领被系统拒绝（必须先有拒签材料）。
6. **普通认领回归与权限**：未发起复核的事件权属单位主动认领正常；未受邀单位签字/拒签被拒；
   权属单位只能看到被邀请参加的复核单；未登录访问返回 401。
7. **演示数据验证**：事件 MH20260928-0001 为联合复核进行中（燃气/排水待签、通信已拒签带证据）；
   MH20260925-0001 已进入市级协调池（3 家拒签均带证据、应急基金垫付 ¥3600，待市级研判）；
   种子证据图片可直接访问（`/uploads/seed-evidence-*.png`，image/png 200）。
8. **统计接口验证**：`/api/stats/overview` 返回 `jointReviewing`、`cityPool` 指标；权属争议统计纳入联合复核/市级池事件。
9. **前端服务验证**：Vite dev server 启动，`http://localhost:5173` 返回页面 200，`/api` 代理到后端正常。

> 说明：Docker 配置（`docker-compose.yml`、前后端 Dockerfile/Nginx）已提供并校验语法结构；当前验证环境未安装 Docker 守护进程，镜像构建请在具备 Docker 的机器上执行 `docker compose up --build`。

人工界面验证建议路径：
- 用 **street** 打开责任待定事件详情 → 右侧「联合现场复核」选择燃气/通信/排水单位并填写证据包发起；
- 分别用 **gas / telecom / drainage** 登录，在顶部「联合复核」菜单或事件详情中完成代表签字、拒签（上传证据图片）；
- 用 **street** 在事件详情结束复核：全部签字可生成权属结论与分摊建议，否则上报市级协调池；
- 用 **admin（市级协调人员）** 打开市级协调池事件 → 登记应急基金垫付 → 依据拒签理由与证据强制认领；
- 强制认领后由该权属单位派单维修，巡查员验收，管理员费用复核结案。

## 主要接口

| 接口 | 说明 |
|---|---|
| `POST /api/auth/login` `POST /api/auth/register` | 登录 / 市民注册 |
| `POST /api/incidents` `GET /api/incidents` `GET /api/incidents/{id}` | 上报 / 角色范围列表 / 聚合详情 |
| `POST /api/incidents/{id}/emergency/dispatch` `/secure` | 应急派单 / 完成围护 |
| `POST /api/incidents/{id}/investigation` `/claim` | 权属排查 / 主动认领 |
| `GET /api/joint-reviews` | 联合复核工作台列表（街道/市级见全部，单位仅见被邀请的） |
| `POST /api/incidents/{id}/joint-review` | 街道发起联合现场复核（证据包+参会单位） |
| `POST /api/incidents/{id}/joint-review/sign` `/refuse` | 代表签字 / 拒签（理由+证据） |
| `POST /api/incidents/{id}/joint-review/finish` | 结束复核：CONFIRMED 权属结论+分摊建议 / UNRESOLVED 进市级协调池 |
| `POST /api/incidents/{id}/joint-review/fund-advance` `/force-claim` | 市级应急基金垫付 / 依据拒签证据强制认领 |
| `POST /api/incidents/{id}/repair-order` `POST /api/repair-orders/{id}/accept` `/finish` | 派单 / 接单 / 完成维修 |
| `POST /api/incidents/{id}/acceptance` | 验收（四项检查） |
| `GET/PUT/POST /api/compensations...` | 赔付列表 / 补充 / 定责 / 赔付 |
| `GET /api/cost-allocations` `POST /api/cost-allocations/{id}/review` | 费用分摊复核 |
| `GET /api/stats/*` | overview / hotspots / disputes / repair-timeouts / compensation / emergency |
| `GET/POST/DELETE /api/admin/users` `/routes` `/rules` | 账号 / 巡检路线 / 移交规则 |
| `POST /api/files` `GET /uploads/*` | 照片上传与访问 |
