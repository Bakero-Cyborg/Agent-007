package com.example

import com.example.model.AgentRole
import com.example.model.AgentTask
import com.example.model.TaskStatus
import com.example.orchestrator.AgentOrchestrator
import com.example.orchestrator.DependencyResolver
import com.example.orchestrator.TaskValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class OrchestratorRobolectricTest {

    @Test
    fun `test state machine transitions enforce validity`() {
        // Valid transitions
        assertTrue(TaskStatus.PENDING.canTransitionTo(TaskStatus.QUEUED))
        assertTrue(TaskStatus.PENDING.canTransitionTo(TaskStatus.RUNNING))
        assertTrue(TaskStatus.RUNNING.canTransitionTo(TaskStatus.WAITING_FOR_APPROVAL))
        assertTrue(TaskStatus.RUNNING.canTransitionTo(TaskStatus.SUCCEEDED))
        assertTrue(TaskStatus.RUNNING.canTransitionTo(TaskStatus.FAILED))
        assertTrue(TaskStatus.WAITING_FOR_APPROVAL.canTransitionTo(TaskStatus.RUNNING))
        assertTrue(TaskStatus.WAITING_FOR_APPROVAL.canTransitionTo(TaskStatus.SUCCEEDED))

        // Terminal states cannot transition
        assertFalse(TaskStatus.SUCCEEDED.canTransitionTo(TaskStatus.RUNNING))
        assertFalse(TaskStatus.FAILED.canTransitionTo(TaskStatus.RUNNING))
        assertFalse(TaskStatus.CANCELLED.canTransitionTo(TaskStatus.PENDING))
        assertFalse(TaskStatus.TIMED_OUT.canTransitionTo(TaskStatus.SUCCEEDED))
    }

    @Test
    fun `test dependency resolver identifies ready tasks in DAG`() {
        val taskA = AgentTask(
            taskId = "A",
            runId = "R1",
            title = "Task A",
            objective = "First task",
            assignedAgent = AgentRole.RESEARCH,
            inputData = "",
            expectedOutput = "",
            dependencies = emptyList(),
            status = TaskStatus.SUCCEEDED
        )
        val taskB = AgentTask(
            taskId = "B",
            runId = "R1",
            title = "Task B",
            objective = "Depends on A",
            assignedAgent = AgentRole.CODING,
            inputData = "",
            expectedOutput = "",
            dependencies = listOf("A"),
            status = TaskStatus.PENDING
        )
        val taskC = AgentTask(
            taskId = "C",
            runId = "R1",
            title = "Task C",
            objective = "Depends on B",
            assignedAgent = AgentRole.CRITIC_QA,
            inputData = "",
            expectedOutput = "",
            dependencies = listOf("B"),
            status = TaskStatus.PENDING
        )

        val ready = DependencyResolver.findReadyTasks(listOf(taskA, taskB, taskC))
        assertEquals(1, ready.size)
        assertEquals("B", ready[0].taskId)
    }

    @Test
    fun `test cycle detection prevents deadlock`() {
        // Cyclic dependency: A -> B -> C -> A
        val taskA = AgentTask(
            taskId = "A", runId = "R1", title = "Task A", objective = "",
            assignedAgent = AgentRole.RESEARCH, inputData = "", expectedOutput = "",
            dependencies = listOf("C")
        )
        val taskB = AgentTask(
            taskId = "B", runId = "R1", title = "Task B", objective = "",
            assignedAgent = AgentRole.CODING, inputData = "", expectedOutput = "",
            dependencies = listOf("A")
        )
        val taskC = AgentTask(
            taskId = "C", runId = "R1", title = "Task C", objective = "",
            assignedAgent = AgentRole.AUTOMATION, inputData = "", expectedOutput = "",
            dependencies = listOf("B")
        )

        assertTrue(DependencyResolver.hasCycle(listOf(taskA, taskB, taskC)))

        // Non-cyclic DAG: A -> B, A -> C
        val acyclicC = taskC.copy(dependencies = listOf("A"))
        assertFalse(DependencyResolver.hasCycle(listOf(taskA.copy(dependencies = emptyList()), taskB, acyclicC)))
    }

    @Test
    fun `test task validator rejects empty and incomplete outputs`() {
        val dummyTask = AgentTask(
            taskId = "T1", runId = "R1", title = "Check", objective = "Audit",
            assignedAgent = AgentRole.CYBERSECURITY, inputData = "", expectedOutput = ""
        )

        val emptyRes = TaskValidator.validate(dummyTask, "")
        assertFalse(emptyRes.isValid)

        val todoRes = TaskValidator.validate(dummyTask, "TODO: implement security audit later")
        assertFalse(todoRes.isValid)

        val validRes = TaskValidator.validate(
            dummyTask,
            "تقرير التدقيق الأمني: تم فحص ضوابط الأمان وتقييم المخاطر وتطبيق ضوابط الصلاحيات الأقل بنجاح."
        )
        assertTrue(validRes.isValid)
        assertTrue(validRes.score >= 0.70f)
    }

    @Test
    fun `test end-to-end orchestration pipeline with mock execution`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val orchestrator = AgentOrchestrator(
            scope = testScope
        )

        val runId = orchestrator.submitGoal("بحث استراتيجي في نماذج الأعمال والجدوى الاقتصادية")
        assertNotNull(runId)

        // Advance coroutines to complete supervisor planning and task execution
        testScheduler.advanceTimeBy(10_000)
        testScope.advanceUntilIdle()

        val runs = orchestrator.runs.value
        val currentRun = runs.find { it.runId == runId }
        assertNotNull(currentRun)
        assertFalse(currentRun!!.tasks.isEmpty())
        assertTrue(currentRun.tasks.all { it.status == TaskStatus.SUCCEEDED })
    }

    @Test
    fun `test ViewModelProvider creates AgentPlatformViewModel instance with Application`() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        val vm = factory.create(com.example.viewmodel.AgentPlatformViewModel::class.java)
        assertNotNull(vm)
    }
}

