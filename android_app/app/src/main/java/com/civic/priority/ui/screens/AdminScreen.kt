package com.civic.priority.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.civic.priority.data.*
import com.civic.priority.ui.components.IssueRowCard
import com.civic.priority.ui.theme.CivicColors
import com.civic.priority.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdminScreen(viewModel: AppViewModel, navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var workers by remember { mutableFloatStateOf(viewModel.availableResources.value.fieldWorkers.toFloat()) }
    var vehicles by remember { mutableFloatStateOf(viewModel.availableResources.value.serviceVehicles.toFloat()) }
    var hours by remember { mutableFloatStateOf(viewModel.availableResources.value.workingHours.toFloat()) }

    // Smooth animation for sliders on Optimizer runs
    val animatedWorkers by animateFloatAsState(targetValue = workers, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "WorkersSlider")
    val animatedVehicles by animateFloatAsState(targetValue = vehicles, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "VehiclesSlider")
    val animatedHours by animateFloatAsState(targetValue = hours, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "HoursSlider")

    // Infinite pulse animation for Optimizer button
    val infiniteTransition = rememberInfiniteTransition(label = "ButtonPulse")
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ButtonScale"
    )

    // Override dialog state
    var showOverrideDialog by remember { mutableStateOf(false) }
    var overrideNote by remember { mutableStateOf("") }
    var selectedIssue by remember { mutableStateOf<Issue?>(null) }
    var targetStatus by remember { mutableStateOf(IssueStatus.RESOLVED) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = CivicColors.TextPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CivicColors.CardBackground,
                contentColor = CivicColors.Primary,
                indicator = {},
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Resource Optimizer",
                            color = if (selectedTab == 0) CivicColors.Primary else CivicColors.TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Audit Log",
                            color = if (selectedTab == 1) CivicColors.Primary else CivicColors.TextSecondary
                        )
                    }
                )
            }

            Crossfade(targetState = selectedTab, label = "AdminTabTransition") { tab ->
                if (tab == 0) {
                    // Resource Optimizer View
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                "Resource Constraints",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = CivicColors.TextPrimary
                            )
                        }

                        // Resource sliders card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(15.dp)
                                ) {
                                    // Workers slider
                                    Column {
                                        Text("Field Workers: ${workers.toInt()} / 20", color = CivicColors.TextPrimary)
                                        Slider(
                                            value = animatedWorkers, onValueChange = {
                                                workers = it
                                                viewModel.updateResources(workers.toDouble(), vehicles.toDouble(), hours.toDouble())
                                            },
                                            valueRange = 0f..20f, steps = 19,
                                            colors = SliderDefaults.colors(
                                                thumbColor = CivicColors.Primary,
                                                activeTrackColor = CivicColors.Primary
                                            )
                                        )
                                    }
                                    // Vehicles slider
                                    Column {
                                        Text("Service Vehicles: ${vehicles.toInt()} / 8", color = CivicColors.TextPrimary)
                                        Slider(
                                            value = animatedVehicles, onValueChange = {
                                                vehicles = it
                                                viewModel.updateResources(workers.toDouble(), vehicles.toDouble(), hours.toDouble())
                                            },
                                            valueRange = 0f..8f, steps = 7,
                                            colors = SliderDefaults.colors(
                                                thumbColor = CivicColors.Primary,
                                                activeTrackColor = CivicColors.Primary
                                            )
                                        )
                                    }
                                    // Hours slider
                                    Column {
                                        Text("Working Hours: ${hours.toInt()} / 120", color = CivicColors.TextPrimary)
                                        Slider(
                                            value = animatedHours, onValueChange = {
                                                hours = it
                                                viewModel.updateResources(workers.toDouble(), vehicles.toDouble(), hours.toDouble())
                                            },
                                            valueRange = 0f..120f, steps = 23,
                                            colors = SliderDefaults.colors(
                                                thumbColor = CivicColors.Primary,
                                                activeTrackColor = CivicColors.Primary
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Run Optimizer button
                        item {
                            Button(
                                onClick = {
                                    viewModel.runOptimizer()
                                    workers = viewModel.availableResources.value.fieldWorkers.toFloat()
                                    vehicles = viewModel.availableResources.value.serviceVehicles.toFloat()
                                    hours = viewModel.availableResources.value.workingHours.toFloat()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .graphicsLayer {
                                        scaleX = buttonScale
                                        scaleY = buttonScale
                                    },
                                border = BorderStroke(
                                    width = (1 + (buttonScale - 1f) * 60).dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            CivicColors.Primary,
                                            CivicColors.ButtonLt,
                                            CivicColors.Primary
                                        )
                                    )
                                ),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CivicColors.Primary)
                            ) {
                                Text("Run Resource Optimizer", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        // Dashboard summary
                        item {
                            val targetScheduled = viewModel.issues.count { it.status == IssueStatus.IN_PROGRESS }
                            val targetDeferred = viewModel.issues.count { it.status == IssueStatus.DEFERRED }
                            val animatedScheduled by animateIntAsState(targetValue = targetScheduled, animationSpec = tween(600), label = "ScheduledCount")
                            val animatedDeferred by animateIntAsState(targetValue = targetDeferred, animationSpec = tween(600), label = "DeferredCount")

                            Card(
                                colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Scheduled", fontSize = 12.sp, color = CivicColors.TextSecondary)
                                        Text(
                                            "$animatedScheduled",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4FC3F7)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Deferred", fontSize = 12.sp, color = CivicColors.TextSecondary)
                                        Text(
                                            "$animatedDeferred",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Red
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "Queue (Long-Press to Override)",
                                fontWeight = FontWeight.Bold,
                                color = CivicColors.TextPrimary,
                                fontSize = 16.sp
                            )
                        }

                        // Issue queue
                        val sortedIssues = viewModel.issues.sortedWith(Comparator { a, b -> Issue.sort(a, b) })
                        items(sortedIssues, key = { it.id }) { issue ->
                            var showContextMenu by remember { mutableStateOf(false) }

                            Box(modifier = Modifier.animateItemPlacement()) {
                                IssueRowCard(
                                    issue = issue,
                                    viewModel = viewModel,
                                    onClick = { navController.navigate("issue_detail/${issue.id}") }
                                )

                                // Long press overlay
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .combinedClickable(
                                            onClick = { navController.navigate("issue_detail/${issue.id}") },
                                            onLongClick = { showContextMenu = true }
                                        )
                                )

                                DropdownMenu(
                                    expanded = showContextMenu,
                                    onDismissRequest = { showContextMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Override to In Progress") },
                                        onClick = {
                                            selectedIssue = issue; targetStatus = IssueStatus.IN_PROGRESS
                                            showOverrideDialog = true; showContextMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Override to Resolved") },
                                        onClick = {
                                            selectedIssue = issue; targetStatus = IssueStatus.RESOLVED
                                            showOverrideDialog = true; showContextMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Override to Deferred") },
                                        onClick = {
                                            selectedIssue = issue; targetStatus = IssueStatus.DEFERRED
                                            showOverrideDialog = true; showContextMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Audit Log View
                    if (viewModel.auditLogs.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("No Audit Logs Available", fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Override actions performed by Admins will appear here.",
                                fontSize = 12.sp, color = CivicColors.TextSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(viewModel.auditLogs) { log ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(key1 = log.timestamp) {
                                    visible = true
                                }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(animationSpec = tween(350)) + slideInVertically(initialOffsetY = { 30 }, animationSpec = tween(350)),
                                    exit = fadeOut()
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    log.actionType,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CivicColors.Primary,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    formatTime(log.timestamp),
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Text(
                                                "${log.issueTitle} (#${log.issueId})",
                                                fontSize = 13.sp,
                                                color = CivicColors.TextPrimary
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    log.oldStatus,
                                                    fontSize = 12.sp,
                                                    color = Color.Gray,
                                                    textDecoration = TextDecoration.LineThrough
                                                )
                                                Text(" → ", fontSize = 12.sp, color = CivicColors.TextSecondary)
                                                Text(log.newStatus, fontSize = 12.sp, color = Color.Green)
                                            }
                                            Text("By: ${log.adminName}", fontSize = 11.sp, color = Color(0xFF4FC3F7))
                                            Text(
                                                "Reason: ${log.note}",
                                                fontSize = 12.sp,
                                                color = CivicColors.TextSecondary,
                                                fontWeight = FontWeight.Light
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Override dialog
    if (showOverrideDialog) {
        AlertDialog(
            onDismissRequest = { showOverrideDialog = false; overrideNote = "" },
            title = { Text("Override Note Required") },
            text = {
                Column {
                    Text("Please provide a reason for overriding this issue's status.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = overrideNote,
                        onValueChange = { overrideNote = it },
                        placeholder = { Text("Enter reason...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedIssue != null && overrideNote.isNotEmpty()) {
                        viewModel.overrideStatus(selectedIssue!!.id, targetStatus, overrideNote)
                        overrideNote = ""
                        showOverrideDialog = false
                    }
                }) {
                    Text("Confirm", color = CivicColors.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverrideDialog = false; overrideNote = "" }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
