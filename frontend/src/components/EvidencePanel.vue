<template>
  <el-card v-if="ev.profile" class="page-card">
    <template #header>
      <div class="flex-between">
        <b>权属复核证据包（井盖编号 · 管线图 · 施工许可 · 维修记录）</b>
        <el-tag type="danger" size="small">推诿复核专用</el-tag>
      </div>
    </template>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="井盖编号">
        <b style="font-size: 15px">{{ ev.profile.manholeCode }}</b>
      </el-descriptions-item>
      <el-descriptions-item label="井盖类型">
        {{ ev.profile.coverType || '不明' }}
      </el-descriptions-item>
      <el-descriptions-item label="点位地址" :span="2">{{ ev.profile.address }}</el-descriptions-item>
      <el-descriptions-item label="井室内部标识" :span="2">
        {{ ev.profile.chamberMarking || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="台账登记单位">
        <el-tag v-if="ev.registeredOwnerOrg" type="warning" size="small">{{ ev.registeredOwnerOrg }}</el-tag>
        <el-tag v-else type="danger" size="small">台账漏登（无主）</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="地下管线图" :span="2">
        {{ ev.profile.pipelineDiagram || '-' }}
      </el-descriptions-item>
    </el-descriptions>

    <div v-if="ev.pipelinePhotoList?.length" class="mt16">
      <el-image v-for="(p, i) in ev.pipelinePhotoList" :key="i" :src="p"
                :preview-src-list="ev.pipelinePhotoList" fit="cover"
                style="width: 140px; height: 100px; margin-right: 8px; border-radius: 4px" />
    </div>

    <el-divider content-position="left">最近一次施工许可</el-divider>
    <el-descriptions v-if="ev.latestPermit" :column="2" border>
      <el-descriptions-item label="许可编号">{{ ev.latestPermit.permitNo }}</el-descriptions-item>
      <el-descriptions-item label="施工单位">{{ ev.latestPermit.constructor }}</el-descriptions-item>
      <el-descriptions-item label="项目名称" :span="2">{{ ev.latestPermit.projectName }}</el-descriptions-item>
      <el-descriptions-item label="开工时间">{{ fmtTime(ev.latestPermit.startedAt) }}</el-descriptions-item>
      <el-descriptions-item label="竣工时间">{{ fmtTime(ev.latestPermit.finishedAt) }}</el-descriptions-item>
      <el-descriptions-item label="涉及管线单位">{{ ev.latestPermit.involvedUnits || '-' }}</el-descriptions-item>
      <el-descriptions-item label="移交情况">
        <span :class="{ 'text-danger': ev.latestPermit.handoverInfo?.includes('无') || ev.latestPermit.handoverInfo?.includes('漏') }">
          {{ ev.latestPermit.handoverInfo || '-' }}
        </span>
      </el-descriptions-item>
    </el-descriptions>
    <el-empty v-else description="未查询到施工许可记录" :image-size="60" />

    <el-divider content-position="left">历史维修记录</el-divider>
    <el-table v-if="ev.repairRecords?.length" :data="ev.repairRecords" size="small">
      <el-table-column label="维修时间" width="150">
        <template #default="{ row }">{{ fmtTime(row.repairedAt) }}</template>
      </el-table-column>
      <el-table-column prop="repairOrg" label="维修单位" min-width="150" />
      <el-table-column prop="content" label="维修内容" min-width="180" />
      <el-table-column prop="ownerOrg" label="当时权属记载" min-width="140" />
      <el-table-column label="费用" width="100">
        <template #default="{ row }">{{ money(row.cost) }}</template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="暂无历史维修记录" :image-size="60" />
  </el-card>
</template>

<script setup>
import { fmtTime, money } from '../utils/format'

defineProps({
  ev: { type: Object, required: true }
})
</script>
