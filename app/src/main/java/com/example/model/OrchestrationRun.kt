package com.example.model

/**
 * Top-level Orchestration Plan Run tracking an end-to-end user goal execution.
 */
data class OrchestrationRun(
    val runId: String,
    val userGoal: String,
    val status: RunStatus = RunStatus.RUNNING,
    val tasks: List<AgentTask> = emptyList(),
    val activeAgents: Set<AgentRole> = emptySet(),
    val startTimeMs: Long = System.currentTimeMillis(),
    val endTimeMs: Long? = null,
    val finalSynthesisReport: String? = null,
    val remainingConstraints: List<String> = emptyList(),
    val totalTokensEstimated: Long = 0L,
    val failureReason: String? = null
) {
    val durationSeconds: Long
        get() {
            val end = endTimeMs ?: System.currentTimeMillis()
            return ((end - startTimeMs) / 1000).coerceAtLeast(0)
        }

    val progressPercent: Float
        get() {
            if (tasks.isEmpty()) return 0f
            val completed = tasks.count { it.status == TaskStatus.SUCCEEDED }
            return (completed.toFloat() / tasks.size).coerceIn(0f, 1f)
        }
}

enum class RunStatus(val titleAr: String, val isFinished: Boolean) {
    PLANNING("جاري التخطيط والتفكيك", false),
    RUNNING("جاري التنفيذ والتنسيق", false),
    WAITING_APPROVAL("بانتظار موافقة المستخدم", false),
    COMPLETED("اكتمل بنجاح وموثق", true),
    FAILED("فشل التنفيذ", true),
    CANCELLED("ملغي من قبل المستخدم", true)
}
