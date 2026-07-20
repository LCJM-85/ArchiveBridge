<template>
  <el-main class="content">
    <keep-alive :include="['ArchiveUpload', 'AIAssistant']">
      <component :is="currentComponent" />
    </keep-alive>
  </el-main>
</template>

<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useMenuStore } from '@/store/menu'
import Dashboard from '@/views/dashboard/Dashboard.vue'
import ArchiveUpload from '@/views/archive/ArchiveUpload.vue'
import OCRProcess from '@/views/ocr/OCRProcess.vue'
import ReportGenerate from '@/views/report/ReportGenerate.vue'
import AdmissionData from '@/views/data/AdmissionData.vue'
import StudentStatusData from '@/views/data/StudentStatusData.vue'
import GraduationData from '@/views/data/GraduationData.vue'
import AddmissionTrend from '@/views/charts/AddmissionTrend.vue'
import AIPrediction from '@/views/charts/AIPrediction.vue'
import Geographic from '@/views/charts/Geographic.vue'
import MajorTrainingPath from '@/views/charts/MajorTrainingPath.vue'
import MetaDataManage from '@/views/system/MetaDataManage.vue'
import UserManage from '@/views/system/UserManage.vue'
import AIAssistant from '@/views/ai/AIAssistant.vue'
import Knowledge from '@/views/knowledge/Knowledge.vue'

const menuStore = useMenuStore()
const { activeMenu } = storeToRefs(menuStore)

const componentMap = {
  dashboard: Dashboard,
  upload: ArchiveUpload,
  process: OCRProcess,
  report: ReportGenerate,
  admission: AdmissionData,
  studentstatus: StudentStatusData,
  graduation: GraduationData,
  meta: MetaDataManage,
  users: UserManage,
  'ai-assistant': AIAssistant,
  trend: AddmissionTrend,
  prediction: AIPrediction,
  geo: Geographic,
  path: MajorTrainingPath,
  knowledge: Knowledge
}

const currentComponent = computed(() => componentMap[activeMenu.value] || Dashboard)
</script>

<style scoped>
.content {
  padding: 20px;
  background: var(--content-bg);
}
</style>
