package com.example.model

/**
 * Task Status state machine with allowed state transitions.
 */
enum class TaskStatus(
    val titleAr: String,
    val isTerminal: Boolean
) {
    PENDING("قيد الانتظار", isTerminal = false),
    QUEUED("في قائمة الانتظار", isTerminal = false),
    RUNNING("قيد التنفيذ", isTerminal = false),
    WAITING_FOR_APPROVAL("بانتظار الموافقة البشرية", isTerminal = false),
    SUCCEEDED("تم بنجاح", isTerminal = true),
    FAILED("فشل التنفيذ", isTerminal = true),
    CANCELLED("ملغي", isTerminal = true),
    TIMED_OUT("انتهت المهلة", isTerminal = true);

    /**
     * Enforce strict, valid state transitions to guarantee consistency.
     */
    fun canTransitionTo(next: TaskStatus): Boolean {
        if (this == next) return true
        return when (this) {
            PENDING -> next in setOf(QUEUED, RUNNING, CANCELLED)
            QUEUED -> next in setOf(RUNNING, CANCELLED)
            RUNNING -> next in setOf(WAITING_FOR_APPROVAL, SUCCEEDED, FAILED, TIMED_OUT, CANCELLED)
            WAITING_FOR_APPROVAL -> next in setOf(RUNNING, SUCCEEDED, CANCELLED, FAILED)
            SUCCEEDED, FAILED, CANCELLED, TIMED_OUT -> false // Terminal states cannot transition further
        }
    }
}

enum class TaskPriority(val titleAr: String, val level: Int) {
    LOW("منخفضة", 1),
    MEDIUM("متوسطة", 2),
    HIGH("عالية", 3),
    CRITICAL("حرجة", 4)
}
