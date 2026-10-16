package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.entity.AgentProfileEntity
import com.example.model.AgentRole
import com.example.model.AgentTask
import com.example.model.OrchestrationRun
import com.example.model.RunStatus
import com.example.model.TaskStatus
import com.example.ui.components.ActiveAgentsStatusCard
import com.example.ui.components.AgentOperationalState
import com.example.ui.components.buildAgentLiveStatuses
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ActiveAgentsStatusSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test buildAgentLiveStatuses returns standby when no active run`() {
        val statuses = buildAgentLiveStatuses(activeRun = null, profiles = emptyList())
        assertEquals(AgentRole.entries.size, statuses.size)
        assertTrue(statuses.all { it.state == AgentOperationalState.STANDBY })
    }

    @Test
    fun `test buildAgentLiveStatuses identifies running and waiting approval agents`() {
        val task1 = AgentTask(
            taskId = "T1",
            runId = "R1",
            title = "Security Scan",
            objective = "Run vulnerability scan",
            assignedAgent = AgentRole.CYBERSECURITY,
            inputData = "",
            expectedOutput = "",
            status = TaskStatus.RUNNING
        )
        val task2 = AgentTask(
            taskId = "T2",
            runId = "R1",
            title = "Deploy Code",
            objective = "Deploy to production",
            assignedAgent = AgentRole.CODING,
            inputData = "",
            expectedOutput = "",
            status = TaskStatus.WAITING_FOR_APPROVAL
        )

        val run = OrchestrationRun(
            runId = "R1",
            userGoal = "Audit and deploy",
            status = RunStatus.RUNNING,
            tasks = listOf(task1, task2),
            activeAgents = setOf(AgentRole.CYBERSECURITY, AgentRole.CODING, AgentRole.SUPERVISOR)
        )

        val statuses = buildAgentLiveStatuses(activeRun = run, profiles = emptyList())
        val cyberStatus = statuses.find { it.role == AgentRole.CYBERSECURITY }
        val codingStatus = statuses.find { it.role == AgentRole.CODING }
        val supervisorStatus = statuses.find { it.role == AgentRole.SUPERVISOR }

        assertNotNull(cyberStatus)
        assertEquals(AgentOperationalState.RUNNING, cyberStatus!!.state)
        assertEquals("Security Scan", cyberStatus.activeTask?.title)

        assertNotNull(codingStatus)
        assertEquals(AgentOperationalState.WAITING_APPROVAL, codingStatus!!.state)

        assertNotNull(supervisorStatus)
        assertEquals(AgentOperationalState.RUNNING, supervisorStatus!!.state)
    }

    @Test
    fun `test buildAgentLiveStatuses respects disabled profiles`() {
        val disabledProfile = AgentProfileEntity(
            id = "creativity",
            nameAr = "وكيل الإبداع والمحتوى",
            nameEn = "Creativity Agent",
            role = AgentRole.CREATIVITY,
            descriptionAr = "",
            descriptionEn = "",
            systemPrompt = "",
            capabilities = emptyList(),
            isEnabled = false
        )

        val statuses = buildAgentLiveStatuses(activeRun = null, profiles = listOf(disabledProfile))
        val creativityStatus = statuses.find { it.role == AgentRole.CREATIVITY }

        assertNotNull(creativityStatus)
        assertEquals(AgentOperationalState.DISABLED, creativityStatus!!.state)
    }

    @Test
    fun `test ActiveAgentsStatusCard renders in Compose and filters correctly`() {
        val task = AgentTask(
            taskId = "T1",
            runId = "R1",
            title = "تحليل البيانات الإحصائية",
            objective = "حساب المؤشرات",
            assignedAgent = AgentRole.DATA_ANALYSIS,
            inputData = "",
            expectedOutput = "",
            status = TaskStatus.RUNNING
        )

        val run = OrchestrationRun(
            runId = "R1",
            userGoal = "تحليل الأداء",
            status = RunStatus.RUNNING,
            tasks = listOf(task),
            activeAgents = setOf(AgentRole.DATA_ANALYSIS, AgentRole.SUPERVISOR)
        )

        composeTestRule.setContent {
            ActiveAgentsStatusCard(
                activeRun = run,
                profiles = emptyList()
            )
        }

        // Verify main section is displayed
        composeTestRule.onNodeWithTag("active_agents_status_section").assertIsDisplayed()

        // Verify title text exists
        composeTestRule.onNodeWithText("مركز مراقبة الوكلاء النشطين").assertIsDisplayed()

        // Verify agent card for Supervisor is visible at the top
        composeTestRule.onNodeWithTag("agent_card_supervisor").assertIsDisplayed()

        // Verify agent card for Data Analysis exists in hierarchy
        composeTestRule.onNodeWithTag("agent_card_data_analysis").assertExists()

        // Click on "النشطة حالياً" filter chip
        composeTestRule.onNodeWithTag("filter_active").performClick()

        // Active agents (Data Analysis, Supervisor) should exist
        composeTestRule.onNodeWithTag("agent_card_data_analysis").assertExists()
        composeTestRule.onNodeWithTag("agent_card_supervisor").assertExists()
    }
}
