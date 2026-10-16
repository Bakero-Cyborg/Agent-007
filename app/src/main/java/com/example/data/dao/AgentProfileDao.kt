package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.AgentProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for AI Agent Profiles.
 */
@Dao
interface AgentProfileDao {

    @Query("SELECT * FROM agent_profiles ORDER BY isBuiltIn DESC, nameAr ASC")
    fun getAllProfiles(): Flow<List<AgentProfileEntity>>

    @Query("SELECT * FROM agent_profiles WHERE isEnabled = 1 ORDER BY isBuiltIn DESC, nameAr ASC")
    fun getEnabledProfiles(): Flow<List<AgentProfileEntity>>

    @Query("SELECT * FROM agent_profiles WHERE id = :id")
    fun getProfileById(id: String): Flow<AgentProfileEntity?>

    @Query("SELECT * FROM agent_profiles WHERE id = :id")
    suspend fun getProfileByIdSync(id: String): AgentProfileEntity?

    @Query("SELECT * FROM agent_profiles WHERE role = :roleName LIMIT 1")
    suspend fun getProfileByRoleSync(roleName: String): AgentProfileEntity?

    @Query("SELECT * FROM agent_profiles WHERE nameAr LIKE '%' || :query || '%' OR nameEn LIKE '%' || :query || '%' OR capabilities LIKE '%' || :query || '%'")
    fun searchProfiles(query: String): Flow<List<AgentProfileEntity>>

    @Query("SELECT COUNT(*) FROM agent_profiles")
    suspend fun getProfilesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AgentProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<AgentProfileEntity>)

    @Update
    suspend fun updateProfile(profile: AgentProfileEntity)

    @Query("UPDATE agent_profiles SET systemPrompt = :newPrompt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSystemPrompt(id: String, newPrompt: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE agent_profiles SET isEnabled = :isEnabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setAgentEnabled(id: String, isEnabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE agent_profiles SET temperature = :temperature, maxOutputTokens = :maxTokens, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateModelConfig(id: String, temperature: Float, maxTokens: Int, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteProfile(profile: AgentProfileEntity)

    @Query("DELETE FROM agent_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: String)

    @Query("DELETE FROM agent_profiles WHERE isBuiltIn = 0")
    suspend fun deleteAllCustomProfiles()
}
