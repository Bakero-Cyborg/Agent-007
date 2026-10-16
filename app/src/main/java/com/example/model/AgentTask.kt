package com.example.model

/**
 * Core Task entity representing a discrete executable unit in the Orchestration DAG.
 */
data class AgentTask(
    val taskId: String,
    val runId: String,
    val parentTaskId: String? = null,
    val title: String,
    val objective: String,
    val assignedAgent: AgentRole,
    val inputData: String,
    val expectedOutput: String,
    val dependencies: List<String> = emptyList(),
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val deadlineMs: Long = 60_000L,
    val retryCount: Int = 0,
    val maxRetries: Int = 2,
    val validationResult: TaskValidationResult? = null,
    val actualOutput: String? = null,
    val evidenceCitations: List<EvidenceCitation> = emptyList(),
    val isDestructiveOrExternal: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val logs: List<String> = emptyList()
) {
    val isReadyToExecute: Boolean
        get() = status == TaskStatus.PENDING || status == TaskStatus.QUEUED

    fun canRetry(): Boolean = retryCount < maxRetries
}

data class TaskValidationResult(
    val isValid: Boolean,
    val reviewer: AgentRole = AgentRole.CRITIC_QA,
    val score: Float, // 0.0 to 1.0
    val feedback: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class EvidenceCitation(
    val sourceTitle: String,
    val uriOrRef: String,
    val excerpt: String,
    val confidence: Float = 0.95f
)
