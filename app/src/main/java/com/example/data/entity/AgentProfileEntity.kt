package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.AgentRole

/**
 * Room Entity representing an AI Agent Profile.
 * Stores role, capabilities, operational parameters, and system prompt configuration.
 */
@Entity(tableName = "agent_profiles")
data class AgentProfileEntity(
    @PrimaryKey
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val role: AgentRole,
    val descriptionAr: String,
    val descriptionEn: String,
    val systemPrompt: String,
    val capabilities: List<String>,
    val temperature: Float = 0.2f,
    val topP: Float = 0.95f,
    val maxOutputTokens: Int = 2048,
    val modelProvider: String = "gemini-2.0-flash",
    val isBuiltIn: Boolean = true,
    val isEnabled: Boolean = true,
    val requiresHumanApproval: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
