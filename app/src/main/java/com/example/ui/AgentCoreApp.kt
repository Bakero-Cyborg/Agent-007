package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.screens.AgentsScreen
import com.example.ui.screens.CommandScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.WorkflowScreen
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.viewmodel.AgentPlatformViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentCoreApp(
    viewModel: AgentPlatformViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val activeRun by viewModel.activeRun.collectAsState()
    val events by viewModel.events.collectAsState()
    val approvalRequests by viewModel.approvalRequests.collectAsState()
    val taskFilter by viewModel.taskFilter.collectAsState()
    val agentProfiles by viewModel.agentProfiles.collectAsState()
    val selectedProfile by viewModel.selectedProfileForDetail.collectAsState()

    // Enforce RTL layout for natural Arabic experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            containerColor = DarkBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "A",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = DarkBackground
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(DarkSurfaceElevated)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Multi-Agent OS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = CyberCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("main_navigation_bar")
                ) {
                    val pendingApprovals = metrics.pendingApprovalsCount

                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text(stringResource(R.string.dashboard_tab)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            indicatorColor = DarkSurfaceElevated,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        icon = { Icon(Icons.Default.Group, contentDescription = null) },
                        label = { Text(stringResource(R.string.agents_tab)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            indicatorColor = DarkSurfaceElevated,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        icon = {
                            if (pendingApprovals > 0) {
                                BadgedBox(badge = {
                                    Badge(containerColor = AmberWarning) {
                                        Text("$pendingApprovals", color = DarkBackground, fontWeight = FontWeight.Bold)
                                    }
                                }) {
                                    Icon(Icons.Default.AccountTree, contentDescription = null)
                                }
                            } else {
                                Icon(Icons.Default.AccountTree, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(R.string.tasks_tab)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            indicatorColor = DarkSurfaceElevated,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { viewModel.selectTab(3) },
                        icon = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
                        label = { Text(stringResource(R.string.chat_tab)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            indicatorColor = DarkSurfaceElevated,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { viewModel.selectTab(4) },
                        icon = { Icon(Icons.Default.Security, contentDescription = null) },
                        label = { Text(stringResource(R.string.security_tab)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberCyan,
                            selectedTextColor = CyberCyan,
                            indicatorColor = DarkSurfaceElevated,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(DarkBackground)
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        metrics = metrics,
                        activeRun = activeRun,
                        events = events,
                        profiles = agentProfiles,
                        onNavigateToCommand = { viewModel.selectTab(3) },
                        onNavigateToWorkflows = { viewModel.selectTab(2) },
                        onNavigateToAgents = { viewModel.selectTab(1) },
                        onCancelRun = { viewModel.cancelActiveRun(it) }
                    )
                    1 -> AgentsScreen(
                        profiles = agentProfiles,
                        activeAgents = activeRun?.activeAgents ?: emptySet(),
                        selectedProfile = selectedProfile,
                        onSelectProfile = { viewModel.selectProfileDetail(it) },
                        onUpdateSystemPrompt = { id, prompt -> viewModel.updateSystemPrompt(id, prompt) },
                        onToggleEnabled = { id, enabled -> viewModel.toggleAgentEnabled(id, enabled) },
                        onResetDefaults = { viewModel.resetToDefaults() }
                    )
                    2 -> WorkflowScreen(
                        activeRun = activeRun,
                        approvalRequests = approvalRequests,
                        selectedFilter = taskFilter,
                        onSelectFilter = { viewModel.setTaskFilter(it) },
                        onResolveApproval = { id, approved -> viewModel.resolveApproval(id, approved) }
                    )
                    3 -> CommandScreen(
                        activeRun = activeRun,
                        onSubmitGoal = { viewModel.submitNewGoal(it) },
                        onCancelRun = { viewModel.cancelActiveRun(it) }
                    )
                    4 -> SecurityScreen(events = events)
                }
            }
        }
    }
}
