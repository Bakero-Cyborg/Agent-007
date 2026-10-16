package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AgentProfileEntity
import com.example.model.AgentRole
import com.example.model.AgentTask
import com.example.model.OrchestrationRun
import com.example.model.RunStatus
import com.example.model.TaskStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess

/**
 * Operational state classification for an AI agent in the multi-agent system.
 */
enum class AgentOperationalState(
    val titleAr: String,
    val titleEn: String,
    val color: Color
) {
    RUNNING("يعمل الآن", "Running", EmeraldSuccess),
    PLANNING("تخطيط وتنسيق", "Coordinating", ElectricViolet),
    WAITING_APPROVAL("بانتظار موافقة", "Waiting Approval", AmberWarning),
    QUEUED("في الانتظار", "Queued", CyberCyan),
    COMPLETED("أنجز المهام", "Completed", EmeraldSuccess),
    STANDBY("في الاستعداد", "Standby", Color(0xFF64748B)),
    DISABLED("معطل", "Disabled", CrimsonDanger)
}

/**
 * Aggregate status snapshot for an agent displayed on the centralized dashboard.
 */
data class AgentLiveStatusItem(
    val role: AgentRole,
    val profile: AgentProfileEntity?,
    val state: AgentOperationalState,
    val activeTask: AgentTask? = null,
    val completedTasksCount: Int = 0,
    val totalTasksCount: Int = 0,
    val currentActivitySummary: String
)

/**
 * Helper to compute live status items for all agents given active run and profiles.
 */
fun buildAgentLiveStatuses(
    activeRun: OrchestrationRun?,
    profiles: List<AgentProfileEntity>
): List<AgentLiveStatusItem> {
    val profileMap = profiles.associateBy { it.role }

    return AgentRole.entries.map { role ->
        val profile = profileMap[role]
        val isEnabled = profile?.isEnabled ?: true

        if (!isEnabled) {
            AgentLiveStatusItem(
                role = role,
                profile = profile,
                state = AgentOperationalState.DISABLED,
                currentActivitySummary = "تم تعطيل هذا الوكيل في إعدادات المنظومة"
            )
        } else if (activeRun != null && !activeRun.status.isFinished) {
            val assignedTasks = activeRun.tasks.filter { it.assignedAgent == role }
            val runningTask = assignedTasks.find { it.status == TaskStatus.RUNNING }
            val waitingTask = assignedTasks.find { it.status == TaskStatus.WAITING_FOR_APPROVAL }
            val queuedTask = assignedTasks.find { it.status == TaskStatus.QUEUED || it.status == TaskStatus.PENDING }
            val succeededCount = assignedTasks.count { it.status == TaskStatus.SUCCEEDED }

            when {
                runningTask != null -> {
                    AgentLiveStatusItem(
                        role = role,
                        profile = profile,
                        state = AgentOperationalState.RUNNING,
                        activeTask = runningTask,
                        completedTasksCount = succeededCount,
                        totalTasksCount = assignedTasks.size,
                        currentActivitySummary = runningTask.title
                    )
                }
                waitingTask != null -> {
                    AgentLiveStatusItem(
                        role = role,
                        profile = profile,
                        state = AgentOperationalState.WAITING_APPROVAL,
                        activeTask = waitingTask,
                        completedTasksCount = succeededCount,
                        totalTasksCount = assignedTasks.size,
                        currentActivitySummary = "بانتظار قرار بشري للمهمة: ${waitingTask.title}"
                    )
                }
                role == AgentRole.SUPERVISOR && activeRun.status == RunStatus.PLANNING -> {
                    AgentLiveStatusItem(
                        role = role,
                        profile = profile,
                        state = AgentOperationalState.PLANNING,
                        completedTasksCount = 0,
                        totalTasksCount = 1,
                        currentActivitySummary = "تفكيك الهدف إلى مهام فرعية وبناء مخطط الـ DAG والتحقق من التبعيات"
                    )
                }
                role == AgentRole.SUPERVISOR && activeRun.status == RunStatus.RUNNING -> {
                    AgentLiveStatusItem(
                        role = role,
                        profile = profile,
                        state = AgentOperationalState.RUNNING,
                        completedTasksCount = activeRun.tasks.count { it.status == TaskStatus.SUCCEEDED },
                        totalTasksCount = activeRun.tasks.size,
                        currentActivitySummary = "مراقبة مسارات التنفيذ وتوجيه التدفق بين الوكلاء"
                    )
                }
                queuedTask != null -> {
                    AgentLiveStatusItem(
                        role = role,
                        profile = profile,
                        state = AgentOperationalState.QUEUED,
                        activeTask = queuedTask,
                        completedTasksCount = succeededCount,
                        totalTasksCount = assignedTasks.size,
                        currentActivitySummary = "مجدول لتنفيذ: ${queuedTask.title}"
                    )
                }
                assignedTasks.isNotEmpty() && assignedTasks.all { it.status == TaskStatus.SUCCEEDED } -> {
                    AgentLiveStatusItem(
                        role = role,
                        profile = profile,
                        state = AgentOperationalState.COMPLETED,
                        completedTasksCount = succeededCount,
                        totalTasksCount = assignedTasks.size,
                        currentActivitySummary = "أنجز جميع المهام المسندة بنجاح ($succeededCount/${assignedTasks.size})"
                    )
                }
                role in activeRun.activeAgents -> {
                    AgentLiveStatusItem(
                        role = role,
                        profile = profile,
                        state = AgentOperationalState.QUEUED,
                        completedTasksCount = succeededCount,
                        totalTasksCount = assignedTasks.size,
                        currentActivitySummary = "مدرج ضمن فريق العمل، بانتظار مرحلة التنفيذ الخاصة به"
                    )
                }
                else -> {
                    AgentLiveStatusItem(
                        role = role,
                        profile = profile,
                        state = AgentOperationalState.STANDBY,
                        currentActivitySummary = "في وضع الاستعداد، جاهز لاستقبال مهام جديدة"
                    )
                }
            }
        } else {
            AgentLiveStatusItem(
                role = role,
                profile = profile,
                state = AgentOperationalState.STANDBY,
                currentActivitySummary = "جاهز في المنظومة، بانتظار تكليف هدف أو مهمة"
            )
        }
    }
}

enum class AgentStatusFilter(val titleAr: String) {
    ALL("الكل"),
    ACTIVE("النشطة حالياً"),
    STANDBY("في الاستعداد"),
    NEEDS_ATTENTION("تتطلب إجراء")
}

/**
 * Centralized Dashboard UI Component listing active AI agents and their current status.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveAgentsStatusCard(
    activeRun: OrchestrationRun?,
    profiles: List<AgentProfileEntity>,
    modifier: Modifier = Modifier,
    onNavigateToAgents: () -> Unit = {},
    onNavigateToWorkflow: () -> Unit = {},
    onAssignNewGoal: () -> Unit = {}
) {
    val liveStatuses = remember(activeRun, profiles) {
        buildAgentLiveStatuses(activeRun, profiles)
    }

    var selectedFilter by remember { mutableStateOf(AgentStatusFilter.ALL) }
    var expandedAgentRole by remember { mutableStateOf<AgentRole?>(null) }

    val activeCount = liveStatuses.count {
        it.state == AgentOperationalState.RUNNING ||
        it.state == AgentOperationalState.PLANNING ||
        it.state == AgentOperationalState.WAITING_APPROVAL ||
        it.state == AgentOperationalState.QUEUED
    }
    val standbyCount = liveStatuses.count { it.state == AgentOperationalState.STANDBY }
    val attentionCount = liveStatuses.count { it.state == AgentOperationalState.WAITING_APPROVAL }

    val filteredList = when (selectedFilter) {
        AgentStatusFilter.ALL -> liveStatuses
        AgentStatusFilter.ACTIVE -> liveStatuses.filter {
            it.state == AgentOperationalState.RUNNING ||
            it.state == AgentOperationalState.PLANNING ||
            it.state == AgentOperationalState.WAITING_APPROVAL ||
            it.state == AgentOperationalState.QUEUED
        }
        AgentStatusFilter.STANDBY -> liveStatuses.filter { it.state == AgentOperationalState.STANDBY }
        AgentStatusFilter.NEEDS_ATTENTION -> liveStatuses.filter {
            it.state == AgentOperationalState.WAITING_APPROVAL || it.state == AgentOperationalState.DISABLED
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_agents_status_section"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (activeCount > 0) EmeraldSuccess else CyberCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مركز مراقبة الوكلاء النشطين",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "حالة كل وكيل متخصص ومسار عمله الحالي في المنظومة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Active counter badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (activeCount > 0) EmeraldSuccess.copy(alpha = 0.15f)
                            else CyberCyan.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "$activeCount نشط • $standbyCount جاهز",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (activeCount > 0) EmeraldSuccess else CyberCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AgentStatusFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        AgentStatusFilter.ALL -> liveStatuses.size
                        AgentStatusFilter.ACTIVE -> activeCount
                        AgentStatusFilter.STANDBY -> standbyCount
                        AgentStatusFilter.NEEDS_ATTENTION -> attentionCount
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = "${filter.titleAr} ($count)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("filter_${filter.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan,
                            containerColor = DarkSurface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) CyberCyan else DarkSurfaceBorder,
                            borderWidth = 1.dp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Agent Items List
            if (filteredList.isEmpty()) {
                // Empty state for current filter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد وكلاء تطابق هذا الفلتر حالياً",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedFilter == AgentStatusFilter.ACTIVE && activeCount == 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onAssignNewGoal,
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = DarkSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تكليف هدف جديد للبدء", color = DarkSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredList.forEach { item ->
                        val isExpanded = expandedAgentRole == item.role
                        AgentStatusRow(
                            item = item,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedAgentRole = if (isExpanded) null else item.role
                            },
                            onNavigateToWorkflow = onNavigateToWorkflow,
                            onNavigateToAgents = onNavigateToAgents
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Quick Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إجمالي الفريق: ${AgentRole.entries.size} وكلاء متخصصين",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onNavigateToAgents,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("manage_all_agents_button")
                ) {
                    Text("إدارة ملفات الوكلاء", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * Individual Agent Status Row with glowing state badge, activity card, and expandable details.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentStatusRow(
    item: AgentLiveStatusItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigateToWorkflow: () -> Unit,
    onNavigateToAgents: () -> Unit
) {
    val roleColor = getAgentRoleColor(item.role)
    val roleIcon = getAgentRoleIcon(item.role)
    val isActivelyExecuting = item.state == AgentOperationalState.RUNNING || item.state == AgentOperationalState.PLANNING

    // Pulsing animation for actively working agents
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_${item.role.name}")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha_${item.role.name}"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("agent_card_${item.role.identifier}")
            .clickable(onClick = onToggleExpand),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(
            1.dp,
            if (isActivelyExecuting) roleColor.copy(alpha = 0.6f) else DarkSurfaceBorder
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Main Top Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Agent Avatar with status pulse ring
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActivelyExecuting) roleColor.copy(alpha = 0.2f * pulseAlpha)
                            else DarkSurfaceElevated
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = roleIcon,
                        contentDescription = item.role.titleAr,
                        tint = roleColor,
                        modifier = Modifier.size(24.dp)
                    )

                    // Small indicator dot in corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(item.state.color)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Agent Names & Role Title
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.profile?.nameAr ?: item.role.titleAr,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "• ${item.role.titleEn}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.currentActivitySummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActivelyExecuting) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Operational Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(item.state.color.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.state.titleAr,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = item.state.color
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "طي" else "توسيع",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded Details Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(
                        color = DarkSurfaceBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Role Description
                    Text(
                        text = item.profile?.descriptionAr ?: item.role.descriptionAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tool capabilities chips
                    val tools = item.profile?.capabilities ?: item.role.defaultTools
                    if (tools.isNotEmpty()) {
                        Text(
                            text = "الأدوات والقدرات المصرحة:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tools.forEach { tool ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkSurfaceElevated)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = tool,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = CyberCyan
                                    )
                                }
                            }
                        }
                    }

                    // Active task metadata if present
                    if (item.activeTask != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "المهمة: ${item.activeTask.taskId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                    Text(
                                        text = "أولوية ${item.activeTask.priority.titleAr}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.activeTask.objective,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (item.activeTask.status == TaskStatus.WAITING_FOR_APPROVAL) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تتطلب موافقة بشرية في شاشة المهام",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AmberWarning,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.activeTask != null) {
                            OutlinedButton(
                                onClick = onNavigateToWorkflow,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                            ) {
                                Text("عرض في الـ DAG", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Button(
                            onClick = onNavigateToAgents,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Text("تعديل البرومبت والتهيئة", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns distinct visual icon for each specialized agent role.
 */
fun getAgentRoleIcon(role: AgentRole): ImageVector {
    return when (role) {
        AgentRole.SUPERVISOR -> Icons.Default.Psychology
        AgentRole.RESEARCH -> Icons.Default.Search
        AgentRole.CODING -> Icons.Default.Code
        AgentRole.CYBERSECURITY -> Icons.Default.Security
        AgentRole.DATA_ANALYSIS -> Icons.Default.Analytics
        AgentRole.DATA_ENGINEERING -> Icons.Default.Storage
        AgentRole.AUTOMATION -> Icons.Default.Bolt
        AgentRole.CREATIVITY -> Icons.Default.AutoAwesome
        AgentRole.BUSINESS -> Icons.Default.TrendingUp
        AgentRole.PROJECT -> Icons.Default.AccountTree
        AgentRole.CRITIC_QA -> Icons.Default.FactCheck
    }
}

/**
 * Returns signature theme accent color for each agent role.
 */
fun getAgentRoleColor(role: AgentRole): Color {
    return when (role) {
        AgentRole.SUPERVISOR -> ElectricViolet
        AgentRole.CYBERSECURITY -> CrimsonDanger
        AgentRole.CODING -> CyberCyan
        AgentRole.RESEARCH -> Color(0xFF38BDF8)
        AgentRole.DATA_ANALYSIS -> Color(0xFFF472B6)
        AgentRole.DATA_ENGINEERING -> Color(0xFFFB923C)
        AgentRole.AUTOMATION -> Color(0xFFFBBF24)
        AgentRole.CREATIVITY -> Color(0xFFA78BFA)
        AgentRole.BUSINESS -> EmeraldSuccess
        AgentRole.PROJECT -> Color(0xFF2DD4BF)
        AgentRole.CRITIC_QA -> Color(0xFF34D399)
    }
}
