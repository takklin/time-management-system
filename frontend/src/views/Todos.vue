<template>
  <div class="todo-container">
    <!-- OKR 卡片 -->
    <el-card class="okr-card" shadow="never">
      <div class="okr-content">
        <div class="okr-icon">🎯</div>
        <div class="okr-text">
          <h3>聚焦核心目标</h3>
          <p>将任务与 OKR 对齐，优先推进真正重要的工作。建议：每天专注 2-3 件核心任务。</p>
        </div>
        <div style="margin-left:auto; display:flex; gap:8px; align-items:center">
          <el-button type="text" size="small" @click="showOKRInfo = !showOKRInfo">OKR 说明</el-button>
          <el-button type="text" size="small" @click="showScoring = !showScoring">评价标准</el-button>
        </div>
      </div>
    </el-card>

    <div v-if="showOKRInfo" class="okr-panel">
      <div class="okr-header">
        <div class="okr-badge">🎯</div>
        <div class="okr-title-block">
          <h4>什么是 OKR？</h4>
          <div class="okr-sub">Objectives & Key Results — 目标与关键成果简介</div>
        </div>
      </div>
      <div class="okr-body">
        <dl>
          <dt>Objective（目标）</dt>
          <dd>定性、鼓舞人心的方向，指明要往哪里去。例如：提升个人时间管理能力。</dd>
          <dt>Key Results（关键成果）</dt>
          <dd>衡量目标达成的定量指标，通常 2~5 个。例如：连续 30 天每日任务完成率 ≥ 80%。</dd>
        </dl>
        <hr />
        <p class="okr-tip">在待办页使用“与 OKR 对齐”可帮助你把注意力聚焦在真正推动目标的任务上，避免被琐事淹没。</p>
        <div class="okr-metrics">
          <div class="okr-metric"><strong>建议：</strong><span class="okr-pill">每天专注 2–3 件</span> 核心任务</div>
          <div class="okr-metric"><strong>评分规则：</strong>对齐×1.5 · 紧急×1.2 · 耗时×0.5 · 依赖×1.0 → 归一化 0–10</div>
        </div>
      </div>
    </div>

    <div v-if="showScoring" class="scoring-panel">
      <h4>优先级评分规则（维度 1~3 分）</h4>
      <table class="scoring-table">
        <thead>
          <tr><th>维度</th><th>高 (3分)</th><th>中 (2分)</th><th>低 (1分)</th></tr>
        </thead>
        <tbody>
          <tr><td>与OKR对齐</td><td>核心对齐</td><td>部分对齐</td><td>无关</td></tr>
          <tr><td>紧急程度</td><td>今天必须</td><td>本周内</td><td>可推迟</td></tr>
          <tr><td>预计耗时</td><td>大块任务(&gt;2h)</td><td>中等</td><td>碎片任务</td></tr>
          <tr><td>他人依赖</td><td>阻塞他人</td><td>独立</td><td>被他人阻塞</td></tr>
        </tbody>
      </table>
      <div class="scoring-formula">综合分数 = 对齐分×1.5 + 紧急分×1.2 + 耗时分×0.5 + 依赖分×1.0（权重可调）</div>
    </div>

    <!-- 今日心智栏 -->
    <div class="mindset-bar">
      <div class="date">{{ todayStr }}</div>
      <div class="quote">{{ todayQuote }}</div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="left">
        <el-switch v-model="smartSort" active-text="智能排序" inactive-text="默认排序" />
        <el-select v-model="activeFilter" placeholder="筛选" clearable style="width: 140px; margin-left: 12px">
          <el-option label="全部" value="all" />
          <el-option label="未完成" value="active" />
          <el-option label="已完成" value="completed" />
        </el-select>
      </div>
      <div class="right">
        <el-button type="primary" @click="openAddDialog">+ 新建待办</el-button>
      </div>
    </div>

    <!-- 过载保护警示条 -->
    <el-alert v-if="overloadAlert" title="今天任务有点多" type="warning" show-icon :closable="false" class="overload-alert">
      <template #default>
        <div>
          你的精力有限，建议只挑 <strong>2-3 件最重要的事</strong> 完成，其余可以：
          <el-button link type="primary" @click="delayLowPriorityTasks">一键推迟非高优任务</el-button>
          <el-button link type="primary" @click="copyPriorityRequest">请求Leader拍优先级</el-button>
          <el-button link type="primary" @click="deleteNonCoreTasks">直接删除非核心任务</el-button>
        </div>
      </template>
    </el-alert>

    <div class="main-columns">
      <div class="todo-list">
        <!-- 高优队列 -->
        <div v-if="highList.length" class="priority-group high-priority">
          <div class="group-title">🔥 高优先级 · 务必完成</div>
          <draggable v-model="highList" item-key="id" @end="onDragEnd">
            <template #item="{element}">
              <TodoItem :task="element" @complete="onComplete" @delay="delayTodo" @delete="deleteTodo" @request-priority="() => copyPriorityRequestForTask(element)" @edit="openEditTask" />
            </template>
          </draggable>
        </div>

        <!-- 中优队列 -->
        <div v-if="midList.length" class="priority-group medium-priority">
          <div class="group-title">⚡ 中优先级 · 酌情处理</div>
          <draggable v-model="midList" item-key="id" @end="onDragEnd">
            <template #item="{element}">
              <TodoItem :task="element" @complete="onComplete" @delay="delayTodo" @delete="deleteTodo" @edit="openEditTask" />
            </template>
          </draggable>
        </div>

        <!-- 摆烂鱼塘 -->
        <div v-if="lowList.length" class="priority-group procrastinate-group">
          <div class="group-title">
            <span>🐟 可拖延鱼塘</span>
            <el-button type="text" size="small" @click="lowCollapsed = !lowCollapsed" style="margin-left:8px">{{ lowCollapsed ? '展开' : '折叠' }}</el-button>
          </div>
          <div class="group-desc">这些任务不做天也不会塌下来。等有空再说，或者直接删除。</div>
          <div v-if="!lowCollapsed">
            <draggable v-model="lowList" item-key="id" @end="onDragEnd">
              <template #item="{element}">
                <TodoItem :task="element" :showProcrastinateBadge="true" @complete="onComplete" @delay="delayTodo" @delete="deleteTodo" @edit="openEditTask" />
              </template>
            </draggable>
          </div>
          <div v-else class="collapsed-summary">折叠中 · {{ lowList.length }} 个任务（可随时展开）</div>
        </div>

        <el-empty v-if="!highList.length && !midList.length && !lowList.length">暂无待办，给自己放个假吧</el-empty>
      </div>

      <!-- 右侧侧边栏：快速模板与心理洞察（拆分为两个卡片） -->
      <div class="right-sidebar">
        <el-card class="sidebar-card quick-card" shadow="never">
          <div class="sidebar-title">📝 快速插入</div>
          <div class="quick-templates-card">
            <el-button size="small" @click="addTemplate('core')">🎯 [核心] 推进OKR最关键一步</el-button>
            <el-button size="small" @click="addTemplate('chore')">🧹 [杂活] 不紧急的行政事务</el-button>
            <el-button size="small" @click="addTemplate('sync')">💬 [沟通] 同步协作方（不阻塞自己）</el-button>
          </div>
        </el-card>

        <el-card class="sidebar-card mindset-card" shadow="never" style="margin-top:12px">
          <div class="sidebar-title">😌 心理洞察</div>
          <div class="mindset-card">
            <div class="load-assessment">
              当前高优任务: {{ highList.length }} / 建议 ≤3
              <el-progress :percentage="Math.min(100, (highList.length/3)*100)" :color="highList.length>3?'#f56c6c':'#67c23a'" />
              <span v-if="highList.length > 3" class="warning">你的精力带宽有限，考虑删减或推迟</span>
            </div>

            <div class="decision-card">
              <el-button type="text" @click="showDecisionHelper = !showDecisionHelper">🤔 决策自问清单</el-button>
              <div v-if="showDecisionHelper" class="decision-body">
                <ol class="decision-list">
                  <li>
                    <div class="num">1</div>
                    <div class="content"><strong>如果不做，谁会受影响？影响多大？</strong>
                      <div class="note">评估影响范围与时间成本，越多人受影响越优先。</div>
                    </div>
                  </li>
                  <li>
                    <div class="num">2</div>
                    <div class="content"><strong>这件事对我的 OKR 有直接贡献吗？</strong>
                      <div class="note">若能直接推动关键成果，优先级应上升。</div>
                    </div>
                  </li>
                  <li>
                    <div class="num">3</div>
                    <div class="content"><strong>能否委派或简化？</strong>
                      <div class="note">委派或拆分为更小的任务可以降低优先级。</div>
                    </div>
                  </li>
                  <li>
                    <div class="num">4</div>
                    <div class="content"><strong>是否有明确截止/对他人有阻塞依赖？</strong>
                      <div class="note">若阻塞他人或截止临近，应优先处理或沟通延期。</div>
                    </div>
                  </li>
                </ol>
              </div>
            </div>

            <div class="quote-sidebar">{{ randomQuote }}</div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="addDialogVisible" :teleported="false" title="添加待办" width="520px">
      <el-form :model="newTask" label-width="120px">
        <el-form-item label="标题">
          <el-input v-model="newTask.title" autocomplete="off" />
        </el-form-item>
        <el-form-item label="与OKR对齐">
          <el-switch v-model="newTask.isCoreAligned" active-text="核心对齐" inactive-text="无关" />
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-radio-group v-model="newTask.urgency">
            <el-radio label="today">今天必须</el-radio>
            <el-radio label="week">本周内</el-radio>
            <el-radio label="later">可推迟</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="预计耗时">
          <el-radio-group v-model="newTask.estimatedEffort">
            <el-radio label="large">大块任务 (&gt;2h)</el-radio>
            <el-radio label="medium">中等</el-radio>
            <el-radio label="small">碎片任务</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="他人依赖">
          <el-radio-group v-model="newTask.dependency">
            <el-radio label="blocking">阻塞他人</el-radio>
            <el-radio label="independent">独立</el-radio>
            <el-radio label="blocked">被他人阻塞</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="newTask.deadline" type="date" placeholder="选择日期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addTask">确定</el-button>
      </template>
    </el-dialog>

    <!-- 编辑任务对话（与 Tasks.vue 保持一致的字段） -->
    <el-dialog v-model="editDialogVisible" :teleported="false" title="编辑任务" width="600px">
      <el-form :model="editForm" label-width="110px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" autocomplete="off" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editForm.categoryId" placeholder="选择分类" clearable>
            <el-option v-for="cat in taskStore.categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="editForm.priority">
            <el-radio label="high">高</el-radio>
            <el-radio label="medium">中</el-radio>
            <el-radio label="low">低</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="起始时间">
          <el-date-picker v-model="editForm.startTime" type="datetime" placeholder="选择日期时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="预估时长(分)">
          <DurationSlider v-model="editForm.estimatedMinutes" :min="0" :max="1440" :step="5" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="4" :placeholder="descriptionPlaceholder" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEditedTask">保存</el-button>
      </template>
    </el-dialog>

    <!-- 完成任务弹窗（填写开始时间与持续时长） -->
    <el-dialog v-model="completeDialogVisible" :teleported="false" title="完成任务 - 填写时间" width="520px">
      <el-form label-width="120px">
        <el-form-item label="开始时间">
          <el-date-picker v-model="completeStartTime" type="datetime" placeholder="选择开始时间" style="width:100%" />
        </el-form-item>
        <el-form-item label="持续时长（分钟）">
          <el-input-number v-model="completeDuration" :min="1" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmComplete">确认并完成</el-button>
      </template>
    </el-dialog>
    <!-- 后备编辑模态（当 Element Plus dialog 未渲染时显示） -->
    <div v-if="fallbackDialogVisible" class="fallback-overlay">
      <div class="fallback-modal">
        <h3>编辑任务</h3>
        <el-form :model="editForm" label-width="110px">
          <el-form-item label="标题">
            <el-input v-model="editForm.title" autocomplete="off" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="editForm.categoryId" placeholder="选择分类" clearable>
              <el-option v-for="cat in taskStore.categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级">
            <el-radio-group v-model="editForm.priority">
              <el-radio label="high">高</el-radio>
              <el-radio label="medium">中</el-radio>
              <el-radio label="low">低</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="起始时间">
            <el-date-picker v-model="editForm.startTime" type="datetime" placeholder="选择日期时间" style="width:100%" />
          </el-form-item>
          <el-form-item label="预估时长(分)">
            <DurationSlider v-model="editForm.estimatedMinutes" :min="0" :max="1440" :step="5" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="editForm.description" type="textarea" :rows="4" :placeholder="descriptionPlaceholder" />
          </el-form-item>
        </el-form>
        <div style="display:flex; justify-content:flex-end; gap:8px; margin-top:12px">
          <el-button @click="fallbackDialogVisible = false; editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEditedTask">保存</el-button>
        </div>
      </div>
    </div>

    <!-- 庆祝浮层 -->
    <div v-if="celebrationVisible" class="celebration-toast">🎉 {{ celebrationMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, onActivated } from 'vue'
import dayjs from 'dayjs'
import isBetween from 'dayjs/plugin/isBetween'
import { nextTick } from 'vue'
dayjs.extend(isBetween)
import { ElMessage } from 'element-plus'
import Draggable from 'vuedraggable'
import TodoItem from '@/components/TodoItem.vue'
import DurationSlider from '@/components/DurationSlider.vue'
import { useTimeRecordStore } from '@/store/time-record'
import { usePomodoroStore } from '@/store/pomodoro'
import { useTodoStore, type Todo } from '@/stores/todo'
import { useTaskStore } from '@/store/task'
import { useUserStore } from '@/store/user'

const todoStore = useTodoStore()
const timeRecordStore = useTimeRecordStore()
const taskStore = useTaskStore()
const userStore = useUserStore()
const pomodoroStore = usePomodoroStore()
const showOKRInfo = ref(false)
const showDecisionHelper = ref(false)

const todayStr = ref(new Date().toLocaleDateString('zh-CN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }))
const quotes = [
  '完美是完成的天敌。',
  '最重要的事只有一件。',
  '紧急的事往往不重要，重要的事往往不紧急。',
  '如果所有事都重要，那没有一件事重要。',
  '学会对杂活说‘不’，对核心说‘是’。',
]
const randomQuote = ref(quotes[Math.floor(Math.random()*quotes.length)])
const todayQuote = ref(quotes[Math.floor(Math.random()*quotes.length)])

const smartSort = ref(true)
const activeFilter = ref<'all'|'active'|'completed'>('all')
const addDialogVisible = ref(false)
const newTask = reactive<any>({ title: '', isCoreAligned: false, urgency: 'later', estimatedEffort: 'small', dependency: 'independent', deadline: null })

const showScoring = ref(false)
const lowCollapsed = ref(false)

const OVERLOAD_THRESHOLD = 5

const sorted = computed(() => {
  const arr = [...todoStore.tasks]
  if (smartSort.value) arr.sort((a,b) => todoStore.computePriorityScore(b) - todoStore.computePriorityScore(a))
  return arr
})

const highList = ref<Todo[]>([])
const midList = ref<Todo[]>([])
const lowList = ref<Todo[]>([])
const completedList = ref<Todo[]>([])

const refreshGroups = () => {
  const arr = sorted.value
  // 仅展示与今日前后近 3 天相关的任务（含无日期任务）
  const windowStart = dayjs().subtract(3, 'day').startOf('day')
  const windowEnd = dayjs().add(3, 'day').endOf('day')
  const arrFiltered = arr.filter(t => {
    const dt = (t as any).startTime || (t as any).deadline
    if (!dt) return true
    try { return dayjs(dt).isBetween(windowStart, windowEnd, null, '[]') } catch { return true }
  })

  highList.value = arrFiltered.filter(t => !t.completed && todoStore.computePriorityScore(t) >= 7)
  midList.value = arrFiltered.filter(t => !t.completed && todoStore.computePriorityScore(t) >=4 && todoStore.computePriorityScore(t) < 7)
  lowList.value = arrFiltered.filter(t => !t.completed && todoStore.computePriorityScore(t) < 4)
  completedList.value = arrFiltered.filter(t => t.completed)
}

watch(() => todoStore.tasks, () => { refreshGroups() }, { deep: true, immediate: true })

const overloadAlert = computed(() => {
  const today = dayjs().format('YYYY-MM-DD')
  const count = todoStore.tasks.filter(t => !t.completed && t.deadline === today).length
  return count >= OVERLOAD_THRESHOLD
})

const celebrationVisible = ref(false)
const celebrationMessage = ref('')

// 完成任务（与 Tasks.vue 保持一致的行为）
const completeDialogVisible = ref(false)
const completingOriginId = ref<string | number | null>(null)
const completingTodoLocalId = ref<number | string | null>(null)
const completeStartTime = ref<any>(null)
const completeDuration = ref<number | null>(null)

const onComplete = async (task: Todo, checked?: boolean) => {
  const markCompleted = typeof checked === 'boolean' ? checked : true
  // 如果当前任务正在被番茄钟计时，先停止并记录实际用时，确保登出/完成时的数据一致
  try {
    const activeId = pomodoroStore.activeTaskId
    const taskIdToCompare = (task as any).originTaskId ?? task.id
    if (pomodoroStore.isRunning && activeId != null && String(activeId) === String(taskIdToCompare)) {
      try { await pomodoroStore.stop(true) } catch (e) { console.warn('stop pomodoro before complete failed', e) }
    }
  } catch (e) { console.warn('pomodoro check on complete failed', e) }
  if (!markCompleted) {
    // 取消完成：若为后端任务，调用 TaskStore；否则本地回滚
    if (task.originTaskId) {
      try { await taskStore.updateTask(task.originTaskId as string|number, { completed: false }) } catch (e) { console.error(e) }
      // reload tasks
      try { await taskStore.fetchTasks() } catch {}
    } else {
      todoStore.markComplete(task.id)
    }
    refreshGroups()
    return
  }

  // 若任务来源于后端，使用后端完成流程
  if (task.originTaskId) {
    const originId = task.originTaskId
    try {
      // 尝试读取近 30 天的 time records
      await fetchRecordsForRange('month')
      const existing = timeRecordStore.records.filter(r => String(r.taskId) === String(originId) && r.endTime)
      const taskObj: any = taskStore.tasks.find((t:any) => String(t.id) === String(originId))

      const hasEstimate = (() => {
        if (!taskObj) return false
        if (Object.prototype.hasOwnProperty.call(taskObj, 'estimatedMinutes') && taskObj.estimatedMinutes != null) {
          const mins = Number(taskObj.estimatedMinutes)
          if (!Number.isNaN(mins) && mins > 0) return true
        }
        if (Object.prototype.hasOwnProperty.call(taskObj, 'estimatedTime') && taskObj.estimatedTime != null) {
          const hours = Number(taskObj.estimatedTime)
          const mins = Number.isNaN(hours) ? 0 : Math.round(hours * 60)
          if (mins > 0) return true
        }
        return false
      })()

      if (existing && existing.length > 0) {
        if (!hasEstimate) {
          ElMessage.warning('该任务已有时间记录，但未填写预估时长，请先完善预估时长后再完成任务')
          if (taskObj) openEditTask(task)
          return
        }
        const total = existing.reduce((s, r) => s + (Number((r as any).duration) || 0), 0)
        await taskStore.updateTask(originId, { completed: true, estimatedTime: total / 60, actualMinutes: total })
        await fetchRecordsForRange('month')
        await taskStore.fetchTasks()
        ElMessage.success('任务已完成，时间已记录')
        return
      }

      // 无已结束时间记录，弹窗要求填写开始时间和持续时长
      completingOriginId.value = originId
      completingTodoLocalId.value = null
      completeStartTime.value = null
      completeDuration.value = null
      completeDialogVisible.value = true
      return
    } catch (e) {
      console.error('完成任务失败：', e)
      ElMessage.error('完成操作失败')
      return
    }
  }

  // 否则为本地 todo，直接标记并展示庆祝
  const isHigh = todoStore.markComplete(task.id)
  if (isHigh) {
    celebrationMessage.value = '太棒了！你搞定了今天最重要的事。🎉 剩下的已经不重要了，可以放松一下。'
    celebrationVisible.value = true
    setTimeout(() => { celebrationVisible.value = false }, 3000)
    const key = 'weeklyCoreDone'
    const prev = parseInt(localStorage.getItem(key) || '0') || 0
    localStorage.setItem(key, String(prev+1))
  }
}

const delayTodo = (task: Todo) => {
  todoStore.delayTask(task.id, 1)
  ElMessage.info('已推迟到明天')
}

const delayLowPriorityTasks = () => {
  todoStore.delayLowPriorityTasks()
  ElMessage.success('已将低优先级任务全部推迟一天')
}

const copyPriorityRequest = () => {
  const highTitles = highList.value.map(t => t.title).join('、')
  const message = `Leader，我当前正处理以下高优任务：${highTitles}。如果再加上其他任务，我无法保证质量，请帮我拍定优先级。`
  try { navigator.clipboard.writeText(message); ElMessage.success('已复制请求话术，可发给Leader') } catch (e) { ElMessage.info('请手动复制：'+message) }
}

const copyPriorityRequestForTask = (task: Todo) => {
  const message = `Leader，我正在处理「${task.title}」（核心/紧急）。如果还需插入其他任务，请帮我排定优先级。`
  try { navigator.clipboard.writeText(message); ElMessage.success('已复制话术') } catch (e) { ElMessage.info('请手动复制：'+message) }
}

const deleteNonCoreTasks = () => {
  for (let i = todoStore.tasks.length - 1; i >= 0; i--) {
    if (!todoStore.tasks[i].isCoreAligned && !todoStore.tasks[i].completed) todoStore.tasks.splice(i, 1)
  }
  ElMessage.success('已删除非核心任务')
}

const deleteTodo = async (task: Todo) => {
  if (task.originTaskId) {
    try {
      await taskStore.deleteTask(task.originTaskId)
      // 移除本地映射的 todos（可能存在多个关联项）
      for (let i = todoStore.tasks.length - 1; i >= 0; i--) {
        if (String(todoStore.tasks[i].originTaskId) === String(task.originTaskId)) todoStore.tasks.splice(i, 1)
      }
      ElMessage.success('已删除后端任务并同步')
    } catch (e) {
      console.error(e)
      ElMessage.error('删除失败，请稍后重试')
    }
  } else {
    todoStore.deleteTask(task.id)
    ElMessage.success('已从本地待办删除')
  }
  refreshGroups()
}

const addTemplate = (type: string) => {
  if (type === 'core') {
    newTask.title = '推进OKR最关键一步'
    newTask.isCoreAligned = true
    newTask.urgency = 'today'
    newTask.estimatedEffort = 'large'
    newTask.dependency = 'blocking'
    newTask.deadline = dayjs().format('YYYY-MM-DD')
  } else if (type === 'chore') {
    newTask.title = '处理行政杂务'
    newTask.isCoreAligned = false
    newTask.urgency = 'later'
    newTask.estimatedEffort = 'small'
    newTask.dependency = 'independent'
    newTask.deadline = null
  } else if (type === 'sync') {
    newTask.title = '同步项目进度给协作方'
    newTask.isCoreAligned = false
    newTask.urgency = 'week'
    newTask.estimatedEffort = 'medium'
    newTask.dependency = 'independent'
    newTask.deadline = dayjs().add(1,'day').format('YYYY-MM-DD')
  }
  addDialogVisible.value = true
}

const openAddDialog = () => { addDialogVisible.value = true }

const addTask = async () => {
  if (!newTask.title || !newTask.title.trim()) { ElMessage.error('请输入标题'); return }
  // 优先尝试在任务管理 store 创建任务，使其在 Tasks 页面可见
  try {
    const payload: any = {
      title: newTask.title,
      priority: newTask.isCoreAligned ? 'high' : 'medium',
      deadline: newTask.deadline || undefined,
      estimatedMinutes: newTask.estimatedEffort === 'large' ? 180 : (newTask.estimatedEffort === 'medium' ? 60 : 15),
      description: '',
    }
    const created = await taskStore.createTask(payload)
    if (created) {
      // 将后端任务导入到 todoStore（带 originTaskId）
      todoStore.importFromTasks([created])
      ElMessage.success('已在任务管理中创建并同步到待办')
    } else {
      // 回退到本地添加
      todoStore.addTask({
        title: newTask.title,
        deadline: newTask.deadline || null,
        isCoreAligned: !!newTask.isCoreAligned,
        urgency: newTask.urgency,
        estimatedEffort: newTask.estimatedEffort,
        dependency: newTask.dependency,
      })
      ElMessage.success('已添加到本地待办')
    }
  } catch (e) {
    // 若后端失败则回退到本地保存
    todoStore.addTask({
      title: newTask.title,
      deadline: newTask.deadline || null,
      isCoreAligned: !!newTask.isCoreAligned,
      urgency: newTask.urgency,
      estimatedEffort: newTask.estimatedEffort,
      dependency: newTask.dependency,
    })
    ElMessage.info('网络或后端异常，已本地保存待办项')
  }
  addDialogVisible.value = false
  newTask.title = ''
  refreshGroups()
}

// 同步功能已移除：由设计决定不在 Todos 页面暴露 "从任务管理同步" 按钮。

const fetchRecordsForRange = async (range: 'week'|'month'|'30d' = 'month') => {
  const days = range === 'week' ? 7 : 30
  const start = dayjs().subtract(days - 1, 'day').format('YYYY-MM-DD')
  const end = dayjs().format('YYYY-MM-DD')
  try { await timeRecordStore.fetchRecords(start, end) } catch (e) { console.warn('fetchRecordsForRange failed', e) }
}

const confirmComplete = async () => {
  try {
    if (!completeStartTime.value || !completeDuration.value) { ElMessage.error('请填写开始时间和持续时长（分钟）'); return }
    const originId = completingOriginId.value
    const localId = completingTodoLocalId.value
    if (originId) {
      await timeRecordStore.createRecord({
        taskId: String(originId),
        startTime: dayjs(completeStartTime.value).toISOString(),
        endTime: dayjs(completeStartTime.value).add(Number(completeDuration.value), 'minute').toISOString(),
        duration: Number(completeDuration.value),
        note: '手动记录',
      })
      await taskStore.updateTask(originId, { completed: true, estimatedTime: Number(completeDuration.value) / 60, actualMinutes: Number(completeDuration.value) })
      await fetchRecordsForRange('month')
      await taskStore.fetchTasks()
      ElMessage.success('已记录时间并完成任务')
      completeDialogVisible.value = false
      completingOriginId.value = null
      return
    }
    // 本地 todo 完成回落
    if (localId) {
      todoStore.markComplete(localId)
      completeDialogVisible.value = false
      ElMessage.success('已完成')
      return
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('完成操作失败')
  }
}

const onDragEnd = () => {
  const newOrder = [...highList.value, ...midList.value, ...lowList.value, ...completedList.value]
  const idSet = new Set(newOrder.map(t => String(t.id)))
  const remaining = todoStore.tasks.filter(t => !idSet.has(String(t.id)))
  const final = [...newOrder, ...remaining]
  todoStore.setTasksOrder(final)
  refreshGroups()
}

onMounted(async () => {
  // 尝试在页面加载时同步后端任务并映射为待办项
  try {
    await taskStore.fetchTasks()
    // 确保分类已加载，供编辑对话使用
    try { await taskStore.fetchCategories() } catch (e) { console.warn('fetchCategories failed', e) }
    todoStore.importFromTasks(taskStore.tasks as any)
  } catch (e) {
    console.warn('同步任务失败：', e)
  } finally {
    refreshGroups()
  }
})

// 当用户切换（登录/登出/注册）时，重新拉取任务并重建 todoStore，防止旧用户数据残留
watch(() => userStore.user?.id, async (newId, oldId) => {
  try {
    // 若用户变更为非空，则刷新后端数据并替换本地 todo
    if (newId) {
      await taskStore.fetchTasks()
      // 清空本地 todos 并导入后端任务
      if (typeof (todoStore as any).setTasksOrder === 'function') (todoStore as any).setTasksOrder([])
      todoStore.importFromTasks(taskStore.tasks as any)
    } else {
      // 用户登出：清空本地 todo 数据
      if (typeof (todoStore as any).setTasksOrder === 'function') (todoStore as any).setTasksOrder([])
    }
  } catch (e) { console.warn('[Todos] 用户切换时刷新任务失败', e) }
  refreshGroups()
}, { immediate: false })

onActivated(async () => {
  try {
    await taskStore.fetchTasks()
    todoStore.importFromTasks(taskStore.tasks as any)
  } catch (e) { console.warn('[Todos] onActivated fetchTasks failed', e) }
  refreshGroups()
})

// 编辑任务对话
const editDialogVisible = ref(false)
const fallbackDialogVisible = ref(false)
const editingOriginTaskId = ref<string|number|null>(null)
const editForm = reactive<any>({ title: '', categoryId: null, priority: 'medium', startTime: null, estimatedMinutes: 0, description: '' })
const editDurationSlider = ref<any>(null)

const descriptionPlaceholder = `📋 回答几个小问题，帮你自动打标签：
① 这件事和你的核心目标挂钩吗？（如有写“是”或“OKR”）
② 需要等谁做完什么才能开始？（如有写“等XX”）
③ 谁需要等着你做完这件事？（如有写“阻塞XX”）
示例：是；等产品出设计图；阻塞前端开发`

const openEditTask = (todo: Todo) => {
  console.debug('[Todos] openEditTask called with', todo)
  ElMessage.info('打开编辑：' + (todo?.title || ''))
  // 若为后端任务，取后端实体优先
    if (todo.originTaskId) {
    const originId = todo.originTaskId
    const backend = taskStore.tasks.find((t:any) => String(t.id) === String(originId))
    editingOriginTaskId.value = originId
    editForm.title = backend?.title || todo.title
    editForm.categoryId = backend?.categoryId ?? null
    editForm.priority = backend?.priority || todo.priority || 'medium'
    editForm.startTime = backend?.startTime || todo.startTime || todo.deadline || null
    editForm.estimatedMinutes = backend?.estimatedMinutes ?? (backend?.estimatedTime ? Math.round(Number(backend.estimatedTime)*60) : (todo.estimatedMinutes ?? (todo.estimatedEffort === 'large' ? 180 : (todo.estimatedEffort === 'medium' ? 60 : 15))))
    editForm.description = backend?.description || ''
  } else {
    editingOriginTaskId.value = null
    editForm.title = todo.title
    editForm.categoryId = null
    editForm.priority = todo.priority || 'medium'
    editForm.startTime = todo.startTime || todo.deadline || null
    editForm.estimatedMinutes = todo.estimatedMinutes ?? (todo.estimatedEffort === 'large' ? 180 : (todo.estimatedEffort === 'medium' ? 60 : 15))
    editForm.description = ''
  }
    editDialogVisible.value = true
  // ensure dialog visibility after next tick (HMR / draggable edge cases)
  nextTick(() => { 
    editDialogVisible.value = true
    console.debug('[Todos] requested dialog open; DOM wrappers:', document.querySelectorAll('.el-dialog__wrapper').length)
  })
}

// debug: watch dialog visible state and log DOM probes to help diagnose why dialog may not show
watch(editDialogVisible, (val) => {
  console.debug('[Todos] editDialogVisible changed ->', val)
  if (val) {
    setTimeout(() => {
      try {
        const wrappers = document.querySelectorAll('.el-dialog__wrapper')
        console.debug('[Todos] .el-dialog__wrapper count:', wrappers.length, wrappers)
        const dialogs = document.querySelectorAll('.el-dialog')
        console.debug('[Todos] .el-dialog count:', dialogs.length, dialogs)
        const overlays = document.querySelectorAll('.v-modal')
        console.debug('[Todos] .v-modal count:', overlays.length, overlays)
        // 如果 Element Plus 的 dialog 没有渲染（数量为 0），则显示后备模态以保证可编辑性
        if ((dialogs.length === 0 && wrappers.length === 0)) {
          console.warn('[Todos] Element Plus dialog not present; showing fallback modal')
          fallbackDialogVisible.value = true
        } else {
          fallbackDialogVisible.value = false
        }
      } catch (e) { console.warn('DOM probe failed', e) }
    }, 80)
  }
})

const saveEditedTask = async () => {
  if (!editForm.title) { ElMessage.error('请输入标题'); return }
  if (Number(editForm.estimatedMinutes) <= 0) { ElMessage.error('请填写预估时长（分钟）'); return }
  try {
    const payload: any = {
      title: editForm.title,
      priority: editForm.priority,
      startTime: editForm.startTime || undefined,
      estimatedMinutes: Number(editForm.estimatedMinutes),
      description: editForm.description,
    }
    // 如果选择了分类，则包含 categoryId
    if (editForm.categoryId != null) payload.categoryId = editForm.categoryId
    if (editingOriginTaskId.value) {
      await taskStore.updateTask(editingOriginTaskId.value, payload)
      // refresh and import
      await taskStore.fetchTasks()
      todoStore.importFromTasks(taskStore.tasks as any)
      ElMessage.success('已更新任务并同步到待办')
    } else {
      const created = await taskStore.createTask(payload)
      if (created) {
        todoStore.importFromTasks([created])
        ElMessage.success('已在任务管理创建并同步到待办')
      } else {
        // 回退本地
        todoStore.addTask({ title: editForm.title, deadline: editForm.startTime || null, isCoreAligned: false, urgency: 'later', estimatedEffort: editForm.estimatedMinutes >= 120 ? 'large' : (editForm.estimatedMinutes >= 30 ? 'medium' : 'small'), estimatedMinutes: Number(editForm.estimatedMinutes), dependency: 'independent' })
        ElMessage.info('后端未返回实体，已本地保存')
      }
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('保存失败')
  } finally {
    editDialogVisible.value = false
    fallbackDialogVisible.value = false
    await refreshGroups()
  }
}

</script>

<style scoped>
:root{
  --bg:#f7f9fc;
  --card:#ffffff;
  --muted:#909399;
  --accent:#409eff;
}
.todo-container{
  max-width:1100px;
  margin:24px auto;
  padding:18px;
  display:flex;
  flex-direction:column;
  gap:14px;
  font-family: -apple-system, "Helvetica Neue", "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
}
.okr-card{ border-radius:12px; overflow:hidden; background:linear-gradient(90deg, rgba(64,158,255,0.06), rgba(64,158,255,0.02)); box-shadow:0 6px 18px rgba(16,24,40,0.04); }
.okr-content{ display:flex; gap:14px; align-items:center; padding:14px; }
.okr-icon{ font-size:28px }
.okr-text h3{ margin:0; font-size:16px; font-weight:700; color:#2c3e50 }
.okr-text p{ margin:0; color:var(--muted); font-size:13px }

.mindset-bar{ display:flex; justify-content:space-between; gap:12px; padding:10px 14px; background:var(--bg); border-radius:10px; color:var(--muted) }
.mindset-bar .date{ font-weight:600 }
.mindset-bar .quote{ color:var(--muted); font-size:13px }

.action-bar{ display:flex; justify-content:space-between; align-items:center; gap:12px }
.action-bar .left{ display:flex; align-items:center; gap:8px }
.action-bar .right{ display:flex; align-items:center }

.main-columns{ display:flex; gap:20px; align-items:flex-start }
.todo-list{ flex:1 }
.right-sidebar{ width:300px; min-width:220px; background:var(--card); border-radius:12px; padding:14px; box-shadow:0 8px 20px rgba(20,20,20,0.04); position:sticky; top:24px }

.priority-group{ margin-bottom:14px }
.group-title{ display:flex; align-items:center; gap:10px; font-weight:700; font-size:15px; margin-bottom:8px }
.group-title::before{ content:''; display:inline-block; width:10px; height:10px; border-radius:3px; background:#f56c6c }
.medium-priority .group-title::before{ background:#f7b955 }
.procrastinate-group .group-title::before{ background:#909399 }
.group-desc{ font-size:13px; color:var(--muted); margin-bottom:8px }

.todo-item{ margin-bottom:10px; border-radius:10px; transition:box-shadow .18s ease, transform .08s ease }
.todo-item:hover{ box-shadow: 0 8px 18px rgba(16,24,40,0.06); transform:translateY(-2px) }

.celebration-toast{ position:fixed; right:24px; bottom:24px; background:linear-gradient(90deg,#67c23a,#409eff); color:#fff; padding:10px 14px; border-radius:10px; box-shadow:0 10px 30px rgba(0,0,0,0.12); z-index:2000 }

.quick-templates .sidebar-title, .mindset-sidebar .sidebar-title{ font-weight:700; margin-bottom:8px }
.quick-templates el-button{ margin-bottom:8px; width:100%; text-align:left }

@media (max-width:980px){ .main-columns{ flex-direction:column } .right-sidebar{ position:static; width:100% } }

.scoring-panel{ background:var(--card); padding:12px; border-radius:8px; border:1px solid rgba(16,24,40,0.04); }
.scoring-table{ width:100%; border-collapse:collapse; margin-top:8px }
.scoring-table th, .scoring-table td{ border:1px solid rgba(16,24,40,0.04); padding:8px; text-align:left; font-size:13px }
.scoring-formula{ margin-top:8px; color:var(--muted); font-size:13px }
.collapsed-summary{ font-size:13px; color:var(--muted); padding:8px }

/* OKR 说明美化样式 */
.okr-panel{ background: linear-gradient(180deg, rgba(64,158,255,0.04), rgba(64,158,255,0.01)); padding:14px; border-radius:12px; border:1px solid rgba(64,158,255,0.08); box-shadow:0 8px 20px rgba(16,24,40,0.03); color:#2c3e50; margin-top:8px }
.okr-header{ display:flex; gap:12px; align-items:center; margin-bottom:10px }
.okr-badge{ font-size:26px; width:48px; height:48px; display:flex; align-items:center; justify-content:center; background:rgba(64,158,255,0.09); border-radius:10px }
.okr-title-block h4{ margin:0; font-size:16px; font-weight:700 }
.okr-sub{ font-size:12px; color:var(--muted) }
.okr-body dl{ margin:0 }
.okr-body dt{ font-weight:700; margin-top:8px; font-size:13px }
.okr-body dd{ margin:6px 0 10px 0; color:var(--muted); font-size:13px }
.okr-tip{ margin-top:8px; color:var(--muted); font-size:13px }
.okr-metrics{ display:flex; gap:12px; margin-top:10px; flex-wrap:wrap; align-items:center }
.okr-metric{ font-size:13px; color:#2c3e50 }
.okr-pill{ padding:4px 10px; background:#f0f9eb; color:#67c23a; border-radius:999px; font-weight:700; margin-left:6px }

/* 右侧卡片与决策清单 */
.sidebar-card{ border-radius:10px; padding:12px; background:var(--card); box-shadow:0 10px 30px rgba(16,24,40,0.04); }
.quick-templates-card el-button{ display:block; margin-bottom:8px }
.mindset-card{ display:flex; flex-direction:column; gap:10px }
.decision-card{ background:linear-gradient(90deg, rgba(100,116,255,0.03), rgba(255,255,255,0)); padding:8px; border-radius:8px; border:1px solid rgba(64,158,255,0.06) }
.decision-body{ padding-top:8px }
.decision-list{ list-style:none; padding:0; margin:0; display:flex; flex-direction:column; gap:8px }
.decision-list li{ display:flex; gap:10px; align-items:flex-start }
.decision-list .num{ width:30px; height:30px; border-radius:50%; background:#eef6ff; color:#409eff; display:flex; align-items:center; justify-content:center; font-weight:700 }
.decision-list .content{ flex:1 }
.decision-list .note{ color:var(--muted); font-size:12px; margin-top:6px }

.quick-card .sidebar-title, .mindset-card .sidebar-title{ font-weight:700; margin-bottom:8px }

/* 后备模态样式 */
.fallback-overlay{ position:fixed; left:0; top:0; right:0; bottom:0; background:rgba(0,0,0,0.45); display:flex; align-items:center; justify-content:center; z-index:3000 }
.fallback-modal{ background:#fff; padding:18px; border-radius:8px; width:640px; max-width:95%; box-shadow:0 20px 50px rgba(0,0,0,0.35) }
.fallback-modal h3{ margin:0 0 12px 0 }

</style>
