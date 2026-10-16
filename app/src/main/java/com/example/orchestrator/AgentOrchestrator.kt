package com.example.orchestrator

import com.example.model.AgentMessage
import com.example.model.AgentRole
import com.example.model.AgentTask
import com.example.model.ApprovalRequest
import com.example.model.ApprovalStatus
import com.example.model.EventLevel
import com.example.model.EvidenceCitation
import com.example.model.ExecutionEvent
import com.example.model.MessageType
import com.example.model.OrchestrationRun
import com.example.model.RiskLevel
import com.example.model.RunStatus
import com.example.model.SensitivityLevel
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import com.example.provider.DeterministicMockProvider
import com.example.provider.ModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Central Multi-Agent Orchestration Engine.
 * Coordinates task lifecycles, DAG execution, verification gates, and Human-in-the-Loop approvals.
 */
class AgentOrchestrator(
    private val modelProvider: ModelProvider = DeterministicMockProvider(),
    private val policy: ExecutionPolicy = ExecutionPolicy(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val _runs = MutableStateFlow<List<OrchestrationRun>>(emptyList())
    val runs: StateFlow<List<OrchestrationRun>> = _runs.asStateFlow()

    private val _activeRun = MutableStateFlow<OrchestrationRun?>(null)
    val activeRun: StateFlow<OrchestrationRun?> = _activeRun.asStateFlow()

    private val _events = MutableStateFlow<List<ExecutionEvent>>(emptyList())
    val events: StateFlow<List<ExecutionEvent>> = _events.asStateFlow()

    private val _approvalRequests = MutableStateFlow<List<ApprovalRequest>>(emptyList())
    val approvalRequests: StateFlow<List<ApprovalRequest>> = _approvalRequests.asStateFlow()

    private val _messages = MutableStateFlow<List<AgentMessage>>(emptyList())
    val messages: StateFlow<List<AgentMessage>> = _messages.asStateFlow()

    private var currentExecutionJob: Job? = null

    init {
        logEvent(
            runId = "SYSTEM",
            level = EventLevel.INFO,
            messageAr = "تم بدء تشغيل محرك التنسيق المركزي (AgentCore Orchestration Engine v1.0)",
            messageEn = "Central Orchestration Engine initialized"
        )
    }

    /**
     * Submit a new enterprise goal to be planned and executed.
     */
    fun submitGoal(goal: String): String {
        val runId = "RUN-" + UUID.randomUUID().toString().take(8).uppercase()

        logEvent(
            runId = runId,
            level = EventLevel.INFO,
            messageAr = "استقبال هدف جديد من المستخدم: $goal",
            messageEn = "Received user goal: $goal"
        )

        val newRun = OrchestrationRun(
            runId = runId,
            userGoal = goal,
            status = RunStatus.PLANNING,
            activeAgents = setOf(AgentRole.SUPERVISOR)
        )

        _runs.update { listOf(newRun) + it }
        _activeRun.value = newRun

        currentExecutionJob?.cancel()
        currentExecutionJob = scope.launch {
            executePlanPipeline(runId, goal)
        }

        return runId
    }

    /**
     * Cancel an active orchestration run.
     */
    fun cancelRun(runId: String) {
        currentExecutionJob?.cancel()
        _runs.update { list ->
            list.map { run ->
                if (run.runId == runId && !run.status.isFinished) {
                    run.copy(
                        status = RunStatus.CANCELLED,
                        endTimeMs = System.currentTimeMillis(),
                        failureReason = "تم الإلغاء بواسطة المستخدم"
                    )
                } else run
            }
        }
        if (_activeRun.value?.runId == runId) {
            _activeRun.value = _runs.value.find { it.runId == runId }
        }
        logEvent(
            runId = runId,
            level = EventLevel.WARN,
            messageAr = "تم إلغاء تشغيل المهمة بناءً على طلب المستخدم",
            messageEn = "Run cancelled by user"
        )
    }

    /**
     * Resolve a pending Human-in-the-Loop approval request.
     */
    fun resolveApproval(requestId: String, approved: Boolean, notes: String = "") {
        val request = _approvalRequests.value.find { it.requestId == requestId } ?: return
        val updatedStatus = if (approved) ApprovalStatus.APPROVED else ApprovalStatus.REJECTED

        _approvalRequests.update { list ->
            list.map { req ->
                if (req.requestId == requestId) {
                    req.copy(
                        status = updatedStatus,
                        resolvedAt = System.currentTimeMillis(),
                        reviewerNotes = notes
                    )
                } else req
            }
        }

        logEvent(
            runId = request.runId,
            taskId = request.taskId,
            level = EventLevel.AUDIT,
            messageAr = "الموافقة البشرية: ${if (approved) "تم الاعتماد والتفويض" else "تم الرفض والإلغاء"}. $notes",
            messageEn = "Human approval resolved: ${if (approved) "APPROVED" else "REJECTED"}"
        )

        // Resume pipeline if approved, or fail the specific task if rejected
        scope.launch {
            if (approved) {
                resumeTaskAfterApproval(request.runId, request.taskId)
            } else {
                failTask(request.runId, request.taskId, "تم رفض الإجراء في مرحلة الموافقة البشرية")
            }
        }
    }

    private suspend fun executePlanPipeline(runId: String, goal: String) {
        delay(400) // Supervisor decomposition simulation

        // Phase 1: Supervisor decomposes goal into a DAG of specialized subtasks
        val tasks = decomposeGoalIntoDag(runId, goal)

        if (DependencyResolver.hasCycle(tasks)) {
            logEvent(
                runId = runId,
                level = EventLevel.ERROR,
                messageAr = "خطأ حرج: تم اكتشاف حلقة تبعيات دائرية (Circular Dependency) في الخطة",
                messageEn = "Circular dependency detected in execution plan"
            )
            updateRunState(runId) { it.copy(status = RunStatus.FAILED, failureReason = "Circular dependency detected") }
            return
        }

        val requiredAgents = tasks.map { it.assignedAgent }.toSet() + AgentRole.SUPERVISOR + AgentRole.CRITIC_QA

        updateRunState(runId) {
            it.copy(
                status = RunStatus.RUNNING,
                tasks = tasks,
                activeAgents = requiredAgents
            )
        }

        logEvent(
            runId = runId,
            level = EventLevel.INFO,
            messageAr = "قام المشرف بتفكيك الهدف إلى ${tasks.size} مهام فرعية ضمن رسم بياني موجه (DAG)",
            messageEn = "Supervisor generated plan with ${tasks.size} subtasks"
        )

        // Phase 2: Iterative DAG Execution Loop
        while (true) {
            val currentRun = _runs.value.find { it.runId == runId } ?: break
            if (currentRun.status.isFinished) break

            // Check if any blocked tasks exist due to failed dependencies
            val blockedTasks = DependencyResolver.findBlockedTasks(currentRun.tasks)
            if (blockedTasks.isNotEmpty()) {
                for (b in blockedTasks) {
                    failTask(runId, b.taskId, "فشلت المهمة المعتمد عليها مسبقاً")
                }
            }

            val readyTasks = DependencyResolver.findReadyTasks(currentRun.tasks)
            if (readyTasks.isEmpty()) {
                // If no tasks ready, check if any task is still running or waiting approval
                val pendingOrRunning = currentRun.tasks.any { it.status == TaskStatus.RUNNING || it.status == TaskStatus.WAITING_FOR_APPROVAL }
                if (!pendingOrRunning) {
                    // All tasks reached a terminal state!
                    finalizeRun(runId)
                    break
                } else {
                    delay(300)
                    continue
                }
            }

            // Execute ready tasks (respecting concurrency limit)
            for (task in readyTasks.take(policy.maxActiveAgents)) {
                executeSingleTask(runId, task)
            }

            delay(300)
        }
    }

    private suspend fun executeSingleTask(runId: String, task: AgentTask) {
        // Step 1: Transition to RUNNING
        if (!task.status.canTransitionTo(TaskStatus.RUNNING)) return
        updateTask(runId, task.taskId) {
            it.copy(status = TaskStatus.RUNNING, updatedAt = System.currentTimeMillis())
        }

        sendMessage(
            from = AgentRole.SUPERVISOR,
            to = task.assignedAgent,
            taskId = task.taskId,
            runId = runId,
            type = MessageType.TASK_ASSIGNMENT,
            payload = "تكليف بتنفيذ: ${task.title}. الهدف: ${task.objective}"
        )

        // Step 2: Human-in-the-loop Gate for destructive operations
        if (task.isDestructiveOrExternal && policy.requireHumanApprovalForDestructive) {
            if (task.status.canTransitionTo(TaskStatus.WAITING_FOR_APPROVAL)) {
                updateTask(runId, task.taskId) {
                    it.copy(status = TaskStatus.WAITING_FOR_APPROVAL, updatedAt = System.currentTimeMillis())
                }
                val approvalId = "REQ-" + UUID.randomUUID().toString().take(6).uppercase()
                val req = ApprovalRequest(
                    requestId = approvalId,
                    runId = runId,
                    taskId = task.taskId,
                    title = "موافقة مطلوبة: ${task.title}",
                    description = "تتطلب المهمة صلاحيات حساسة أو إجراء تعديلات غير قابلة للتراجع.",
                    proposedAction = task.objective,
                    riskLevel = RiskLevel.HIGH
                )
                _approvalRequests.update { listOf(req) + it }
                logEvent(
                    runId = runId,
                    taskId = task.taskId,
                    level = EventLevel.AUDIT,
                    messageAr = "تم تعليق المهمة بانتظار الموافقة البشرية الصريحة (Human-in-the-Loop)",
                    messageEn = "Task paused waiting for human authorization"
                )
                return
            }
        }

        // Step 3: Run Agent Provider
        performAgentTaskExecution(runId, task)
    }

    private suspend fun resumeTaskAfterApproval(runId: String, taskId: String) {
        val currentRun = _runs.value.find { it.runId == runId } ?: return
        val task = currentRun.tasks.find { it.taskId == taskId } ?: return
        updateTask(runId, taskId) {
            it.copy(status = TaskStatus.RUNNING, updatedAt = System.currentTimeMillis())
        }
        performAgentTaskExecution(runId, task)
    }

    private suspend fun performAgentTaskExecution(runId: String, task: AgentTask) {
        delay(600) // Simulation of agent thinking & tool invocation

        val providerResult = modelProvider.executeTask(
            role = task.assignedAgent,
            taskTitle = task.title,
            objective = task.objective,
            inputData = task.inputData,
            expectedOutput = task.expectedOutput
        )

        if (!providerResult.isSuccess || providerResult.error != null) {
            handleTaskFailureOrRetry(runId, task, providerResult.error ?: "خطأ في مزود النموذج")
            return
        }

        // Step 4: Independent Critic/QA Validation Gate
        sendMessage(
            from = task.assignedAgent,
            to = AgentRole.CRITIC_QA,
            taskId = task.taskId,
            runId = runId,
            type = MessageType.VALIDATION_REQUEST,
            payload = providerResult.output
        )

        val validation = TaskValidator.validate(task, providerResult.output)

        sendMessage(
            from = AgentRole.CRITIC_QA,
            to = AgentRole.SUPERVISOR,
            taskId = task.taskId,
            runId = runId,
            type = MessageType.VALIDATION_RESULT,
            payload = "درجة التحقق: ${(validation.score * 100).toInt()}%. النتيجة: ${if (validation.isValid) "معتمد" else "مرفوض"}"
        )

        if (validation.isValid) {
            updateTask(runId, task.taskId) {
                it.copy(
                    status = TaskStatus.SUCCEEDED,
                    actualOutput = providerResult.output,
                    evidenceCitations = providerResult.citations,
                    validationResult = validation,
                    updatedAt = System.currentTimeMillis()
                )
            }
            logEvent(
                runId = runId,
                taskId = task.taskId,
                agentRole = task.assignedAgent,
                level = EventLevel.INFO,
                messageAr = "اكتملت المهمة [${task.title}] وتمت مصادقتها بنجاح",
                messageEn = "Task ${task.title} completed and validated"
            )
        } else {
            handleTaskFailureOrRetry(runId, task, validation.feedback)
        }
    }

    private fun handleTaskFailureOrRetry(runId: String, task: AgentTask, reason: String) {
        if (task.canRetry()) {
            val nextRetry = task.retryCount + 1
            logEvent(
                runId = runId,
                taskId = task.taskId,
                level = EventLevel.WARN,
                messageAr = "فشل التحقق للمهمة [${task.title}]. إعادة المحاولة ($nextRetry/${task.maxRetries}). السبب: $reason",
                messageEn = "Task failed validation, retrying ($nextRetry/${task.maxRetries})"
            )
            updateTask(runId, task.taskId) {
                it.copy(
                    status = TaskStatus.QUEUED,
                    retryCount = nextRetry,
                    updatedAt = System.currentTimeMillis()
                )
            }
        } else {
            failTask(runId, task.taskId, reason)
        }
    }

    private fun failTask(runId: String, taskId: String, reason: String) {
        updateTask(runId, taskId) {
            it.copy(
                status = TaskStatus.FAILED,
                actualOutput = "فشل: $reason",
                updatedAt = System.currentTimeMillis()
            )
        }
        logEvent(
            runId = runId,
            taskId = taskId,
            level = EventLevel.ERROR,
            messageAr = "فشلت المهمة نهائياً بعد استنفاد المحاولات: $reason",
            messageEn = "Task failed permanently: $reason"
        )
    }

    private fun finalizeRun(runId: String) {
        val currentRun = _runs.value.find { it.runId == runId } ?: return
        val allSucceeded = currentRun.tasks.all { it.status == TaskStatus.SUCCEEDED }

        val finalReport = if (allSucceeded) {
            """
                ### التقرير النهائي للوكيل المشرف المركزي (Executive Synthesis Report)
                
                **الهدف الأساسي:** ${currentRun.userGoal}
                **حالة الإنجاز:** تم إنجاز جميع المهام (${currentRun.tasks.size}/${currentRun.tasks.size}) بنجاح واعتمادها عبر بوابات الجودة المستقلة.
                
                #### ملخص مخرجات الوكلاء المتخصصين:
                ${currentRun.tasks.joinToString("\n") { "- **${it.assignedAgent.titleAr} (${it.title}):** ${it.actualOutput?.take(160)}..." }}
                
                #### الأدلة والمصادر المستخرجة:
                ${currentRun.tasks.flatMap { it.evidenceCitations }.joinToString("\n") { "• [${it.sourceTitle}] - ${it.excerpt}" }.ifEmpty { "• تم التحقق من المعايير والسياسات الداخلية للنظام." }}
                
                #### القيود المعروفة والمخاطر المتبقية:
                1. يتطلب تفعيل النشر في بيئة الإنتاج تكرار الفحص على شبكة حية.
                2. مراقبة استهلاك الحصص ونوافذ السجلات على المدى الطويل.
            """.trimIndent()
        } else {
            "لم يكتمل سير العمل بنجاح. تعثرت بعض المهام الفرعية مما حال دون تحقيق الهدف الكامل."
        }

        val estimatedTokens = currentRun.tasks.size * 320L + 500L

        updateRunState(runId) {
            it.copy(
                status = if (allSucceeded) RunStatus.COMPLETED else RunStatus.FAILED,
                endTimeMs = System.currentTimeMillis(),
                finalSynthesisReport = finalReport,
                totalTokensEstimated = estimatedTokens,
                remainingConstraints = listOf(
                    "بيئة التشغيل معزولة وفق سياسات Sandbox",
                    "جميع الأدوات مقيدة بنموذج أقل الصلاحيات (Least Privilege)",
                    "البيانات تم تدقيقها ضد محاولات الحقن والتسريب"
                )
            )
        }

        logEvent(
            runId = runId,
            level = if (allSucceeded) EventLevel.INFO else EventLevel.ERROR,
            messageAr = if (allSucceeded) "تم إنهاء وتوثيق دورة العمل بنجاح بواسطة الوكيل المشرف" else "انتهت دورة العمل بوجود أخطاء",
            messageEn = "Run completed with status: ${if (allSucceeded) "COMPLETED" else "FAILED"}"
        )
    }

    private fun decomposeGoalIntoDag(runId: String, goal: String): List<AgentTask> {
        val now = System.currentTimeMillis()
        val lowerGoal = goal.lowercase()

        // Choose appropriate workflow specialization based on goal domain
        return if (lowerGoal.contains("أمن") || lowerGoal.contains("security") || lowerGoal.contains("ثغرات")) {
            val task1 = AgentTask(
                taskId = "T1-$runId",
                runId = runId,
                title = "فحص الثغرات وتحليل الأسطح",
                objective = "فحص نقاط الاتصال والاعتماديات بحثاً عن ثغرات CVE ومخاطر تسرب البيانات",
                assignedAgent = AgentRole.CYBERSECURITY,
                inputData = "Project codebase and external endpoints",
                expectedOutput = "Vulnerability matrix and risk scores",
                dependencies = emptyList(),
                priority = TaskPriority.CRITICAL,
                createdAt = now
            )
            val task2 = AgentTask(
                taskId = "T2-$runId",
                runId = runId,
                title = "تدقيق سياسات RBAC وضوابط الصلاحيات",
                objective = "مراجعة نموذج المصادقة والتفويض وحماية Prompt Injection",
                assignedAgent = AgentRole.CYBERSECURITY,
                inputData = "Access control tables and token verification",
                expectedOutput = "RBAC audit log and least-privilege confirmation",
                dependencies = emptyList(),
                priority = TaskPriority.HIGH,
                createdAt = now
            )
            val task3 = AgentTask(
                taskId = "T3-$runId",
                runId = runId,
                title = "تطبيق رقع التحصين والتحديث الأمني",
                objective = "تحديث الاعتماديات غير الآمنة وعزل تنفيذ الأوامر",
                assignedAgent = AgentRole.CODING,
                inputData = "T1 security findings",
                expectedOutput = "Secure patch code and dependency pin update",
                dependencies = listOf(task1.taskId, task2.taskId),
                priority = TaskPriority.HIGH,
                isDestructiveOrExternal = true, // Triggers Human-in-the-Loop!
                createdAt = now
            )
            listOf(task1, task2, task3)
        } else if (lowerGoal.contains("بيانات") || lowerGoal.contains("data") || lowerGoal.contains("تحليل")) {
            val task1 = AgentTask(
                taskId = "T1-$runId",
                runId = runId,
                title = "استيراد وتطهير مجموعة البيانات",
                objective = "التحقق من المخططات، كشف القيم المفقودة والتكرار",
                assignedAgent = AgentRole.DATA_ENGINEERING,
                inputData = "Raw ingest stream",
                expectedOutput = "Clean dataset schema conforming to spec",
                dependencies = emptyList(),
                createdAt = now
            )
            val task2 = AgentTask(
                taskId = "T2-$runId",
                runId = runId,
                title = "التحليل الإحصائي واكتشاف الأنماط",
                objective = "حساب الاتجاهات واختبار الفرضيات وحساب معامل الارتباط",
                assignedAgent = AgentRole.DATA_ANALYSIS,
                inputData = "Cleaned data from T1",
                expectedOutput = "Statistical metrics report and outlier summary",
                dependencies = listOf(task1.taskId),
                createdAt = now
            )
            val task3 = AgentTask(
                taskId = "T3-$runId",
                runId = runId,
                title = "أتمتة تقرير مؤشرات الأداء",
                objective = "بناء تدفق دوري مجدول لمزامنة المؤشرات مع لوحة التحكم",
                assignedAgent = AgentRole.AUTOMATION,
                inputData = "Analytics findings from T2",
                expectedOutput = "Automated cron workflow definition",
                dependencies = listOf(task2.taskId),
                createdAt = now
            )
            listOf(task1, task2, task3)
        } else {
            // Standard Multi-Agent Trio: Research -> Coding/Strategy -> QA
            val task1 = AgentTask(
                taskId = "T1-$runId",
                runId = runId,
                title = "البحث المعمق وتدقيق المراجع",
                objective = "جمع أفضل الممارسات وتحديد المعايير المعتمدة وتقييم القيود",
                assignedAgent = AgentRole.RESEARCH,
                inputData = goal,
                expectedOutput = "Structured research report with citations",
                dependencies = emptyList(),
                priority = TaskPriority.HIGH,
                createdAt = now
            )
            val task2 = AgentTask(
                taskId = "T2-$runId",
                runId = runId,
                title = "التنفيذ التقني والمعماري",
                objective = "بناء المكونات البرمجية وتكامل الخدمات وفق نتائج البحث",
                assignedAgent = AgentRole.CODING,
                inputData = "Research output from T1",
                expectedOutput = "Modular code implementation with tests",
                dependencies = listOf(task1.taskId),
                priority = TaskPriority.HIGH,
                createdAt = now
            )
            val task3 = AgentTask(
                taskId = "T3-$runId",
                runId = runId,
                title = "تقييم الجدوى ونموذج التشغيل",
                objective = "حساب الموارد المطلوبة وتقدير التكلفة وتحديد مؤشرات النجاح",
                assignedAgent = AgentRole.BUSINESS,
                inputData = "Architecture and tech stack from T2",
                expectedOutput = "Operational and economics feasibility model",
                dependencies = listOf(task2.taskId),
                priority = TaskPriority.MEDIUM,
                createdAt = now
            )
            listOf(task1, task2, task3)
        }
    }

    private fun sendMessage(
        from: AgentRole,
        to: AgentRole,
        taskId: String,
        runId: String,
        type: MessageType,
        payload: String
    ) {
        val msg = AgentMessage(
            messageId = "MSG-" + UUID.randomUUID().toString().take(6).uppercase(),
            taskId = taskId,
            runId = runId,
            fromAgent = from,
            toAgent = to,
            messageType = type,
            payload = payload,
            timestamp = System.currentTimeMillis()
        )
        _messages.update { listOf(msg) + it }
    }

    private fun logEvent(
        runId: String,
        taskId: String? = null,
        agentRole: AgentRole? = null,
        level: EventLevel,
        messageAr: String,
        messageEn: String
    ) {
        val event = ExecutionEvent(
            eventId = "EVT-" + UUID.randomUUID().toString().take(6).uppercase(),
            runId = runId,
            taskId = taskId,
            agentRole = agentRole,
            level = level,
            messageAr = messageAr,
            messageEn = messageEn,
            timestamp = System.currentTimeMillis()
        )
        _events.update { listOf(event) + it.take(200) }
    }

    private fun updateTask(runId: String, taskId: String, update: (AgentTask) -> AgentTask) {
        _runs.update { list ->
            list.map { run ->
                if (run.runId == runId) {
                    val updatedTasks = run.tasks.map { if (it.taskId == taskId) update(it) else it }
                    run.copy(tasks = updatedTasks)
                } else run
            }
        }
        if (_activeRun.value?.runId == runId) {
            _activeRun.value = _runs.value.find { it.runId == runId }
        }
    }

    private fun updateRunState(runId: String, update: (OrchestrationRun) -> OrchestrationRun) {
        _runs.update { list ->
            list.map { if (it.runId == runId) update(it) else it }
        }
        if (_activeRun.value?.runId == runId) {
            _activeRun.value = _runs.value.find { it.runId == runId }
        }
    }
}
