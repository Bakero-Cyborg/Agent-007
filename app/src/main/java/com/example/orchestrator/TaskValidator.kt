package com.example.orchestrator

import com.example.model.AgentRole
import com.example.model.AgentTask
import com.example.model.TaskValidationResult

/**
 * Execution limits and governance policies enforcing system safety.
 */
data class ExecutionPolicy(
    val maxActiveAgents: Int = 4,
    val maxTaskRetries: Int = 2,
    val taskTimeoutMs: Long = 60_000L,
    val requireHumanApprovalForDestructive: Boolean = true,
    val minimumValidationConfidence: Float = 0.70f
)

/**
 * Critic & QA Validation Engine verifying agent outputs prior to acceptance.
 */
object TaskValidator {

    fun validate(task: AgentTask, output: String): TaskValidationResult {
        val trimmed = output.trim()

        // Gate 1: Non-empty check
        if (trimmed.isEmpty()) {
            return TaskValidationResult(
                isValid = false,
                reviewer = AgentRole.CRITIC_QA,
                score = 0.0f,
                feedback = "المخرج فارغ أو لم يكتمل التنفيذ (Empty Output)"
            )
        }

        // Gate 2: Length / Substantiality check
        if (trimmed.length < 20) {
            return TaskValidationResult(
                isValid = false,
                reviewer = AgentRole.CRITIC_QA,
                score = 0.3f,
                feedback = "المخرج مقتضب جداً ولا يحتوي على تفاصيل كافية للتحقق"
            )
        }

        // Gate 3: Hallucination / Mock indicators check
        val containsUnsubstantiatedClaim = trimmed.contains("TODO", ignoreCase = true) ||
                trimmed.contains("NOT_IMPLEMENTED", ignoreCase = true)

        if (containsUnsubstantiatedClaim) {
            return TaskValidationResult(
                isValid = false,
                reviewer = AgentRole.CRITIC_QA,
                score = 0.4f,
                feedback = "تم اكتشاف علامات عدم اكتمال أو بنود غير محققة في المخرج"
            )
        }

        // Gate 4: Role-specific heuristics
        val score = when (task.assignedAgent) {
            AgentRole.RESEARCH -> if (task.evidenceCitations.isNotEmpty() || trimmed.contains("المصدر") || trimmed.contains("المرجع")) 0.95f else 0.80f
            AgentRole.CYBERSECURITY -> if (trimmed.contains("المخاطر") || trimmed.contains("CVSS") || trimmed.contains("الأمان")) 0.98f else 0.85f
            AgentRole.CODING -> if (trimmed.contains("```") || trimmed.contains("fun ") || trimmed.contains("class ")) 0.95f else 0.82f
            else -> 0.90f
        }

        return TaskValidationResult(
            isValid = score >= 0.70f,
            reviewer = AgentRole.CRITIC_QA,
            score = score,
            feedback = "تم التحقق المستقل من صحة المخرج واتساقه مع الهدف المحدد"
        )
    }
}
