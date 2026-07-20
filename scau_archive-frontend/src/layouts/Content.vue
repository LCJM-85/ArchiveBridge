<template>
  <el-main class="content">
    <TabBar />
    <div class="content-wrapper">
      <keep-alive :include="componentNames">
        <component :is="currentComponent" />
      </keep-alive>
    </div>
  </el-main>
</template>

<script setup>
import { computed } from 'vue'
import { useTabStore } from '@/store/tab'
import TabBar from '@/layouts/TabBar.vue'
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

const tabStore = useTabStore()

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
  knowledge: Knowledge,
}

const keyToName = {
  dashboard: 'Dashboard',
  upload: 'ArchiveUpload',
  process: 'OCRProcess',
  report: 'ReportGenerate',
  admission: 'AdmissionData',
  studentstatus: 'StudentStatusData',
  graduation: 'GraduationData',
  meta: 'MetaDataManage',
  users: 'UserManage',
  'ai-assistant': 'AIAssistant',
  trend: 'AddmissionTrend',
  prediction: 'AIPrediction',
  geo: 'Geographic',
  path: 'MajorTrainingPath',
  knowledge: 'Knowledge',
}

const currentComponent = computed(() => componentMap[tabStore.activeTab] || Dashboard)
const componentNames = computed(() =>
  tabStore.tabs.map((t) => keyToName[t.key]).filter(Boolean)
)
</script>

<style scoped>
.content {
  padding: 0;
  background: var(--content-bg);
  display: flex;
  flex-direction: column;
}

.content-wrapper {
  padding: 20px;
  flex: 1;
  overflow: auto;
}
</style>
