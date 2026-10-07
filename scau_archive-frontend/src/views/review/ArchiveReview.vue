<template>
  <div class="review-page">
    <div class="page-hero">
      <div class="ph-left"><div class="ph-kicker">SCAU ARCHIVE BRIDGE · 入库前审查</div><h2 class="font-display">入库审查</h2>
        <p>所有文件解析后先在这里修正；确认整份文件后才进入数据管理。</p></div>
      <el-tag>{{ total }} 份文件</el-tag>
    </div>
    <div class="filter-bar">
      <el-select v-model="status" placeholder="审查状态" style="width:160px" @change="query">
        <el-option label="待审查" value="pending_review"/><el-option label="已入库" value="imported"/>
        <el-option label="已放弃" value="discarded"/><el-option label="全部状态" value=""/>
      </el-select>
      <el-select v-model="archiveType" placeholder="档案类型" style="width:160px" @change="query">
        <el-option label="全部档案" value=""/><el-option label="招生档案" value="admission"/>
        <el-option label="毕业档案" value="graduation"/>
      </el-select>
      <el-input v-model="keyword" placeholder="搜索原文件名" :prefix-icon="Search" clearable class="review-file-search" @keyup.enter="query"/>
      <div class="review-filter-actions">
        <el-button :icon="Search" @click="query">查询</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-tag v-if="reviewStore.logId">已按识别任务定位</el-tag>
    </div>
    <el-card shadow="never" class="table-card">
      <el-table :data="files" stripe border v-loading="loading" row-key="draft_id">
        <el-table-column prop="original_file_name" label="原文件名" min-width="240"/>
        <el-table-column label="档案类型" width="120"><template #default="{row}">
          {{ row.archive_type === 'admission' ? '招生档案' : '毕业档案' }}</template></el-table-column>
        <el-table-column prop="row_count" label="记录数" width="90"/>
        <el-table-column label="状态" width="110"><template #default="{row}">
          <el-tag :type="row.status === 'imported' ? 'success' : row.status === 'discarded' ? 'info' : 'warning'">
            {{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="解析完成时间" min-width="180"><template #default="{row}">{{ formatTime(row.created_at) }}</template></el-table-column>
        <el-table-column prop="last_error" label="处理提示" min-width="210" show-overflow-tooltip/>
        <el-table-column label="操作" width="110"><template #default="{row}">
          <el-button link type="primary" @click="open(row.draft_id)">{{ row.status === 'pending_review' ? '审查修正' : '查看记录' }}</el-button>
        </template></el-table-column>
      </el-table>
      <el-pagination v-model:current-page="page" :page-size="10" :total="total"
        layout="total, prev, pager, next" @current-change="load"/>
    </el-card>

    <el-dialog v-model="visible" class="review-dialog" :title="draft?.original_file_name || '审查记录'" width="min(1800px, 96vw)" top="4vh"
      :close-on-click-modal="false" :before-close="beforeClose" @closed="closeSource">
      <template v-if="draft">
        <div class="review-toolbar">
          <el-tag>{{ statusText(draft.status) }}</el-tag>
          <el-tag v-if="dirty" type="warning">有未保存修改，评分及提示以最近保存内容为准</el-tag>
          <span v-else>数据质量 {{ draft.score?.totalScore ?? '-' }} 分（不是识别准确率）</span>
          <el-button @click="sourceVisible ? closeSource() : preview()">{{ sourceVisible ? '收起原文件' : '查看原文件' }}</el-button>
          <el-button v-if="editable" :disabled="busy" @click="addRow">新增记录</el-button>
          <el-button v-if="editable" type="danger" plain :disabled="busy || !selected.length" @click="removeSelected">删除选中行</el-button>
        </div>
        <el-alert v-if="draft.last_error" :title="draft.last_error" type="error" :closable="false" show-icon/>
        <el-alert v-if="editable" title="确认会提交全部记录，不仅当前页；已有学生可能按学号、身份证或考生号匹配更新。" type="info" :closable="false"/>
        <div class="review-workspace" :class="{'is-split':sourceVisible}">
          <aside v-if="sourceVisible" class="source-pane" v-loading="sourceLoading">
            <div class="source-pane-header"><span>原文件 · {{ draft.original_file_name }}</span><el-button link @click="closeSource">收起</el-button></div>
            <div class="source-content">
              <SourceImagePreview v-if="sourceUrl && sourceType.startsWith('image/')" :src="sourceUrl" :key="sourceUrl" alt="原文件完整预览"/>
              <iframe v-else-if="sourceUrl && sourceType==='application/pdf'" :src="sourceUrl" class="source-pdf" title="原文件预览"/>
              <a v-else-if="sourceUrl" :href="sourceUrl" :download="draft.original_file_name">此文件不支持在线预览，下载原文件查看</a>
              <div v-else-if="!sourceLoading" class="source-empty">原文件加载失败<el-button link @click="preview">重试</el-button></div>
            </div>
          </aside>
          <div class="records-pane">
        <el-tabs v-model="activeView">
          <el-tab-pane label="修正记录" name="edited">
            <el-table class="review-records-table" :data="visibleRows" border stripe row-key="_row_id" :height="reviewTableHeight" size="small"
              @selection-change="selected = $event" :key="`${rowPage}-${rowPageSize}`">
              <el-table-column v-if="editable" type="selection" width="45"/>
              <el-table-column label="行号" width="65"><template #default="scope">{{ (rowPage-1)*rowPageSize+scope.$index+1 }}</template></el-table-column>
              <el-table-column v-for="field in draft.fields" :key="field.code" :label="field.label" :min-width="field.code==='id_card' ? 225 : 155">
                <template #default="{row}">
                  <el-input v-if="editable" size="small" :disabled="busy" v-model="row[field.code]" @input="dirty=true" :placeholder="field.required ? '建议填写' : '未填写'"/>
                  <span v-else>{{ row[field.code] || '未填写' }}</span>
                </template>
              </el-table-column>
              <el-table-column v-if="editable" label="操作" fixed="right" width="75"><template #default="{row}">
                <el-button link type="danger" :disabled="busy" @click="removeRow(row._row_id)">删除</el-button>
              </template></el-table-column>
            </el-table>
            <el-pagination v-model:current-page="rowPage" v-model:page-size="rowPageSize" :page-sizes="[20,30,50]" :total="draft.records.length" layout="total, sizes, prev, pager, next" @size-change="rowPage=1"/>
            <div class="issues">
              <div v-for="(issue,index) in draft.issues" :key="index" :class="issue.level">
                {{ issue.level==='error' ? '阻止入库' : '提示' }} · {{ issue.row ? '第'+issue.row+'行 · ' : '' }}{{ issue.message }}
              </div>
              <span v-if="!draft.issues.length">当前保存内容没有字段校验提示。</span>
            </div>
          </el-tab-pane>
          <el-tab-pane label="原始解析（只读）" name="original">
            <el-table class="review-records-table" :data="originalRows" border stripe :height="reviewTableHeight" size="small">
              <el-table-column v-for="key in originalKeys" :key="key" :prop="key" :label="fieldLabel(key)" min-width="170"/>
            </el-table>
            <el-pagination v-model:current-page="originalPage" v-model:page-size="originalPageSize" :page-sizes="[20,30,50]" :total="draft.originalRecords.length" layout="total, sizes, prev, pager, next" @size-change="originalPage=1"/>
            <details v-if="draft.originalIssues.length"><summary>原始解析提示（不会因修正而覆盖）</summary><pre>{{ JSON.stringify(draft.originalIssues,null,2) }}</pre></details>
          </el-tab-pane>
        </el-tabs>
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="beforeClose(()=>visible=false)">关闭</el-button>
        <el-button v-if="editable" type="danger" plain :loading="busy" @click="discard">放弃入库</el-button>
        <el-button v-if="editable" :loading="busy" @click="save">保存修正并重新校验</el-button>
        <el-button v-if="editable || draft?.last_error && draft?.status==='imported'" type="primary" :loading="busy" @click="confirm">
          {{ editable ? '确认整份文件入库' : '重试原文件归档' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, computed, onActivated, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import SourceImagePreview from '@/components/SourceImagePreview.vue'
import { listReviews, getReview, saveReview, confirmReview, discardReview, getReviewSource } from '@/api/modules/review'
import { useReviewStore } from '@/store/review'
defineOptions({name:'ArchiveReview'})
const reviewStore=useReviewStore()
const status=ref(''),archiveType=ref(''),keyword=ref(''),page=ref(1)
const total=ref(0),files=ref([]),loading=ref(false),busy=ref(false)
const visible=ref(false),draft=ref(null),dirty=ref(false),selected=ref([])
const rowPage=ref(1),originalPage=ref(1),activeView=ref('edited')
const rowPageSize=ref(30),originalPageSize=ref(30)
const reviewTableHeight='clamp(260px, 50vh, 560px)'
const sourceVisible=ref(false),sourceUrl=ref(''),sourceType=ref(''),sourceLoading=ref(false)
let sourceRequestId=0
const editable=computed(()=>draft.value?.status==='pending_review')
const visibleRows=computed(()=>draft.value?.records.slice((rowPage.value-1)*rowPageSize.value,rowPage.value*rowPageSize.value)||[])
const originalRows=computed(()=>draft.value?.originalRecords.slice((originalPage.value-1)*originalPageSize.value,originalPage.value*originalPageSize.value)||[])
const originalKeys=computed(()=>[...new Set(draft.value?.originalRecords.flatMap(Object.keys)||[])])
function statusText(value){return {pending_review:'待审查',imported:'已入库',discarded:'已放弃'}[value]||value}
function formatTime(value){return value ? new Date(value).toLocaleString('zh-CN',{hour12:false}) : '-'}
function fieldLabel(key){return draft.value.fields.find(f=>f.code===key)?.label||key}
function errorMessage(error){return error.response?.data?.msg||error.message||'操作失败'}
async function load(){
  loading.value=true
  try{
    const result=await listReviews({status:status.value,archiveType:archiveType.value,keyword:keyword.value,
      page:page.value,size:10,logId:reviewStore.logId})
    files.value=result.records;total.value=result.total
  }catch(error){ElMessage.error(errorMessage(error))}finally{loading.value=false}
}
function query(){page.value=1;load()}
function reset(){reviewStore.logId=null;status.value='';archiveType.value='';keyword.value='';query()}
async function open(id){
  closeSource()
  try{draft.value=await getReview(id);dirty.value=false;selected.value=[];rowPage.value=1;originalPage.value=1;activeView.value='edited';visible.value=true}
  catch(error){ElMessage.error(errorMessage(error))}
}
function addRow(){
  const row={_row_id:crypto.randomUUID()}
  draft.value.fields.forEach(f=>row[f.code]='')
  draft.value.records.push(row);dirty.value=true;rowPage.value=Math.ceil(draft.value.records.length/rowPageSize.value)
}
function removeRow(id){
  draft.value.records=draft.value.records.filter(r=>r._row_id!==id);dirty.value=true
  rowPage.value=Math.min(rowPage.value,Math.max(1,Math.ceil(draft.value.records.length/rowPageSize.value)))
}
function removeSelected(){const ids=new Set(selected.value.map(r=>r._row_id));ids.forEach(removeRow);selected.value=[]}
async function save(){
  if(busy.value)return
  busy.value=true
  try{draft.value=await saveReview(draft.value.draft_id,{version:draft.value.version,records:draft.value.records});dirty.value=false;ElMessage.success('修正已保存，评分和提示已更新');await load()}
  catch(error){ElMessage.error(errorMessage(error))}finally{busy.value=false}
}
async function confirm(){
  if(busy.value)return
  try{await ElMessageBox.confirm('将确认这份文件的全部记录入库。字段提示允许确认，格式错误会阻止入库。是否继续？','确认入库',{type:'warning'})}
  catch{return}
  busy.value=true
  try{
    draft.value=await confirmReview(draft.value.draft_id,{version:draft.value.version,records:draft.value.records})
    dirty.value=false;ElMessage.success(draft.value.last_error ? '数据已入库，原文件归档需要重试' : '已确认入库');await load()
  }catch(error){
    ElMessage.error(errorMessage(error))
    // 入库失败时服务端已保留本次编辑；冲突时保留本地编辑，供用户比较。
    if(error.response?.data?.data?.reason!=='version_conflict'){
      try{
        const localRecords=draft.value.records
        const latest=await getReview(draft.value.draft_id)
        if(latest.status==='pending_review'){
          // 保存结果未知时仅同步版本与提示，不用旧服务端记录覆盖本地修正。
          dirty.value=JSON.stringify(localRecords)!==JSON.stringify(latest.records)
          draft.value={...latest,records:localRecords}
        }else{draft.value=latest;dirty.value=false}
      }catch{}
    }
  }finally{busy.value=false}
}
async function discard(){
  if(busy.value)return
  try{await ElMessageBox.confirm('放弃后该草稿将只读，记录不会进入业务数据。原解析和原文件仍保留。','放弃入库',{type:'warning'})}catch{return}
  busy.value=true
  try{draft.value=await discardReview(draft.value.draft_id,draft.value.version);dirty.value=false;await load()}
  catch(error){ElMessage.error(errorMessage(error))}finally{busy.value=false}
}
async function beforeClose(done){
  if(busy.value)return
  if(dirty.value){try{await ElMessageBox.confirm('关闭将丢失尚未保存的本地修改。是否关闭？','未保存修改',{type:'warning'})}catch{return}}
  closeSource();done()
}
function releaseSource(){if(sourceUrl.value)URL.revokeObjectURL(sourceUrl.value);sourceUrl.value='';sourceType.value=''}
function closeSource(){sourceRequestId++;sourceVisible.value=false;sourceLoading.value=false;releaseSource()}
async function preview(){
  const draftId=draft.value?.draft_id
  if(!draftId)return
  const requestId=++sourceRequestId
  releaseSource();sourceVisible.value=true;sourceLoading.value=true
  try{
    const response=await getReviewSource(draftId)
    // 关闭或切换草稿后，迟到的响应不能重新打开预览。
    if(requestId!==sourceRequestId || !sourceVisible.value || draft.value?.draft_id!==draftId)return
    sourceType.value=response.data.type;sourceUrl.value=URL.createObjectURL(response.data)
  }catch(error){if(requestId===sourceRequestId)ElMessage.error(errorMessage(error))}
  finally{if(requestId===sourceRequestId)sourceLoading.value=false}
}
onActivated(load)
watch(()=>reviewStore.logId,()=>{status.value='';query()})
onUnmounted(closeSource)
</script>
<style scoped>
.review-page{display:flex;flex-direction:column;gap:20px}
.page-hero{display:flex;align-items:center;justify-content:space-between;background:var(--bg-primary);padding:22px 28px;border-radius:16px}
h2{margin:8px 0;font-size:28px}p{margin:8px 0;color:var(--text-secondary);font-size:14px}
.filter-bar,.review-toolbar{display:flex;align-items:center;gap:12px;flex-wrap:wrap}
.filter-bar{padding:16px 20px;gap:12px;background:var(--card-bg);border:1px solid var(--border-color);border-radius:8px}
.review-file-search{width:280px;max-width:100%}
.review-filter-actions{display:flex;align-items:center;gap:12px;flex-wrap:wrap;border-left:1px solid var(--border-color);padding-left:16px;margin-left:4px}
.review-filter-actions :deep(.el-button){margin-left:0}
.filter-bar :deep(.el-input__wrapper),.filter-bar :deep(.el-select__wrapper){border-radius:9px}
@media(max-width:768px){.review-filter-actions{border-left:0;padding-left:0;margin-left:0}.review-file-search{width:100%}}
.table-card{border-radius:16px}.el-pagination{justify-content:flex-end;margin-top:16px}
.review-toolbar{margin-bottom:16px}.el-alert{margin:12px 0}
.issues{margin-top:10px;max-height:80px;overflow:auto;font-size:13px;line-height:1.8}
.error{color:var(--el-color-danger)}.warning{color:var(--el-color-warning)}
pre{white-space:pre-wrap;max-height:200px;overflow:auto}
.review-dialog{display:flex;flex-direction:column;max-height:92vh;overflow:hidden}
.review-dialog :deep(.el-dialog__body){min-height:0;overflow:auto}
.review-dialog :deep(.el-dialog__header),.review-dialog :deep(.el-dialog__footer){flex-shrink:0}
.review-dialog :deep(.el-dialog__footer){display:flex;justify-content:flex-end;gap:8px;flex-wrap:wrap}
.review-dialog :deep(.el-dialog__footer .el-button){margin-left:0}
.review-workspace{display:grid;grid-template-columns:minmax(0, 1fr);gap:20px;min-width:0}
.review-workspace.is-split{grid-template-columns:minmax(0, 1fr) minmax(0, 1fr)}
.records-pane{min-width:0}
.source-pane{min-width:0;align-self:start;border:1px solid var(--border-color);border-radius:8px;overflow:hidden}
.source-pane-header{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:10px 12px;border-bottom:1px solid var(--border-color)}
.source-pane-header span{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.source-content{height:clamp(260px, 50vh, 560px);padding:8px;overflow:hidden;box-sizing:border-box}
.source-pdf{display:block;width:100%;height:100%;border:0}
.source-empty{display:flex;gap:12px;align-items:center;color:var(--text-secondary)}
.review-records-table :deep(.el-table__cell){padding:6px 0}
.records-pane .el-pagination{flex-wrap:wrap;gap:8px;margin-top:12px}
@media(max-width:1100px){.review-workspace.is-split{grid-template-columns:minmax(0, 1fr)}.source-content{height:32vh}}
</style>
