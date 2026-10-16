package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AgentDatabase
import com.example.data.entity.AgentProfileEntity
import com.example.data.repository.AgentProfileRepository
import com.example.model.AgentRole
import com.example.model.ApprovalRequest
import com.example.model.ExecutionEvent
import com.example.model.OrchestrationRun
import com.example.model.TaskStatus
import com.example.orchestrator.AgentOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * High-level Platform Operational Metrics computed from verified run logs.
 */
data class PlatformMetrics(
    val totalRegisteredAgents: Int = AgentRole.entries.size,
    val activeAgentsCount: Int = 0,
    val totalTasksExecuted: Int = 0,
    val runningTasksCount: Int = 0,
    val succeededTasksCount: Int = 0,
    val failedTasksCount: Int = 0,
    val pendingApprovalsCount: Int = 0,
    val successRatePercent: Float = 100f,
    val averageTaskDurationSec: Float = 0f
)

class AgentPlatformViewModel(
    application: Application,
    private val orchestrator: AgentOrchestrator,
    private val repository: AgentProfileRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        AgentOrchestrator(),
        AgentProfileRepository(AgentDatabase.getInstance(application).agentProfileDao())
    )

    val runs: StateFlow<List<OrchestrationRun>> = orchestrator.runs
    val activeRun: StateFlow<OrchestrationRun?> = orchestrator.activeRun
    val events: StateFlow<List<ExecutionEvent>> = orchestrator.events
    val approvalRequests: StateFlow<List<ApprovalRequest>> = orchestrator.approvalRequests

    // Flow of persistent Agent Profiles from Room Database
    val agentProfiles: StateFlow<List<AgentProfileEntity>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _taskFilter = MutableStateFlow<TaskStatus?>(null)
    val taskFilter: StateFlow<TaskStatus?> = _taskFilter.asStateFlow()

    private val _selectedProfileForDetail = MutableStateFlow<AgentProfileEntity?>(null)
    val selectedProfileForDetail: StateFlow<AgentProfileEntity?> = _selectedProfileForDetail.asStateFlow()

    // Real computed metrics derived from all runs and persisted profiles
    val metrics: StateFlow<PlatformMetrics> = combine(
        runs,
        approvalRequests,
        agentProfiles
    ) { allRuns, approvals, profiles ->
        val allTasks = allRuns.flatMap { it.tasks }
        val succeeded = allTasks.count { it.status == TaskStatus.SUCCEEDED }
        val failed = allTasks.count { it.status == TaskStatus.FAILED || it.status == TaskStatus.TIMED_OUT }
        val running = allTasks.count { it.status == TaskStatus.RUNNING }
        val totalTerminated = succeeded + failed
        val successRate = if (totalTerminated > 0) (succeeded.toFloat() / totalTerminated) * 100f else 100f

        val activeRoles = allRuns.filter { !it.status.isFinished }
            .flatMap { it.activeAgents }
            .toSet()

        val avgDuration = if (allRuns.isNotEmpty()) {
            allRuns.map { it.durationSeconds }.average().toFloat()
        } else 0f

        PlatformMetrics(
            totalRegisteredAgents = if (profiles.isNotEmpty()) profiles.size else AgentRole.entries.size,
            activeAgentsCount = activeRoles.size,
            totalTasksExecuted = allTasks.size,
            runningTasksCount = running,
            succeededTasksCount = succeeded,
            failedTasksCount = failed,
            pendingApprovalsCount = approvals.count { it.status == com.example.model.ApprovalStatus.PENDING },
            successRatePercent = successRate,
            averageTaskDurationSec = avgDuration
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlatformMetrics())

    init {
        // Ensure default profiles are loaded into Room
        viewModelScope.launch {
            repository.ensureDefaultProfilesLoaded()
        }

        // Kick off an initial showcase run so the user immediately sees the multi-agent system coordinating!
        orchestrator.submitGoal("فحص أمني متقدم وتدقيق صلاحيات وتحديث رقع الحماية")
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun setTaskFilter(status: TaskStatus?) {
        _taskFilter.value = status
    }

    fun selectProfileDetail(profile: AgentProfileEntity?) {
        _selectedProfileForDetail.value = profile
    }

    fun updateSystemPrompt(id: String, newPrompt: String) {
        viewModelScope.launch {
            repository.updateSystemPrompt(id, newPrompt)
        }
    }

    fun toggleAgentEnabled(id: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.setAgentEnabled(id, isEnabled)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }

    fun saveCustomProfile(profile: AgentProfileEntity) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            repository.deleteProfileById(id)
            if (_selectedProfileForDetail.value?.id == id) {
                _selectedProfileForDetail.value = null
            }
        }
    }

    fun submitNewGoal(goal: String) {
        if (goal.isNotBlank()) {
            orchestrator.submitGoal(goal.trim())
            _selectedTab.value = 3 // Switch to supervisor command tab
        }
    }

    fun cancelActiveRun(runId: String) {
        orchestrator.cancelRun(runId)
    }

    fun resolveApproval(requestId: String, approved: Boolean, notes: String = "") {
        orchestrator.resolveApproval(requestId, approved, notes)
    }
}
