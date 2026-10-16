package com.example.orchestrator

import com.example.model.AgentTask
import com.example.model.TaskStatus

/**
 * DAG Dependency Resolver for task coordination.
 * Resolves which tasks can run in parallel, and detects circular dependencies.
 */
object DependencyResolver {

    /**
     * Returns all tasks that are ready for immediate execution:
     * - Status is PENDING or QUEUED
     * - All predecessor tasks in [dependencies] have reached SUCCEEDED status
     */
    fun findReadyTasks(tasks: List<AgentTask>): List<AgentTask> {
        val taskMap = tasks.associateBy { it.taskId }
        return tasks.filter { task ->
            task.isReadyToExecute && task.dependencies.all { depId ->
                val dep = taskMap[depId]
                dep != null && dep.status == TaskStatus.SUCCEEDED
            }
        }
    }

    /**
     * Checks if any predecessor failed or was cancelled, which means downstream tasks
     * can no longer succeed.
     */
    fun findBlockedTasks(tasks: List<AgentTask>): List<AgentTask> {
        val taskMap = tasks.associateBy { it.taskId }
        return tasks.filter { task ->
            task.isReadyToExecute && task.dependencies.any { depId ->
                val dep = taskMap[depId]
                dep != null && (dep.status == TaskStatus.FAILED || dep.status == TaskStatus.CANCELLED || dep.status == TaskStatus.TIMED_OUT)
            }
        }
    }

    /**
     * Cycle detection using Kahn's algorithm or DFS to prevent infinite loops.
     */
    fun hasCycle(tasks: List<AgentTask>): Boolean {
        val inDegree = mutableMapOf<String, Int>()
        val adj = mutableMapOf<String, MutableList<String>>()

        for (task in tasks) {
            inDegree[task.taskId] = task.dependencies.size
            adj[task.taskId] = mutableListOf()
        }

        for (task in tasks) {
            for (dep in task.dependencies) {
                adj[dep]?.add(task.taskId)
            }
        }

        val queue = ArrayDeque<String>()
        for ((id, deg) in inDegree) {
            if (deg == 0) queue.add(id)
        }

        var visitedCount = 0
        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            visitedCount++
            for (neighbor in adj[curr] ?: emptyList()) {
                val newDeg = (inDegree[neighbor] ?: 1) - 1
                inDegree[neighbor] = newDeg
                if (newDeg == 0) {
                    queue.add(neighbor)
                }
            }
        }

        return visitedCount != tasks.size
    }
}
