<template>
  <el-card v-if="reviews.length || canInitiate" class="page-card">
    <template #header>
      <div class="flex-between">
        <b>联合现场复核（权属单位互相推诿）</b>
        <el-button v-if="canInitiate" type="danger" plain size="small" @click="openInitiate">
          发起联合现场复核
        </el-button>
      </div>
    </template>

    <el-empty v-if="!reviews.length" description="尚未发起联合复核" :image-size="60" />

    <div v-for="(item, idx) in reviews" :key="item.review.id"
         :style="idx > 0 ? 'margin-top: 20px' : ''">
      <el-divider v-if="idx > 0" />
      <!-- 复核单头部 -->
      <el-descriptions :column="2" border>
        <el-descriptions-item label="复核状态">
          <el-tag :type="REVIEW_STATUS[item.review.status]?.type" size="small">
            {{ REVIEW_STATUS[item.review.status]?.label }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="计划复核时间">{{ fmtTime(item.review.scheduledAt) }}</el-descriptions-item>
        <el-descriptions-item label="发起方">{{ item.initiatorName }}（街道）</el-descriptions-item>
        <el-descriptions-item label="发起时间">{{ fmtTime(item.review.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="现场地点" :span="2">{{ item.review.siteLocation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发起事由" :span="2">{{ item.review.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="item.review.siteFindings" label="现场核验事实" :span="2">
          {{ item.review.siteFindings }}
        </el-descriptions-item>
        <el-descriptions-item v-if="item.confirmedOwnerOrg" label="权属结论">
          <el-tag type="success" size="small">{{ item.confirmedOwnerOrg }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="item.review.concludedAt" label="结论时间">
          {{ fmtTime(item.review.concludedAt) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="item.review.forceReason" label="强制认领理由" :span="2">
          <el-alert type="warning" :closable="false" :title="forceTitle(item)" />
        </el-descriptions-item>
      </el-descriptions>

      <!-- 各方代表签字情况 -->
      <div class="mt16">
        <b>各单位代表签收情况</b>
        <el-table :data="item.signoffs" size="small" class="mt16">
          <el-table-column label="单位" min-width="160">
            <template #default="{ row }">
              {{ row.ownerOrg }}
              <el-tag size="small" effect="plain" style="margin-left: 4px">{{ row.signoff.unitType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="signoff.representativeName" label="到场代表" width="110" />
          <el-table-column label="签收" width="100">
            <template #default="{ row }">
              <el-tag :type="SIGN_STATUS[row.signoff.status]?.type" size="small">
                {{ SIGN_STATUS[row.signoff.status]?.label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="signoff.opinion" label="现场意见" min-width="180" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <template v-if="row.signoff.status === 'REFUSED'">
                <el-popover placement="left" width="380" trigger="click">
                  <template #reference>
                    <el-button size="small" type="danger" text>拒签理由/证据</el-button>
                  </template>
                  <div><b>拒签理由：</b>{{ row.signoff.refuseReason }}</div>
                  <el-image v-for="(p, i) in row.evidenceList" :key="i" :src="p"
                            :preview-src-list="row.evidenceList" fit="cover"
                            style="width: 150px; height: 100px; margin: 8px 8px 0 0; border-radius: 4px" />
                </el-popover>
              </template>
              <el-button v-else-if="canSign(row)" size="small" type="primary"
                         @click="openSign(row)">签字/拒签</el-button>
              <span v-else class="text-muted" style="font-size: 12px">
                {{ row.signoff.status === 'SIGNED' ? fmtTime(row.signoff.signedAt) : '待代表到场' }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 费用分摊建议 -->
      <div v-if="item.costSuggestionList?.length" class="mt16">
        <b>权属结论与费用分摊建议</b>
        <el-table :data="item.costSuggestionList" size="small" class="mt16" border>
          <el-table-column prop="party" label="承担方" min-width="180" />
          <el-table-column label="建议金额" width="130">
            <template #default="{ row }">{{ money(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="分摊依据" min-width="240" />
        </el-table>
        <div class="text-muted" style="font-size: 12px; margin-top: 4px">
          分摊建议为联合复核参考意见，最终以验收后的费用复核单为准。
        </div>
      </div>

      <!-- 应急基金垫付 -->
      <div v-if="item.advance" class="mt16">
        <el-alert type="error" :closable="false">
          <template #title>
            应急基金已垫付 {{ money(item.advance.amount) }}
            <el-tag size="small" :type="item.advance.status === 'RECOVERED' ? 'success' : 'warning'"
                    style="margin-left: 8px">
              {{ item.advance.status === 'RECOVERED' ? '已追偿回补' : '待追偿' }}
            </el-tag>
          </template>
          <div style="line-height: 1.8">
            <div>用途：{{ item.advance.purpose || '应急维修' }}；批准：{{ item.advance.approvedByName }}</div>
            <div v-if="item.advance.recoverFromOwnerId">
              追偿对象：{{ recoverOrgOf(item) }}；
              <span v-if="item.advance.status === 'RECOVERED'">
                已回补 {{ money(item.advance.recoveredAmount) }}（{{ fmtTime(item.advance.recoveredAt) }}）
              </span>
              <span v-else>垫付资金待按复核结论追偿</span>
            </div>
            <div v-else class="text-muted">复核结论出具后锁定追偿对象</div>
            <el-button v-if="canRecover(item)" size="small" type="success" plain class="mt16"
                       @click="doRecover(item)">确认追偿回补到账</el-button>
          </div>
        </el-alert>
      </div>

      <!-- 操作按钮 -->
      <div class="mt16" style="display: flex; gap: 8px; flex-wrap: wrap">
        <el-button v-if="canConclude(item)" type="primary" @click="openConclude(item)">
          结束复核并出具权属结论
        </el-button>
        <template v-if="canForce(item)">
          <el-button type="danger" @click="openForce(item)">市级强制认领决定</el-button>
          <el-button type="warning" plain @click="openAdvance(item)"
                     :disabled="!!item.advance">应急基金先行垫付</el-button>
        </template>
      </div>
    </div>

    <!-- 发起复核 -->
    <el-dialog v-model="initDlg" title="发起联合现场复核" width="560px">
      <el-form label-width="100px">
        <el-alert type="warning" :closable="false" class="mb16"
                  title="将通知燃气、电力、通信、市政排水等全部权属单位派代表到场，各方须在复核记录上签字；拒签须说明理由并上传证据。" />
        <el-form-item label="复核时间" required>
          <el-date-picker v-model="initForm.scheduledAt" type="datetime" placeholder="选择现场复核时间"
                          format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="现场地点">
          <el-input v-model="initForm.siteLocation" placeholder="默认事件地址，可补充具体井口方位" />
        </el-form-item>
        <el-form-item label="发起事由" required>
          <el-input v-model="initForm.reason" type="textarea" :rows="3"
                    placeholder="如：燃气、通信、排水单位均否认井盖归属，请各单位现场共同核验" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="initDlg = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doInitiate">通知各单位并发起</el-button>
      </template>
    </el-dialog>

    <!-- 单位代表签字/拒签 -->
    <el-dialog v-model="signDlg" title="单位代表现场签收" width="560px">
      <el-form label-width="100px">
        <el-form-item label="代表姓名">
          <el-input v-model="signForm.representativeName" placeholder="到场代表姓名" />
        </el-form-item>
        <el-form-item label="签收意见">
          <el-radio-group v-model="signForm.agree">
            <el-radio :value="true">认可结论，签字</el-radio>
            <el-radio :value="false">拒绝签收</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="现场核验意见">
          <el-input v-model="signForm.opinion" type="textarea" :rows="2"
                    placeholder="开井核验情况说明" />
        </el-form-item>
        <template v-if="signForm.agree === false">
          <el-form-item label="拒签理由" required>
            <el-input v-model="signForm.refuseReason" type="textarea" :rows="3"
                      placeholder="必须说明拒绝签收的理由" />
          </el-form-item>
          <el-form-item label="佐证证据" required>
            <el-upload list-type="picture-card" :file-list="signFileList" :http-request="doSignUpload"
                       :on-remove="onSignRemove" accept="image/*">
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="text-muted" style="font-size: 12px">拒签必须上传现场照片/管线图纸/台账截图等证据</div>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="signDlg = false">取消</el-button>
        <el-button :type="signForm.agree === false ? 'danger' : 'success'" :loading="acting"
                   @click="doSign">确认提交</el-button>
      </template>
    </el-dialog>

    <!-- 街道出具结论 -->
    <el-dialog v-model="concludeDlg" title="结束联合复核并出具结论" width="640px">
      <el-alert type="info" :closable="false" class="mb16"
                title="各方代表均已签字或拒签后才能结束复核；被认定权属的单位必须已签字认可，否则应作“仍无法确认”上报市级协调池。" />
      <el-form label-width="110px">
        <el-form-item label="复核结论" required>
          <el-radio-group v-model="concludeForm.conclusion">
            <el-radio value="CONFIRMED">各方签字确认权属单位</el-radio>
            <el-radio value="DISPUTED">仍无法确认，上报市级协调池（应急基金垫付）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="concludeForm.conclusion === 'CONFIRMED'" label="权属单位" required>
          <el-select v-model="concludeForm.confirmedOwnerId" style="width: 100%">
            <el-option v-for="s in signedParticipants" :key="s.signoff.ownerId"
                       :value="s.signoff.ownerId"
                       :label="`${s.ownerOrg}（代表已签字）`" />
          </el-select>
          <div v-if="!signedParticipants.length" class="text-muted" style="font-size: 12px">
            尚无单位签字认可，不能出具确认结论
          </div>
        </el-form-item>
        <el-form-item label="现场查明事实">
          <el-input v-model="concludeForm.siteFindings" type="textarea" :rows="3"
                    placeholder="开井核验、管线比对、台账核查情况" />
        </el-form-item>
        <el-form-item label="费用分摊建议">
          <div>
            <div v-for="(a, i) in concludeForm.costSuggestion" :key="i"
                 style="display: flex; gap: 6px; margin-bottom: 6px">
              <el-input v-model="a.party" placeholder="承担方" style="flex: 2" />
              <el-input-number v-model="a.amount" :min="0" :precision="2" style="flex: 1" />
              <el-button text type="danger" @click="concludeForm.costSuggestion.splice(i, 1)">删</el-button>
            </div>
            <el-button size="small" @click="concludeForm.costSuggestion.push({ party: '', amount: 0, reason: '' })">
              + 添加建议
            </el-button>
            <div class="text-muted" style="font-size: 12px">留空将按确认单位生成默认建议，最终金额以费用复核单为准</div>
          </div>
        </el-form-item>
        <el-form-item v-if="concludeForm.conclusion === 'CONFIRMED' && concludeForm.costSuggestion.length" label="分摊依据">
          <el-input v-for="(a, i) in concludeForm.costSuggestion" :key="i" v-model="a.reason"
                    :placeholder="`第 ${i + 1} 行承担方依据`" class="mb16" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="concludeDlg = false">取消</el-button>
        <el-button type="primary" :loading="acting" @click="doConclude">出具复核结论</el-button>
      </template>
    </el-dialog>

    <!-- 市级强制认领 -->
    <el-dialog v-model="forceDlg" title="市级协调：强制认领决定" width="640px">
      <el-alert type="error" :closable="false" class="mb16"
                title="请审阅各单位现场意见、拒签理由及证据后再作决定；强制认领后应急基金垫付款将向该单位追偿。" />
      <el-form label-width="100px">
        <el-form-item label="强制认领单位" required>
          <el-select v-model="forceForm.ownerId" style="width: 100%">
            <el-option v-for="s in activeItem?.signoffs || []" :key="s.signoff.ownerId"
                       :value="s.signoff.ownerId"
                       :label="`${s.ownerOrg}（${SIGN_STATUS[s.signoff.status]?.label}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="决定理由" required>
          <el-input v-model="forceForm.reason" type="textarea" :rows="4"
                    placeholder="结合管线图、施工许可、维修记录及各方证据说明强制认领依据" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forceDlg = false">取消</el-button>
        <el-button type="danger" :loading="acting" @click="doForce">作出强制认领决定</el-button>
      </template>
    </el-dialog>

    <!-- 应急基金垫付 -->
    <el-dialog v-model="advanceDlg" title="应急基金先行垫付" width="480px">
      <el-form label-width="100px">
        <el-form-item label="垫付金额" required>
          <el-input-number v-model="advanceForm.amount" :min="0" :precision="2" />
          <span class="text-muted" style="margin-left: 6px">元</span>
        </el-form-item>
        <el-form-item label="用途说明">
          <el-input v-model="advanceForm.purpose" type="textarea" :rows="3"
                    placeholder="如：先行抢修、临时围护加固，待权属结论出具后追偿" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="advanceDlg = false">取消</el-button>
        <el-button type="warning" :loading="acting" @click="doAdvance">批准垫付</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  apiInitiateReview, apiSignReview, apiConcludeReview,
  apiForceClaim, apiFundAdvance, apiFundRecover, apiUpload
} from '../api'
import { REVIEW_STATUS, SIGN_STATUS, fmtTime, money } from '../utils/format'

const props = defineProps({
  incidentId: { type: [Number, String], required: true },
  ownerStatus: { type: String, default: '' },
  reviews: { type: Array, default: () => [] },
  role: { type: String, default: '' },
  currentUserId: { type: [Number, String], default: null }
})
const emit = defineEmits(['refresh'])

const acting = ref(false)
const activeItem = ref(null)

const isStreetLike = computed(() => ['STREET_DUTY', 'ADMIN'].includes(props.role))
const canInitiate = computed(() =>
  isStreetLike.value
  && ['INVESTIGATING', 'PENDING'].includes(props.ownerStatus)
  && !props.reviews.some(r => ['INITIATED', 'ESCALATED'].includes(r.review.status)))

const signedParticipants = computed(() =>
  (activeItem.value?.signoffs || []).filter(s => s.signoff.status === 'SIGNED'))

function canSign(row) {
  return props.role === 'OWNER_UNIT'
    && row.signoff.ownerId === props.currentUserId
    && row.signoff.status === 'PENDING'
    && activeOf(row)?.review.status === 'INITIATED'
}

function activeOf(row) {
  return props.reviews.find(r => r.signoffs.some(s => s.signoff.id === row.signoff.id))
}

function canConclude(item) {
  if (!isStreetLike.value || item.review.status !== 'INITIATED') return false
  return item.signoffs.length > 0
    && item.signoffs.every(s => s.signoff.status !== 'PENDING')
}

function canForce(item) {
  return props.role === 'CITY_COORDINATOR' && item.review.status === 'ESCALATED'
}

function canRecover(item) {
  return ['CITY_COORDINATOR', 'ADMIN'].includes(props.role)
    && item.advance?.status === 'PENDING' && item.advance.recoverFromOwnerId
}

function recoverOrgOf(item) {
  const s = item.signoffs.find(x => x.signoff.ownerId === item.advance.recoverFromOwnerId)
  return s ? s.ownerOrg : `#${item.advance.recoverFromOwnerId}`
}

function forceTitle(item) {
  return (item.forcedByName || '市级协调人员') + ' 于 ' + fmtTime(item.review.forcedAt)
    + ' 决定强制认领：' + item.review.forceReason
}

async function run(fn, msg) {
  acting.value = true
  try {
    await fn()
    ElMessage.success(msg)
    await emit('refresh')
  } finally {
    acting.value = false
  }
}

// ---------- 发起 ----------
const initDlg = ref(false)
const initForm = reactive({ scheduledAt: '', siteLocation: '', reason: '' })
function openInitiate() {
  Object.assign(initForm, { scheduledAt: '', siteLocation: '', reason: '' })
  initDlg.value = true
}
function doInitiate() {
  if (!initForm.scheduledAt) return ElMessage.warning('请选择复核时间')
  if (!initForm.reason.trim()) return ElMessage.warning('请填写发起事由')
  run(() => apiInitiateReview(props.incidentId, initForm), '联合现场复核已发起，已通知各权属单位')
    .then(() => { initDlg.value = false })
}

// ---------- 签字/拒签 ----------
const signDlg = ref(false)
const signRow = ref(null)
const signFileList = ref([])
const signPhotos = ref([])
const signForm = reactive({ agree: true, representativeName: '', opinion: '', refuseReason: '' })
function openSign(row) {
  signRow.value = row
  signFileList.value = []
  signPhotos.value = []
  Object.assign(signForm, { agree: true, representativeName: '', opinion: '', refuseReason: '' })
  signDlg.value = true
}
async function doSignUpload({ file }) {
  const url = await apiUpload(file)
  signPhotos.value.push(url)
  signFileList.value.push({ name: file.name, url })
}
function onSignRemove(file) {
  signPhotos.value = signPhotos.value.filter(u => u !== file.url)
  signFileList.value = signFileList.value.filter(f => f.url !== file.url)
}
function doSign() {
  if (signForm.agree === false) {
    if (!signForm.refuseReason.trim()) return ElMessage.warning('拒签必须说明理由')
    if (!signPhotos.value.length) return ElMessage.warning('拒签必须上传证据')
  }
  const reviewId = activeOf(signRow.value).review.id
  run(() => apiSignReview(reviewId, {
    agree: signForm.agree,
    representativeName: signForm.representativeName,
    opinion: signForm.opinion,
    refuseReason: signForm.agree === false ? signForm.refuseReason : null,
    evidencePhotos: signForm.agree === false ? signPhotos.value : null
  }), signForm.agree === false ? '拒签意见与证据已提交' : '已签字确认').then(() => { signDlg.value = false })
}

// ---------- 结论 ----------
const concludeDlg = ref(false)
const concludeForm = reactive({
  conclusion: 'CONFIRMED', confirmedOwnerId: null, siteFindings: '', costSuggestion: []
})
function openConclude(item) {
  activeItem.value = item
  Object.assign(concludeForm, {
    conclusion: 'CONFIRMED', confirmedOwnerId: null, siteFindings: item.review.siteFindings || '',
    costSuggestion: []
  })
  concludeDlg.value = true
}
async function doConclude() {
  if (concludeForm.conclusion === 'CONFIRMED' && !concludeForm.confirmedOwnerId) {
    return ElMessage.warning('请选择已签字认可的权属单位')
  }
  const sug = concludeForm.costSuggestion.filter(a => a.party)
  await ElMessageBox.confirm(
    concludeForm.conclusion === 'DISPUTED'
      ? '确认仍无法认定权属？事件将进入市级协调池并通知应急基金垫付。'
      : '确认出具联合复核权属结论？',
    '结束复核', { type: 'warning' })
  run(() => apiConcludeReview(activeItem.value.review.id, {
    conclusion: concludeForm.conclusion,
    confirmedOwnerId: concludeForm.confirmedOwnerId,
    siteFindings: concludeForm.siteFindings,
    costSuggestion: sug.length ? sug : null
  }), '复核结论已出具').then(() => { concludeDlg.value = false })
}

// ---------- 强制认领 ----------
const forceDlg = ref(false)
const forceForm = reactive({ ownerId: null, reason: '' })
function openForce(item) {
  activeItem.value = item
  Object.assign(forceForm, { ownerId: null, reason: '' })
  forceDlg.value = true
}
function doForce() {
  if (!forceForm.ownerId) return ElMessage.warning('请选择强制认领单位')
  if (!forceForm.reason.trim()) return ElMessage.warning('请填写强制认领理由')
  run(() => apiForceClaim(activeItem.value.review.id, { ...forceForm, costSuggestion: null }),
    '强制认领决定已作出，已通知责任单位并锁定追偿').then(() => { forceDlg.value = false })
}

// ---------- 应急基金 ----------
const advanceDlg = ref(false)
const advanceForm = reactive({ amount: 0, purpose: '' })
function openAdvance(item) {
  activeItem.value = item
  Object.assign(advanceForm, { amount: Number(item.review.advanceAmount) || 0, purpose: '' })
  advanceDlg.value = true
}
function doAdvance() {
  if (!advanceForm.amount || advanceForm.amount <= 0) return ElMessage.warning('请填写垫付金额')
  run(() => apiFundAdvance(activeItem.value.review.id, { ...advanceForm }),
    '应急基金垫付已批准').then(() => { advanceDlg.value = false })
}
function doRecover(item) {
  ElMessageBox.confirm(`确认已向 ${recoverOrgOf(item)} 追偿回补 ${money(item.advance.amount)}？`,
    '追偿回补', { type: 'success' })
    .then(() => run(() => apiFundRecover(item.advance.id, { amount: item.advance.amount }), '追偿款已回补应急基金'))
    .catch(() => {})
}
</script>
