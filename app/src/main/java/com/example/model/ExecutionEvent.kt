package com.example.model

/**
 * System and security audit events for real-time observability.
 */
data class ExecutionEvent(
    val eventId: String,
    val runId: String,
    val taskId: String? = null,
    val agentRole: AgentRole? = null,
    val level: EventLevel = EventLevel.INFO,
    val messageAr: String,
    val messageEn: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class EventLevel(val titleAr: String) {
    INFO("معلومات"),
    AUDIT("تدقيق أمني"),
    WARN("تحذير"),
    ERROR("خطأ")
}

/**
 * Human-in-the-Loop Approval Request for sensitive / destructive tasks.
 */
data class ApprovalRequest(
    val requestId: String,
    val runId: String,
    val taskId: String,
    val title: String,
    val description: String,
    val proposedAction: String,
    val riskLevel: RiskLevel,
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val reviewerNotes: String? = null
)

enum class RiskLevel(val titleAr: String) {
    LOW("منخفض"),
    MEDIUM("متوسط"),
    HIGH("عالي"),
    CRITICAL("حرج")
}

enum class ApprovalStatus(val titleAr: String) {
    PENDING("قيد انتظار المراجعة"),
    APPROVED("تمت الموافقة والتفويض"),
    REJECTED("تم الرفض والإلغاء")
}
