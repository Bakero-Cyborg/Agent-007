package com.example.provider

import com.example.model.AgentRole
import com.example.model.EvidenceCitation

/**
 * Common abstraction for Model and Execution Providers.
 */
interface ModelProvider {
    val name: String
    suspend fun executeTask(
        role: AgentRole,
        taskTitle: String,
        objective: String,
        inputData: String,
        expectedOutput: String
    ): ProviderResult
}

data class ProviderResult(
    val output: String,
    val citations: List<EvidenceCitation> = emptyList(),
    val tokensUsed: Long = 0L,
    val isSuccess: Boolean = true,
    val error: String? = null
)
