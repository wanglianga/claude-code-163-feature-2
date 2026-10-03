<template>
  <div class="page" v-if="d">
    <!-- 头部状态 -->
    <el-card class="page-card">
      <div class="flex-between">
        <div>
          <span style="font-size: 20px; font-weight: 700">{{ d.incident.code }}</span>
          <el-tag style="margin-left: 10px" :type="INCIDENT_STATUS[d.incident.status]?.type">
            {{ INCIDENT_STATUS[d.incident.status]?.label }}
          </el-tag>
          <el-tag v-if="d.incident.responsibilityPending" type="danger" effect="dark" style="margin-left: 6px">
            责任待定
          </el-tag>
        </div>
        <div>
          <span class="text-muted">应急线：</span>
          <el-tag :type="EMERGENCY_STATUS[d.incident.emergencyStatus]?.type" effect="plain">
            {{ EMERGENCY_STATUS[d.incident.emergencyStatus]?.label }}
          </el-tag>
          <span class="text-muted" style="margin-left: 12px">权属线：</span>
          <el-tag :type="OWNER_STATUS[d.incident.ownerStatus]?.type" effect="plain">
            {{ OWNER_STATUS[d.incident.ownerStatus]?.label }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="15">
        <!-- 上报信息 -->
        <el-card class="page-card">
          <template #header><b>上报信息</b></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="问题类型">{{ d.incident.issueType }}</el-descriptions-item>
            <el-descriptions-item label="危险程度">
              <el-tag :type="dangerType(d.incident.dangerLevel)" size="small">{{ d.incident.dangerLevel }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="详细地址" :span="2">{{ d.incident.address }}</el-descriptions-item>
            <el-descriptions-item label="道路类型">{{ d.incident.roadType }}</el-descriptions-item>
            <el-descriptions-item label="经纬度">
              {{ d.incident.longitude }}, {{ d.incident.latitude }}
            </el-descriptions-item>
            <el-descriptions-item label="上报人">{{ d.reporterName }}</el-descriptions-item>
            <el-descriptions-item label="上报时间">{{ fmtTime(d.incident.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="人车损伤" :span="2">
              <el-tag v-if="d.incident.hasDamage" type="danger" size="small">有损伤</el-tag>
              <span v-else>无</span>
              <span v-if="d.incident.damageDesc" style="margin-left: 8px">{{ d.incident.damageDesc }}</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="d.ownerOrg" label="权属单位" :span="2">
              <el-tag type="success">{{ d.ownerOrg }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div v-if="d.photoList?.length" class="mt16">
            <el-image v-for="(p, i) in d.photoList" :key="i" :src="p" :preview-src-list="d.photoList"
                      fit="cover" style="width: 110px; height: 110px; margin-right: 8px; border-radius: 4px" />
          </div>
        </el-card>

        <!-- 应急处置记录 -->
        <el-card v-if="d.emergency" class="page-card">
          <template #header><b>应急处置记录（应急线）</b></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="处置人">{{ d.emergencyOfficer }}</el-descriptions-item>
            <el-descriptions-item label="围护措施">
              <el-tag v-for="m in (d.emergency.measures || '').split(',')" :key="m" size="small"
                      style="margin-right: 4px">{{ m }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="派单时间">{{ fmtTime(d.emergency.dispatchedAt) }}</el-descriptions-item>
            <el-descriptions-item label="到场时间">{{ fmtTime(d.emergency.arrivedAt) }}</el-descriptions-item>
            <el-descriptions-item label="围护完成">{{ fmtTime(d.emergency.securedAt) }}</el-descriptions-item>
            <el-descriptions-item label="应急费用（街道垫付）">{{ money(d.emergency.cost) }}</el-descriptions-item>
            <el-descriptions-item v-if="d.emergency.note" label="备注" :span="2">{{ d.emergency.note }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 权属排查记录 -->
        <el-card v-if="d.investigations?.length" class="page-card">
          <template #header><b>权属排查记录（权属线）</b></template>
          <div v-for="(item, i) in d.investigations" :key="i" style="margin-bottom: 12px">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="排查人">
                {{ item.investigatorName }} · {{ fmtTime(item.investigation.createdAt) }}
                <el-tag :type="item.investigation.conclusion === 'CONFIRMED' ? 'success' : 'danger'"
                        size="small" style="margin-left: 8px">
                  {{ item.investigation.conclusion === 'CONFIRMED' ? '确认权属' : '无法确认（责任待定）' }}
                </el-tag>
                <el-tag v-if="item.confirmedOwnerOrg" type="success" size="small" style="margin-left: 4px">
                  {{ item.confirmedOwnerOrg }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="井盖标识">{{ item.investigation.manholeMarking || '-' }}</el-descriptions-item>
              <el-descriptions-item label="地下管线">{{ item.investigation.pipelineInfo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="历史施工">{{ item.investigation.constructionHistory || '-' }}</el-descriptions-item>
              <el-descriptions-item label="周边商户">{{ item.investigation.merchantInfo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="城管台账">{{ item.investigation.cityLedger || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>

        <!-- 认领记录 -->
        <el-card v-if="d.claims?.length" class="page-card">
          <template #header><b>权属认领记录</b></template>
          <el-timeline>
            <el-timeline-item v-for="(c, i) in d.claims" :key="i" :timestamp="fmtTime(c.claim.createdAt)" type="success">
              <b>{{ c.ownerOrg }}</b> {{ c.claim.claimType }}
              <span v-if="c.claim.note" class="text-muted">（{{ c.claim.note }}）</span>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 联合现场复核 -->
        <el-card v-if="d.jointReview" class="page-card">
          <template #header>
            <div class="flex-between">
              <b>联合现场复核（权属互相推诿复核）</b>
              <el-tag :type="REVIEW_STATUS[d.jointReview.review.status]?.type" size="small">
                {{ REVIEW_STATUS[d.jointReview.review.status]?.label }}
              </el-tag>
            </div>
          </template>

          <el-alert v-if="d.jointReview.review.status === 'ESCALATED'" type="error" :closable="false" class="mb16"
                    title="复核仍无法确认权属，事件已进入市级协调池；维修费用由市级应急基金先行垫付，后续按协调结论追偿。" />
          <el-alert v-if="d.jointReview.review.forcedClaim" type="warning" :closable="false" class="mb16"
                    :title="`已由市级协调人员${d.jointReview.forcedByName || ''}作出强制认领决定`" />

          <el-descriptions :column="2" border>
            <el-descriptions-item label="发起单位">{{ d.jointReview.initiatorOrg }} · {{ d.jointReview.initiatorName }}</el-descriptions-item>
            <el-descriptions-item label="计划复核时间">{{ fmtTime(d.jointReview.review.scheduledAt) }}</el-descriptions-item>
            <el-descriptions-item label="复核地点" :span="2">{{ d.jointReview.review.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="井盖编号" :span="2">{{ d.jointReview.review.manholeCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="地下管线图" :span="2">
              <div style="white-space: pre-wrap">{{ d.jointReview.review.pipelineMap || '-' }}</div>
              <el-image v-for="(u, i) in pipelineImages" :key="i" :src="u" :preview-src-list="pipelineImages"
                        fit="cover" style="width: 100px; height: 100px; margin: 6px 6px 0 0; border-radius: 4px" />
            </el-descriptions-item>
            <el-descriptions-item label="最近一次施工许可" :span="2">
              <div style="white-space: pre-wrap">{{ d.jointReview.review.latestPermit || '-' }}</div>
            </el-descriptions-item>
            <el-descriptions-item label="历史维修记录" :span="2">
              <div style="white-space: pre-wrap">{{ d.jointReview.review.repairHistory || '-' }}</div>
            </el-descriptions-item>
            <el-descriptions-item v-if="d.jointReview.review.note" label="发起说明" :span="2">
              {{ d.jointReview.review.note }}
            </el-descriptions-item>
          </el-descriptions>

          <!-- 代表签字表 -->
          <div class="mt16"><b>各单位代表签收情况</b>
            <span class="text-muted">（已签字 {{ d.jointReview.signedCount }} / 拒签 {{ d.jointReview.refusedCount }} / 待签 {{ d.jointReview.pendingCount }}）</span>
          </div>
          <el-table :data="d.jointReview.signatures" size="small" class="mt16" border>
            <el-table-column prop="ownerOrg" label="受邀单位" min-width="150" />
            <el-table-column label="签收状态" width="110">
              <template #default="{ row }">
                <el-tag :type="SIGN_STATUS[row.signature.status]?.type" size="small">
                  {{ SIGN_STATUS[row.signature.status]?.label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="代表/理由与证据" min-width="260">
              <template #default="{ row }">
                <div v-if="row.signature.status === 'SIGNED'">
                  代表：{{ row.signature.signerName }} · {{ fmtTime(row.signature.signedAt) }}
                  <el-image v-if="row.signature.signImage" :src="row.signature.signImage"
                            :preview-src-list="[row.signature.signImage]" fit="cover"
                            style="width: 56px; height: 56px; margin-left: 6px; border-radius: 4px; vertical-align: middle" />
                </div>
                <div v-else-if="row.signature.status === 'REFUSED'">
                  <div style="color: #f56c6c">拒签理由：{{ row.signature.refuseReason }}</div>
                  <el-image v-for="(u, i) in row.evidenceList" :key="i" :src="u"
                            :preview-src-list="row.evidenceList" fit="cover"
                            style="width: 56px; height: 56px; margin: 4px 4px 0 0; border-radius: 4px" />
                </div>
                <span v-else class="text-muted">待代表到场签字</span>
              </template>
            </el-table-column>
          </el-table>

          <!-- 复核结论与费用分摊建议 -->
          <template v-if="d.jointReview.review.conclusion">
            <el-divider content-position="left">复核结论与费用分摊建议</el-divider>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="权属结论">
                <el-tag v-if="d.jointReview.review.conclusion === 'CONFIRMED'" type="success" size="small">
                  确认权属：{{ d.jointReview.confirmedOwnerOrg }}
                  <span v-if="d.jointReview.review.forcedClaim">（市级强制认领）</span>
                </el-tag>
                <el-tag v-else type="danger" size="small">仍无法确认，已上报市级协调池</el-tag>
                <span class="text-muted" style="margin-left: 8px">{{ fmtTime(d.jointReview.review.finishedAt) }}</span>
              </el-descriptions-item>
              <el-descriptions-item v-if="d.jointReview.review.conclusionNote" label="结论说明">
                {{ d.jointReview.review.conclusionNote }}
              </el-descriptions-item>
              <el-descriptions-item v-if="d.jointReview.review.forcedClaim" label="强制认领说明">
                {{ d.jointReview.review.forcedNote || '市级协调人员依据各方现场复核意见及拒签证据研判决定。' }}
              </el-descriptions-item>
              <el-descriptions-item label="费用分摊建议">
                <el-table :data="d.jointReview.costSuggestionList" size="small" border>
                  <el-table-column prop="party" label="承担方" min-width="200" />
                  <el-table-column label="比例" width="90">
                    <template #default="{ row }">{{ row.ratio }}%</template>
                  </el-table-column>
                  <el-table-column prop="reason" label="依据" min-width="240" />
                </el-table>
              </el-descriptions-item>
            </el-descriptions>
          </template>

          <!-- 应急基金垫付 -->
          <div v-if="d.jointReview.review.fundAdvanceAmount" class="mt16">
            <el-tag type="danger" effect="dark">市级应急基金已垫付 {{ money(d.jointReview.review.fundAdvanceAmount) }}</el-tag>
            <span class="text-muted" style="margin-left: 8px">{{ fmtTime(d.jointReview.review.fundAdvanceAt) }}</span>
            <div v-if="d.jointReview.review.fundAdvanceNote" class="text-muted">{{ d.jointReview.review.fundAdvanceNote }}</div>
          </div>
        </el-card>

        <!-- 维修工单 -->
        <el-card v-if="d.repairOrder" class="page-card">
          <template #header><b>维修工单</b></template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="维修队">{{ d.repairTeamName }}</el-descriptions-item>
            <el-descriptions-item label="工单状态">
              <el-tag :type="REPAIR_STATUS[d.repairOrder.status]?.type" size="small">
                {{ REPAIR_STATUS[d.repairOrder.status]?.label }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="派单时间">{{ fmtTime(d.repairOrder.assignedAt) }}</el-descriptions-item>
            <el-descriptions-item label="到场时间">{{ fmtTime(d.repairOrder.arrivedAt) }}</el-descriptions-item>
            <el-descriptions-item label="井室状况">{{ d.repairOrder.chamberCondition || '-' }}</el-descriptions-item>
            <el-descriptions-item label="井盖规格">{{ d.repairOrder.coverSpec || '-' }}</el-descriptions-item>
            <el-descriptions-item label="需要封路">{{ d.repairOrder.needRoadClosure ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="专业协同">
              {{ d.repairOrder.needCoord ? `涉及：${d.repairOrder.coordUnits}` : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="维修费用">{{ money(d.repairOrder.repairCost) }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ fmtTime(d.repairOrder.finishedAt) }}</el-descriptions-item>
            <el-descriptions-item v-if="d.repairOrder.repairNote" label="维修说明" :span="2">
              {{ d.repairOrder.repairNote }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 验收记录 -->
        <el-card v-if="d.acceptances?.length" class="page-card">
          <template #header><b>验收记录</b></template>
          <div v-for="(a, i) in d.acceptances" :key="i" style="margin-bottom: 10px">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="巡查员">{{ a.inspectorName }}</el-descriptions-item>
              <el-descriptions-item label="结论">
                <el-tag :type="a.acceptance.result === 'PASSED' ? 'success' : 'danger'" size="small">
                  {{ a.acceptance.result === 'PASSED' ? '验收通过' : '验收不通过' }}
                </el-tag>
                <span class="text-muted" style="margin-left: 8px">{{ fmtTime(a.acceptance.inspectedAt) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="验收项" :span="2">
                <el-tag :type="a.acceptance.photoOk ? 'success' : 'danger'" size="small" effect="plain">照片比对</el-tag>
                <el-tag :type="a.acceptance.locationOk ? 'success' : 'danger'" size="small" effect="plain" style="margin-left: 4px">定位一致</el-tag>
                <el-tag :type="a.acceptance.flatnessOk ? 'success' : 'danger'" size="small" effect="plain" style="margin-left: 4px">路面平整度</el-tag>
                <el-tag :type="a.acceptance.rollingTestOk ? 'success' : 'danger'" size="small" effect="plain" style="margin-left: 4px">车辆碾压测试</el-tag>
              </el-descriptions-item>
              <el-descriptions-item v-if="a.acceptance.note" label="备注" :span="2">{{ a.acceptance.note }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>

        <!-- 赔付案件 -->
        <el-card v-if="d.compensation" class="page-card">
          <template #header>
            <div class="flex-between">
              <b>赔付案件</b>
              <el-tag :type="COMP_STATUS[d.compensation.status]?.type" size="small">
                {{ COMP_STATUS[d.compensation.status]?.label }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="索赔人">{{ d.compensation.claimantName }}</el-descriptions-item>
            <el-descriptions-item label="损失类型">{{ d.compensation.damageType }}</el-descriptions-item>
            <el-descriptions-item label="索赔金额">{{ money(d.compensation.claimAmount) }}</el-descriptions-item>
            <el-descriptions-item label="核定金额">{{ money(d.compensation.approvedAmount) }}</el-descriptions-item>
            <el-descriptions-item label="事故证明" :span="2">{{ d.compensation.accidentProof || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="d.compensation.analysis" label="责任分析" :span="2">
              {{ d.compensation.analysis }}
            </el-descriptions-item>
            <el-descriptions-item v-if="d.compensation.responsibility" label="责任认定" :span="2">
              {{ RESPONSIBILITY[d.compensation.responsibility] }}
              <span v-if="d.compensation.responsibility === 'SHARED'" class="text-muted">
                （权属 {{ d.compensation.ownerRatio }}% / 街道 {{ d.compensation.streetRatio }}%）
              </span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 费用分摊 -->
        <el-card v-if="d.costAllocation" class="page-card">
          <template #header>
            <div class="flex-between">
              <b>费用分摊</b>
              <el-tag :type="COST_STATUS[d.costAllocation.status]?.type" size="small">
                {{ COST_STATUS[d.costAllocation.status]?.label }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="4" border>
            <el-descriptions-item label="应急费">{{ money(d.costAllocation.emergencyCost) }}</el-descriptions-item>
            <el-descriptions-item label="维修费">{{ money(d.costAllocation.repairCost) }}</el-descriptions-item>
            <el-descriptions-item label="赔付金">{{ money(d.costAllocation.compensationAmount) }}</el-descriptions-item>
            <el-descriptions-item label="合计"><b>{{ money(d.costAllocation.totalAmount) }}</b></el-descriptions-item>
          </el-descriptions>
          <el-table :data="allocList" size="small" class="mt16">
            <el-table-column prop="party" label="分摊方" min-width="180" />
            <el-table-column label="金额" width="130">
              <template #default="{ row }">{{ money(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="依据" min-width="220" />
          </el-table>
          <div v-if="d.costAllocation.reviewNote" class="text-muted mt16">
            复核意见：{{ d.costAllocation.reviewNote }}
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：操作面板 + 时间线 -->
      <el-col :xs="24" :md="9">
        <el-card v-if="actions.length" class="page-card">
          <template #header><b>可执行操作</b></template>

          <!-- 街道：应急派单 -->
          <div v-if="actions.includes('dispatch')" class="mb16">
            <el-alert type="error" :closable="false" title="事件待应急处置，请立即派单到场围护" class="mb16" />
            <el-button type="danger" style="width: 100%" :loading="acting" @click="doDispatch">
              立即派单（街道值班员）
            </el-button>
          </div>

          <!-- 街道：完成围护 -->
          <div v-if="actions.includes('secure')">
            <el-divider content-position="left">完成临时围护</el-divider>
            <el-form label-width="90px">
              <el-form-item label="围护措施">
                <el-checkbox-group v-model="secureForm.measures">
                  <el-checkbox v-for="m in meta.measureTypes || []" :key="m" :value="m">{{ m }}</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="应急费用">
                <el-input-number v-model="secureForm.cost" :min="0" :precision="2" />
                <span class="text-muted" style="margin-left: 6px">元（街道先行垫付）</span>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="secureForm.note" type="textarea" :rows="2" />
              </el-form-item>
              <el-button type="warning" style="width: 100%" :loading="acting" @click="doSecure">
                确认到场并完成围护
              </el-button>
            </el-form>
          </div>

          <!-- 街道：权属排查 -->
          <div v-if="actions.includes('investigate')">
            <el-divider content-position="left">权属排查（五类证据）</el-divider>
            <el-form label-width="90px">
              <el-form-item label="井盖标识">
                <el-input v-model="invForm.manholeMarking" placeholder="铸字、编号、权属标记" />
              </el-form-item>
              <el-form-item label="地下管线">
                <el-input v-model="invForm.pipelineInfo" placeholder="管线资料比对" />
              </el-form-item>
              <el-form-item label="历史施工">
                <el-input v-model="invForm.constructionHistory" placeholder="施工记录查询" />
              </el-form-item>
              <el-form-item label="周边商户">
                <el-input v-model="invForm.merchantInfo" placeholder="走访情况" />
              </el-form-item>
              <el-form-item label="城管台账">
                <el-input v-model="invForm.cityLedger" placeholder="台账登记信息" />
              </el-form-item>
              <el-form-item label="排查结论">
                <el-radio-group v-model="invForm.conclusion">
                  <el-radio value="CONFIRMED">确认权属</el-radio>
                  <el-radio value="PENDING">无法确认（责任待定）</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="invForm.conclusion === 'CONFIRMED'" label="权属单位">
                <el-select v-model="invForm.confirmedOwnerId" style="width: 100%">
                  <el-option v-for="o in meta.ownerUnits || []" :key="o.id" :value="o.id" :label="o.orgName" />
                </el-select>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="invForm.note" type="textarea" :rows="2" />
              </el-form-item>
              <el-button type="primary" style="width: 100%" :loading="acting" @click="doInvestigate">
                提交排查结论
              </el-button>
            </el-form>
          </div>

          <!-- 权属单位：认领 -->
          <div v-if="actions.includes('claim')">
            <el-alert type="warning" :closable="false" title="经核实该井盖属我单位权属的，请主动认领" class="mb16" />
            <el-input v-model="claimNote" placeholder="认领说明（可选）" class="mb16" />
            <el-button type="primary" style="width: 100%" :loading="acting" @click="doClaim">
              主动认领该井盖
            </el-button>
          </div>

          <!-- 街道：发起联合现场复核 -->
          <div v-if="actions.includes('initiateReview')">
            <el-divider content-position="left">联合现场复核（多单位推诿）</el-divider>
            <el-alert type="error" :closable="false" class="mb16"
                      title="燃气、通信、排水等单位均否认归属时，可发起联合现场复核；平台将汇总井盖编号、地下管线图、施工许可与维修记录。" />
            <el-button type="danger" style="width: 100%" @click="openInitiate">
              发起联合现场复核
            </el-button>
          </div>

          <!-- 街道/市级：复核进行中操作 -->
          <div v-if="actions.includes('finishReview')">
            <el-divider content-position="left">联合复核结论</el-divider>
            <el-alert type="warning" :closable="false" class="mb16"
                      :title="`签字进展：已签字 ${d.jointReview.signedCount} / 拒签 ${d.jointReview.refusedCount} / 待签 ${d.jointReview.pendingCount}；全部签字后方可确认权属。`" />
            <el-button type="primary" class="mb16" style="width: 100%" @click="openFinish('CONFIRMED')">
              生成权属结论与费用分摊建议（全部已签字）
            </el-button>
            <el-button type="danger" plain style="width: 100%" @click="openFinish('UNRESOLVED')">
              仍无法确认，上报市级协调池
            </el-button>
          </div>

          <!-- 权属单位代表：签字 / 拒签 -->
          <div v-if="actions.includes('reviewSign')">
            <el-divider content-position="left">联合现场复核签收</el-divider>
            <el-alert type="warning" :closable="false" class="mb16"
                      title="请派代表参加现场复核；认可结论请代表签字，拒绝签收须说明理由并上传证据。" />
            <el-button type="primary" class="mb16" style="width: 100%" @click="openSign">
              代表签字
            </el-button>
            <el-button type="danger" plain style="width: 100%" @click="openRefuse">
              拒绝签收（填写理由并上传证据）
            </el-button>
          </div>

          <!-- 市级协调：应急基金垫付 / 强制认领 -->
          <template v-if="d.jointReview?.review.status === 'ESCALATED' && role === 'ADMIN'">
            <div>
              <el-divider content-position="left">市级应急基金垫付（可多次登记，自动累计）</el-divider>
              <el-button type="warning" style="width: 100%" @click="openFund">
                登记应急基金垫付
              </el-button>
            </div>
            <div>
              <el-divider content-position="left">市级强制认领</el-divider>
              <el-alert type="error" :closable="false" class="mb16"
                        title="须有拒签单位提交的书面理由与证据，经研判后才能作出强制认领决定。" />
              <el-button type="danger" style="width: 100%"
                         :disabled="!d.jointReview.refusedCount" @click="openForce">
                研判并强制认领
              </el-button>
            </div>
          </template>

          <!-- 权属单位：派单 -->
          <div v-if="actions.includes('assign')">
            <el-divider content-position="left">派发维修工单</el-divider>
            <el-select v-model="assignTeamId" placeholder="选择维修队" style="width: 100%" class="mb16">
              <el-option v-for="t in meta.repairTeams || []" :key="t.id" :value="t.id" :label="t.orgName" />
            </el-select>
            <el-button type="primary" style="width: 100%" :loading="acting" @click="doAssign">
              派发工单
            </el-button>
          </div>

          <!-- 维修队：接单 -->
          <div v-if="actions.includes('accept')">
            <el-button type="primary" style="width: 100%" :loading="acting" @click="doAccept">
              接单（维修队）
            </el-button>
          </div>

          <!-- 巡查员：验收 -->
          <div v-if="actions.includes('inspect')">
            <el-divider content-position="left">验收（四项检查）</el-divider>
            <el-form label-width="110px">
              <el-form-item label="照片比对">
                <el-switch v-model="accForm.photoOk" active-text="一致" />
              </el-form-item>
              <el-form-item label="定位一致">
                <el-switch v-model="accForm.locationOk" active-text="一致" />
              </el-form-item>
              <el-form-item label="路面平整度">
                <el-switch v-model="accForm.flatnessOk" active-text="合格" />
              </el-form-item>
              <el-form-item label="车辆碾压测试">
                <el-switch v-model="accForm.rollingTestOk" active-text="通过" />
              </el-form-item>
              <el-form-item label="验收结论">
                <el-radio-group v-model="accForm.result">
                  <el-radio value="PASSED">通过</el-radio>
                  <el-radio value="FAILED">不通过（退回重修）</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="accForm.note" type="textarea" :rows="2" />
              </el-form-item>
              <el-button type="success" style="width: 100%" :loading="acting" @click="doInspect">
                提交验收结论
              </el-button>
            </el-form>
          </div>

          <!-- 管理员：费用复核 -->
          <div v-if="actions.includes('costReview')">
            <el-divider content-position="left">费用分摊复核</el-divider>
            <div v-for="(a, i) in costForm.allocations" :key="i" style="display: flex; gap: 6px; margin-bottom: 8px">
              <el-input v-model="a.party" placeholder="分摊方" style="flex: 2" />
              <el-input-number v-model="a.amount" :min="0" :precision="2" style="flex: 1" />
              <el-button text type="danger" @click="costForm.allocations.splice(i, 1)">删</el-button>
            </div>
            <el-button size="small" @click="costForm.allocations.push({ party: '', amount: 0, reason: '' })">
              + 添加分摊方
            </el-button>
            <el-input v-model="costForm.reviewNote" placeholder="复核意见" type="textarea" :rows="2"
                      class="mt16 mb16" />
            <el-button type="primary" style="width: 100%" :loading="acting" @click="doCostReview">
              确认分摊并结案（合计须等于 {{ money(d.costAllocation?.totalAmount) }}）
            </el-button>
          </div>
        </el-card>

        <!-- 流程时间线 -->
        <el-card class="page-card">
          <template #header><b>全流程时间线</b></template>
          <el-timeline>
            <el-timeline-item v-for="(t, i) in timeline" :key="i" :timestamp="t.time"
                              :type="t.type" :hollow="!t.done">
              {{ t.label }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <!-- 发起联合现场复核 -->
    <el-dialog v-model="initiateVisible" title="发起联合现场复核" width="640px">
      <el-form label-width="130px">
        <el-form-item label="参会权属单位" required>
          <el-select v-model="initiateForm.ownerIds" multiple placeholder="选择否认归属/需到场的单位" style="width: 100%">
            <el-option v-for="o in meta.ownerUnits || []" :key="o.id" :value="o.id" :label="o.orgName" />
          </el-select>
        </el-form-item>
        <el-form-item label="井盖编号">
          <el-input v-model="initiateForm.manholeCode" placeholder="井圈铸字编号/设施编号" />
        </el-form-item>
        <el-form-item label="地下管线图">
          <el-input v-model="initiateForm.pipelineMap" type="textarea" :rows="2"
                    placeholder="管线图号/管线走向说明，可附图" />
          <el-upload :show-file-list="false" :http-request="(opt) => uploadTo(opt, initiateForm.pipelineImages)"
                     accept="image/*">
            <el-button size="small">上传管线图</el-button>
          </el-upload>
          <el-image v-for="(u, i) in initiateForm.pipelineImages" :key="i" :src="u"
                    :preview-src-list="initiateForm.pipelineImages" fit="cover"
                    style="width: 64px; height: 64px; margin: 6px 6px 0 0; border-radius: 4px" />
        </el-form-item>
        <el-form-item label="最近施工许可">
          <el-input v-model="initiateForm.latestPermit" type="textarea" :rows="2"
                    placeholder="许可编号、施工单位、许可时间" />
        </el-form-item>
        <el-form-item label="历史维修记录">
          <el-input v-model="initiateForm.repairHistory" type="textarea" :rows="2"
                    placeholder="历史维修工单/承办单位" />
        </el-form-item>
        <el-form-item label="计划复核时间">
          <el-date-picker v-model="initiateForm.scheduledAt" type="datetime" value-format="YYYY-MM-DD HH:mm"
                          placeholder="选择现场复核时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="复核地点">
          <el-input v-model="initiateForm.location" :placeholder="inc.value.address" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="initiateForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="initiateVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="doInitiate">发起复核</el-button>
      </template>
    </el-dialog>

    <!-- 代表签字 -->
    <el-dialog v-model="signVisible" title="单位代表签字" width="480px">
      <el-form label-width="100px">
        <el-form-item label="代表姓名" required>
          <el-input v-model="signForm.signerName" placeholder="现场代表姓名" />
        </el-form-item>
        <el-form-item label="签字照片">
          <el-upload :show-file-list="false" :http-request="(opt) => uploadTo(opt, signImages)" accept="image/*">
            <el-button size="small">上传签字/现场照片（可选）</el-button>
          </el-upload>
          <el-image v-if="signForm.signImage" :src="signForm.signImage" :preview-src-list="[signForm.signImage]"
                    fit="cover" style="width: 90px; height: 90px; margin-top: 6px; border-radius: 4px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="signForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="signVisible = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doSign">确认签字</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝签收 -->
    <el-dialog v-model="refuseVisible" title="拒绝签收（须说明理由并上传证据）" width="520px">
      <el-alert type="error" :closable="false" class="mb16"
                title="拒签材料将提交市级协调人员，作为是否强制认领的研判依据。" />
      <el-form label-width="100px">
        <el-form-item label="拒签理由" required>
          <el-input v-model="refuseForm.reason" type="textarea" :rows="3"
                    placeholder="请书面说明不属于本单位的理由" />
        </el-form-item>
        <el-form-item label="证据材料" required>
          <el-upload :show-file-list="false" :http-request="(opt) => uploadTo(opt, refuseImages)" accept="image/*">
            <el-button size="small">上传图纸/现场照片等证据</el-button>
          </el-upload>
          <el-image v-for="(u, i) in refuseImages" :key="i" :src="u" :preview-src-list="refuseImages"
                    fit="cover" style="width: 64px; height: 64px; margin: 6px 6px 0 0; border-radius: 4px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="refuseForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refuseVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="doRefuse">提交拒签</el-button>
      </template>
    </el-dialog>

    <!-- 结束复核：生成结论 -->
    <el-dialog v-model="finishVisible" :title="finishForm.conclusion === 'CONFIRMED' ? '生成权属结论' : '上报市级协调池'"
               width="560px">
      <el-form label-width="120px">
        <template v-if="finishForm.conclusion === 'CONFIRMED'">
          <el-alert type="success" :closable="false" class="mb16" title="各单位代表均已签字，形成确认权属结论。" />
          <el-form-item label="权属单位" required>
            <el-select v-model="finishForm.confirmedOwnerId" style="width: 100%"
                       placeholder="仅限参加现场复核的单位">
              <el-option v-for="s in attendedOwners" :key="s.ownerId" :value="s.ownerId" :label="s.ownerOrg" />
            </el-select>
          </el-form-item>
          <el-form-item label="费用分摊建议">
            <el-alert type="info" :closable="false" class="mb16"
                      title="默认由确认权属单位承担 100%；多方分摊可调整比例（合计须 100%）。" />
            <div v-for="(a, i) in finishForm.costSuggestion" :key="i">
              <div style="display: flex; gap: 6px; margin-bottom: 6px; align-items: center">
                <el-input v-model="a.party" placeholder="承担方" style="flex: 2" />
                <el-input-number v-model="a.ratio" :min="0" :max="100" style="flex: 1" />
                <span>%</span>
                <el-button text type="danger" @click="finishForm.costSuggestion.splice(i, 1)">删</el-button>
              </div>
              <el-input v-model="a.reason" placeholder="分摊依据" class="mb16" />
            </div>
            <el-button size="small" @click="finishForm.costSuggestion.push({ party: '', ratio: 0, reason: '' })">
              + 添加承担方
            </el-button>
          </el-form-item>
        </template>
        <template v-else>
          <el-alert type="error" :closable="false" class="mb16"
                    title="复核仍无法确认权属，事件将进入市级协调池，维修费用先由市级应急基金垫付，后续追偿。" />
        </template>
        <el-form-item label="结论说明">
          <el-input v-model="finishForm.conclusionNote" type="textarea" :rows="3"
                    placeholder="现场核验情况、各单位意见、结论依据" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="finishVisible = false">取消</el-button>
        <el-button :type="finishForm.conclusion === 'CONFIRMED' ? 'primary' : 'danger'"
                   :loading="acting" @click="doFinish">提交</el-button>
      </template>
    </el-dialog>

    <!-- 应急基金垫付 -->
    <el-dialog v-model="fundVisible" title="市级应急基金垫付" width="460px">
      <el-form label-width="100px">
        <el-form-item label="垫付金额" required>
          <el-input-number v-model="fundForm.amount" :min="0.01" :precision="2" />
          <span class="text-muted" style="margin-left: 6px">元</span>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="fundForm.note" type="textarea" :rows="3" placeholder="垫付项目明细，待责任认定后追偿" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fundVisible = false">取消</el-button>
        <el-button type="warning" :loading="acting" @click="doFund">确认垫付</el-button>
      </template>
    </el-dialog>

    <!-- 市级强制认领 -->
    <el-dialog v-model="forceVisible" title="市级强制认领研判" width="500px">
      <el-alert type="error" :closable="false" class="mb16"
                title="依据各单位现场复核意见及拒签理由、证据研判后，指定责任权属单位；应急基金垫付款按结论向其追偿。" />
      <el-form label-width="100px">
        <el-form-item label="责任单位" required>
          <el-select v-model="forceForm.ownerId" style="width: 100%">
            <el-option v-for="o in meta.ownerUnits || []" :key="o.id" :value="o.id" :label="o.orgName" />
          </el-select>
        </el-form-item>
        <el-form-item label="研判说明">
          <el-input v-model="forceForm.note" type="textarea" :rows="3" placeholder="采信/排除各方证据的理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forceVisible = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="doForce">作出强制认领决定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../store/auth'
import {
  apiIncidentDetail, apiMeta, apiDispatch, apiSecure, apiInvestigate, apiClaim,
  apiCreateOrder, apiAcceptOrder, apiAcceptance, apiCostReview, apiUpload,
  apiReviewInitiate, apiReviewSign, apiReviewRefuse, apiReviewFinish,
  apiFundAdvance, apiForceClaim
} from '../api'
import {
  INCIDENT_STATUS, EMERGENCY_STATUS, OWNER_STATUS, REPAIR_STATUS, COMP_STATUS,
  COST_STATUS, REVIEW_STATUS, SIGN_STATUS, RESPONSIBILITY, dangerType, fmtTime, money
} from '../utils/format'

const route = useRoute()
const auth = useAuthStore()
const id = route.params.id
const d = ref(null)
const meta = ref({})
const acting = ref(false)
const claimNote = ref('')
const assignTeamId = ref(null)

const secureForm = reactive({ measures: ['警示锥'], cost: 0, note: '' })
const invForm = reactive({
  manholeMarking: '', pipelineInfo: '', constructionHistory: '',
  merchantInfo: '', cityLedger: '', conclusion: 'CONFIRMED', confirmedOwnerId: null, note: ''
})
const accForm = reactive({
  photoOk: true, locationOk: true, flatnessOk: true, rollingTestOk: true, result: 'PASSED', note: ''
})
const costForm = reactive({ allocations: [], reviewNote: '' })

// ---------- 联合现场复核 ----------
const initiateVisible = ref(false)
const signVisible = ref(false)
const refuseVisible = ref(false)
const finishVisible = ref(false)
const fundVisible = ref(false)
const forceVisible = ref(false)

const initiateForm = reactive({
  ownerIds: [], manholeCode: '', pipelineMap: '', pipelineImages: [],
  latestPermit: '', repairHistory: '', scheduledAt: '', location: '', note: ''
})
const signForm = reactive({ signerName: '', signImage: '', note: '' })
const signImages = ref([])
const refuseForm = reactive({ reason: '', note: '' })
const refuseImages = ref([])
const finishForm = reactive({
  conclusion: 'CONFIRMED', confirmedOwnerId: null,
  costSuggestion: [], conclusionNote: ''
})
const fundForm = reactive({ amount: null, note: '' })
const forceForm = reactive({ ownerId: null, note: '' })

const jr = computed(() => d.value?.jointReview)
const attendedOwners = computed(() =>
  (jr.value?.signatures || []).map(s => ({ ownerId: s.signature.ownerId, ownerOrg: s.ownerOrg })))

const pipelineImages = computed(() => {
  const text = jr.value?.review?.pipelineMap || ''
  const urls = text.match(/\/uploads\/[\w.-]+/g)
  return urls ? [...new Set(urls)] : []
})

async function uploadTo({ file }, target) {
  const url = await apiUpload(file)
  if (Array.isArray(target)) target.push(url)
  else if (typeof target === 'function') target(url)
}

function openInitiate() {
  Object.assign(initiateForm, {
    ownerIds: (meta.value.ownerUnits || []).map(o => o.id),
    manholeCode: '', pipelineMap: '', pipelineImages: [],
    latestPermit: '', repairHistory: '', scheduledAt: '', location: inc.value.address || '', note: ''
  })
  initiateVisible.value = true
}

function openSign() {
  Object.assign(signForm, { signerName: auth.user?.name || '', signImage: '', note: '' })
  signImages.value = []
  signVisible.value = true
}

function openRefuse() {
  Object.assign(refuseForm, { reason: '', note: '' })
  refuseImages.value = []
  refuseVisible.value = true
}

function openFinish(conclusion) {
  finishForm.conclusion = conclusion
  finishForm.confirmedOwnerId = null
  finishForm.conclusionNote = ''
  finishForm.costSuggestion = conclusion === 'CONFIRMED'
    ? [{ party: '', ratio: 100, reason: '联合现场复核确认权属，各单位代表已签字' }]
    : []
  finishVisible.value = true
}

function openFund() {
  Object.assign(fundForm, { amount: null, note: '' })
  fundVisible.value = true
}

function openForce() {
  Object.assign(forceForm, { ownerId: null, note: '' })
  forceVisible.value = true
}

const doInitiate = () => {
  if (!initiateForm.ownerIds.length) return ElMessage.warning('请选择参会权属单位')
  const mapText = [initiateForm.pipelineMap, ...initiateForm.pipelineImages].filter(Boolean).join('\n')
  run(() => apiReviewInitiate(id, { ...initiateForm, pipelineMap: mapText }), '联合现场复核已发起，已通知各单位派代表参加')
    .then(() => { initiateVisible.value = false })
}
const doSign = () => {
  if (!signForm.signerName.trim()) return ElMessage.warning('请填写代表姓名')
  run(() => apiReviewSign(id, { ...signForm, signImage: signImages.value[0] || '' }), '代表已签字')
    .then(() => { signVisible.value = false })
}
const doRefuse = () => {
  if (!refuseForm.reason.trim()) return ElMessage.warning('请填写拒签理由')
  if (!refuseImages.value.length) return ElMessage.warning('请上传证据材料')
  run(() => apiReviewRefuse(id, { ...refuseForm, evidence: refuseImages.value }), '拒签理由与证据已提交')
    .then(() => { refuseVisible.value = false })
}
const doFinish = () => {
  if (finishForm.conclusion === 'CONFIRMED') {
    if (!finishForm.confirmedOwnerId) return ElMessage.warning('请选择确认的权属单位')
    const ratioSum = finishForm.costSuggestion.reduce((s, a) => s + Number(a.ratio || 0), 0)
    if (ratioSum !== 100) return ElMessage.warning(`分摊比例合计 ${ratioSum}%，须为 100%`)
    const org = attendedOwners.value.find(o => o.ownerId === finishForm.confirmedOwnerId)?.ownerOrg || ''
    finishForm.costSuggestion.forEach(a => { if (!a.party) a.party = `权属单位-${org}` })
  }
  run(() => apiReviewFinish(id, finishForm),
      finishForm.conclusion === 'CONFIRMED' ? '权属结论与费用分摊建议已生成' : '事件已进入市级协调池')
    .then(() => { finishVisible.value = false })
}
const doFund = () => {
  if (!fundForm.amount || fundForm.amount <= 0) return ElMessage.warning('请填写垫付金额')
  run(() => apiFundAdvance(id, fundForm), '应急基金垫付已登记').then(() => { fundVisible.value = false })
}
const doForce = () => {
  if (!forceForm.ownerId) return ElMessage.warning('请选择责任权属单位')
  return ElMessageBox.confirm('强制认领决定作出后将通知责任单位并据此追偿应急基金垫付款，确认？',
      '市级强制认领', { type: 'warning' })
    .then(() => run(() => apiForceClaim(id, forceForm), '已作出强制认领决定')
      .then(() => { forceVisible.value = false }))
    .catch(() => {})
}

const role = computed(() => auth.role)
const inc = computed(() => d.value?.incident || {})

const actions = computed(() => {
  if (!d.value) return []
  const i = inc.value
  const jr = d.value.jointReview
  const list = []
  if (['STREET_DUTY', 'ADMIN'].includes(role.value)) {
    if (i.emergencyStatus === 'PENDING') list.push('dispatch')
    if (i.emergencyStatus !== 'SECURED') list.push('secure')
    if (['INVESTIGATING', 'PENDING'].includes(i.ownerStatus) && !jr) list.push('investigate')
  }
  if (role.value === 'OWNER_UNIT') {
    // 联合复核中的签收由复核流程承接，不再显示普通认领
    if (i.ownerStatus !== 'CONFIRMED' && !jr) list.push('claim')
    if (i.ownerStatus === 'CONFIRMED' && i.ownerId === auth.user.id && !d.value.repairOrder
        && i.status === 'OWNER_CONFIRMED') list.push('assign')
  }
  // 联合复核相关操作
  if (jr) {
    if (['STREET_DUTY', 'ADMIN'].includes(role.value) && jr.review.status === 'IN_PROGRESS') {
      list.push('finishReview')
    }
    if (role.value === 'OWNER_UNIT' && jr.review.status === 'IN_PROGRESS') {
      const mine = (jr.signatures || []).find(s => s.mine)
      if (mine && mine.signature.status !== 'SIGNED') list.push('reviewSign')
    }
  }
  if (['STREET_DUTY', 'ADMIN'].includes(role.value)
      && i.emergencyStatus === 'SECURED'
      && ['INVESTIGATING', 'PENDING'].includes(i.ownerStatus) && !jr) {
    list.push('initiateReview')
  }
  if (role.value === 'REPAIR_TEAM' && d.value.repairOrder
      && ['ASSIGNED', 'RETURNED'].includes(d.value.repairOrder.status)
      && d.value.repairOrder.teamId === auth.user.id) list.push('accept')
  if (role.value === 'INSPECTOR' && i.status === 'REPAIRED') list.push('inspect')
  if (role.value === 'ADMIN' && d.value.costAllocation?.status === 'PENDING') list.push('costReview')
  return list
})

const allocList = computed(() => {
  try {
    return JSON.parse(d.value?.costAllocation?.allocations || '[]')
  } catch {
    return []
  }
})

const timeline = computed(() => {
  if (!d.value) return []
  const i = inc.value
  const em = d.value.emergency
  const o = d.value.repairOrder
  const t = []
  t.push({ label: `市民上报（${i.issueType} · 危险${i.dangerLevel}）`, time: fmtTime(i.createdAt), done: true, type: 'primary' })
  t.push({ label: '街道派单应急处置', time: em ? fmtTime(em.dispatchedAt) : '待派单', done: !!em, type: em ? 'warning' : 'info' })
  t.push({ label: `到场临时围护${em?.measures ? '（' + em.measures + '）' : ''}`, time: em?.securedAt ? fmtTime(em.securedAt) : '待围护', done: !!em?.securedAt, type: em?.securedAt ? 'warning' : 'info' })
  const inv = d.value.investigations?.[0]
  t.push({ label: inv ? `权属排查：${inv.investigation.conclusion === 'CONFIRMED' ? '确认权属' : '责任待定'}` : '权属排查', time: inv ? fmtTime(inv.investigation.createdAt) : '待排查', done: !!inv, type: inv ? 'primary' : 'info' })
  const cl = d.value.claims?.[0]
  t.push({ label: cl ? `权属确认：${cl.ownerOrg}（${cl.claim.claimType}）` : '权属确认', time: cl ? fmtTime(cl.claim.createdAt) : '待确认', done: !!cl, type: cl ? 'success' : 'info' })
  const jrv = d.value.jointReview?.review
  if (jrv) {
    t.push({ label: '街道发起联合现场复核（多单位推诿）', time: fmtTime(jrv.createdAt), done: true, type: 'warning' })
    if (jrv.status === 'ESCALATED') {
      t.push({ label: '复核无法确认 → 进入市级协调池', time: fmtTime(jrv.escalatedAt), done: true, type: 'danger' })
      if (jrv.fundAdvanceAmount) t.push({ label: `市级应急基金垫付 ${money(jrv.fundAdvanceAmount)}`, time: fmtTime(jrv.fundAdvanceAt), done: true, type: 'danger' })
    } else if (jrv.status === 'CONFIRMED') {
      t.push({ label: jrv.forcedClaim ? '市级强制认领确认权属' : '联合复核确认权属（代表签字齐全）',
        time: fmtTime(jrv.finishedAt), done: true, type: 'success' })
    } else {
      t.push({ label: `复核进行中（签字 ${d.value.jointReview.signedCount}/拒签 ${d.value.jointReview.refusedCount}/待签 ${d.value.jointReview.pendingCount}）`,
        time: '待结论', done: false, type: 'warning' })
    }
  }
  t.push({ label: o ? `派发维修工单（${d.value.repairTeamName}）` : '派发维修工单', time: o ? fmtTime(o.assignedAt) : '待派单', done: !!o, type: o ? 'primary' : 'info' })
  t.push({ label: '维修完成', time: o?.finishedAt ? fmtTime(o.finishedAt) : '待维修', done: !!o?.finishedAt, type: o?.finishedAt ? 'primary' : 'info' })
  const acc = d.value.acceptances?.find(a => a.acceptance.result === 'PASSED')
  t.push({ label: '验收通过', time: acc ? fmtTime(acc.acceptance.inspectedAt) : '待验收', done: !!acc, type: acc ? 'success' : 'info' })
  if (d.value.compensation) {
    t.push({ label: `赔付：${COMP_STATUS[d.value.compensation.status]?.label}`, time: d.value.compensation.paidAt ? fmtTime(d.value.compensation.paidAt) : (d.value.compensation.decidedAt ? fmtTime(d.value.compensation.decidedAt) : '审核中'), done: d.value.compensation.status === 'PAID', type: 'warning' })
  }
  t.push({ label: '费用分摊复核结案', time: i.closedAt ? fmtTime(i.closedAt) : '待复核', done: i.status === 'CLOSED', type: i.status === 'CLOSED' ? 'success' : 'info' })
  return t
})

async function load() {
  d.value = await apiIncidentDetail(id)
  if (d.value.costAllocation?.allocations) {
    costForm.allocations = allocList.value.map(a => ({ ...a }))
  }
}

async function run(fn, successMsg) {
  acting.value = true
  try {
    await fn()
    ElMessage.success(successMsg)
    await load()
  } finally {
    acting.value = false
  }
}

const doDispatch = () => run(() => apiDispatch(id), '已派单，请尽快到场')
const doSecure = () => {
  if (!secureForm.measures.length) return ElMessage.warning('请选择围护措施')
  run(() => apiSecure(id, secureForm), '临时围护已完成')
}
const doInvestigate = () => {
  if (invForm.conclusion === 'CONFIRMED' && !invForm.confirmedOwnerId) {
    return ElMessage.warning('请选择权属单位')
  }
  run(() => apiInvestigate(id, invForm), '排查结论已提交')
}
const doClaim = () => run(() => apiClaim(id, { note: claimNote.value }), '认领成功，请尽快派单维修')
const doAssign = () => {
  if (!assignTeamId.value) return ElMessage.warning('请选择维修队')
  run(() => apiCreateOrder(id, { teamId: assignTeamId.value }), '工单已派发')
}
const doAccept = () => run(() => apiAcceptOrder(d.value.repairOrder.id), '已接单')
const doInspect = () => run(() => apiAcceptance(id, accForm), '验收结论已提交')
const doCostReview = async () => {
  const total = costForm.allocations.reduce((s, a) => s + Number(a.amount || 0), 0)
  if (Math.abs(total - Number(d.value.costAllocation.totalAmount)) > 0.001) {
    return ElMessage.warning(`分摊合计 ${total} 与总费用 ${d.value.costAllocation.totalAmount} 不一致`)
  }
  await ElMessageBox.confirm('分摊复核后事件将结案，确认提交？', '费用复核', { type: 'warning' })
  run(() => apiCostReview(d.value.costAllocation.id, costForm), '费用分摊完成，事件已结案')
}

onMounted(async () => {
  await load()
  meta.value = await apiMeta()
})
</script>
