package com.example.data.repository

import com.example.data.DefaultAgentProfiles
import com.example.data.dao.AgentProfileDao
import com.example.data.entity.AgentProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository layer abstracting Agent Profile persistence from ViewModel and UI.
 */
class AgentProfileRepository(
    private val agentProfileDao: AgentProfileDao
) {

    val allProfiles: Flow<List<AgentProfileEntity>> = agentProfileDao.getAllProfiles()
    val enabledProfiles: Flow<List<AgentProfileEntity>> = agentProfileDao.getEnabledProfiles()

    fun getProfileById(id: String): Flow<AgentProfileEntity?> {
        return agentProfileDao.getProfileById(id)
    }

    suspend fun getProfileByIdSync(id: String): AgentProfileEntity? {
        return agentProfileDao.getProfileByIdSync(id)
    }

    suspend fun getProfileByRoleSync(roleName: String): AgentProfileEntity? {
        return agentProfileDao.getProfileByRoleSync(roleName)
    }

    fun searchProfiles(query: String): Flow<List<AgentProfileEntity>> {
        return agentProfileDao.searchProfiles(query)
    }

    suspend fun saveProfile(profile: AgentProfileEntity) {
        agentProfileDao.insertProfile(profile)
    }

    suspend fun updateSystemPrompt(id: String, newPrompt: String) {
        agentProfileDao.updateSystemPrompt(id, newPrompt)
    }

    suspend fun setAgentEnabled(id: String, isEnabled: Boolean) {
        agentProfileDao.setAgentEnabled(id, isEnabled)
    }

    suspend fun updateModelConfig(id: String, temperature: Float, maxTokens: Int) {
        agentProfileDao.updateModelConfig(id, temperature, maxTokens)
    }

    suspend fun deleteProfile(profile: AgentProfileEntity) {
        if (!profile.isBuiltIn) {
            agentProfileDao.deleteProfile(profile)
        }
    }

    suspend fun deleteProfileById(id: String) {
        val existing = agentProfileDao.getProfileByIdSync(id)
        if (existing != null && !existing.isBuiltIn) {
            agentProfileDao.deleteProfileById(id)
        }
    }

    suspend fun ensureDefaultProfilesLoaded() {
        if (agentProfileDao.getProfilesCount() == 0) {
            agentProfileDao.insertProfiles(DefaultAgentProfiles.createDefaults())
        }
    }

    suspend fun resetToDefaults() {
        agentProfileDao.insertProfiles(DefaultAgentProfiles.createDefaults())
    }
}
