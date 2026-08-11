<template>
  <el-main class="content">
    <!-- 全局装饰插画背景（档案盒/折线/嫩芽，方案B） -->
    <div class="content-art" aria-hidden="true">
      <svg viewBox="0 0 760 560" fill="none" xmlns="http://www.w3.org/2000/svg">
        <g opacity=".55">
          <rect x="420" y="330" width="200" height="140" rx="12" fill="#0e8a5f"/>
          <rect x="448" y="368" width="144" height="10" rx="5" fill="#d9b877" opacity=".85"/>
          <rect x="448" y="392" width="100" height="10" rx="5" fill="rgba(230,205,149,.6)"/>
          <rect x="448" y="416" width="120" height="34" rx="6" fill="rgba(11,92,64,.6)"/>
          <rect x="368" y="392" width="190" height="132" rx="12" fill="#0b5c40"/>
          <rect x="396" y="428" width="134" height="10" rx="5" fill="#c9a45c" opacity=".8"/>
          <rect x="396" y="452" width="96" height="10" rx="5" fill="rgba(230,205,149,.55)"/>
        </g>
        <path d="M60 440 C 160 400, 230 340, 320 320 S 480 240, 560 200" stroke="#0e8a5f" stroke-width="4" stroke-linecap="round" opacity=".5"/>
        <path d="M60 440 C 160 400, 230 340, 320 320 S 480 240, 560 200 L560 560 L60 560 Z" fill="url(#artGrad)" opacity=".16"/>
        <defs>
          <linearGradient id="artGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stop-color="#14a06f" stop-opacity=".5"/>
            <stop offset="1" stop-color="#14a06f" stop-opacity="0"/>
          </linearGradient>
        </defs>
        <g fill="#0e8a5f" opacity=".55">
          <circle cx="160" cy="400" r="6"/><circle cx="320" cy="320" r="6"/><circle cx="480" cy="240" r="6"/>
        </g>
        <circle cx="560" cy="200" r="8" fill="#d9b877" opacity=".8"/>
        <circle cx="560" cy="200" r="16" stroke="#d9b877" stroke-width="2" opacity=".35"/>
        <g opacity=".5">
          <path d="M640 470 V 430" stroke="#0e8a5f" stroke-width="3" stroke-linecap="round"/>
          <path d="M640 448 C 640 424, 668 424, 674 434 C 666 437, 654 440, 644 448" fill="#14a06f"/>
          <path d="M640 460 C 640 438, 666 436, 672 446 C 664 449, 653 452, 644 460" fill="#0e8a5f"/>
        </g>
        <g fill="#c9a45c" opacity=".45">
          <circle cx="120" cy="180" r="5"/><circle cx="300" cy="120" r="4"/><circle cx="520" cy="90" r="5"/>
          <circle cx="700" cy="280" r="4"/><circle cx="80" cy="320" r="4"/>
        </g>
        <circle cx="240" cy="500" r="4" fill="#2fb984" opacity=".4"/>
        <circle cx="660" cy="520" r="5" fill="#2fb984" opacity=".35"/>
      </svg>
    </div>
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
import CollegeManage from '@/views/system/CollegeManage.vue'
import MajorManage from '@/views/system/MajorManage.vue'
import ClassManage from '@/views/system/ClassManage.vue'
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
  college: CollegeManage,
  major: MajorManage,
  class: ClassManage,
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
  college: 'CollegeManage',
  major: 'MajorManage',
  class: 'ClassManage',
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
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

/* 全局装饰插画背景（右下角，半透明，不拦截交互） */
.content-art {
  position: fixed;
  right: -40px;
  bottom: -30px;
  width: min(58vw, 760px);
  opacity: 0.5;
  pointer-events: none;
  z-index: 0;
}
.content-art svg {
  width: 100%;
  height: auto;
  display: block;
}
.dark .content-art {
  opacity: 0.32;
}

.tab-bar {
  position: relative;
  z-index: 1;
  flex-shrink: 0;
}

.content-wrapper {
  position: relative;
  z-index: 1;
  padding: 20px;
  flex: 1;
  overflow: auto;
}

@media (max-width: 768px) {
  .content-wrapper {
    padding: 12px;
  }
}
</style>
