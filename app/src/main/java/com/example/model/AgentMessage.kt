package com.example.model

/**
 * Standardized Inter-Agent Communication Protocol message.
 */
data class AgentMessage(
    val messageId: String,
    val taskId: String,
    val runId: String,
    val fromAgent: AgentRole,
    val toAgent: AgentRole,
    val messageType: MessageType,
    val payload: String,
    val citations: List<EvidenceCitation> = emptyList(),
    val validationStatus: String? = null,
    val sensitivityLevel: SensitivityLevel = SensitivityLevel.INTERNAL,
    val errors: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageType(val titleAr: String) {
    TASK_ASSIGNMENT("تكليف مهمة"),
    TASK_RESULT("تسليم نتيجة"),
    VALIDATION_REQUEST("طلب تدقيق واعتماد"),
    VALIDATION_RESULT("نتيجة التدقيق"),
    APPROVAL_REQUEST("طلب موافقة بشرية"),
    ERROR_REPORT("تقرير خطأ"),
    STATUS_UPDATE("تحديث الحالة")
}

enum class SensitivityLevel(val titleAr: String) {
    PUBLIC("عام"),
    INTERNAL("داخلي"),
    CONFIDENTIAL("سري"),
    RESTRICTED("مقيد بحاجة لموافقة")
}
