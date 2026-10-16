package com.example.data.converter

import androidx.room.TypeConverter
import com.example.model.AgentRole

/**
 * Room Type Converters for Lists and Enums.
 */
class AgentConverters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(SEPARATOR) ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(SEPARATOR).filter { it.isNotBlank() }
    }

    @TypeConverter
    fun fromAgentRole(role: AgentRole?): String {
        return role?.name ?: AgentRole.SUPERVISOR.name
    }

    @TypeConverter
    fun toAgentRole(value: String?): AgentRole {
        return try {
            if (value != null) AgentRole.valueOf(value) else AgentRole.SUPERVISOR
        } catch (_: Exception) {
            AgentRole.SUPERVISOR
        }
    }

    companion object {
        private const val SEPARATOR = "|||__|||"
    }
}
