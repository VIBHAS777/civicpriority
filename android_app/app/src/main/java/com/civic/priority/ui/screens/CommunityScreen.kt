package com.civic.priority.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.civic.priority.data.*
import com.civic.priority.ui.components.IssueRowCard
import com.civic.priority.ui.theme.CivicColors
import com.civic.priority.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(viewModel: AppViewModel, navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf<IssueStatus?>(null) }
    var sortMode by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("CivicPriority", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate("new_issue") }) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "Report Issue",
                            tint = CivicColors.Primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = CivicColors.TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented tabs
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
                            "Dashboard",
                            color = if (selectedTab == 0) CivicColors.Primary else CivicColors.TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Issue Registry",
                            color = if (selectedTab == 1) CivicColors.Primary else CivicColors.TextSecondary
                        )
                    }
                )
            }

            Crossfade(targetState = selectedTab, label = "TabTransition") { tab ->
                if (tab == 0) {
                    DashboardTab(viewModel = viewModel)
                } else {
                    RegistryTab(
                        viewModel = viewModel,
                        navController = navController,
                        searchText = searchText,
                        onSearchChange = { searchText = it },
                        filterStatus = filterStatus,
                        onFilterChange = { filterStatus = it },
                        sortMode = sortMode,
                        onSortChange = { sortMode = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTab(viewModel: AppViewModel) {
    var card1Visible by remember { mutableStateOf(false) }
    var card2Visible by remember { mutableStateOf(false) }
    var card3Visible by remember { mutableStateOf(false) }
    var chartVisible by remember { mutableStateOf(false) }
    var formulaVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        card1Visible = true
        delay(80)
        card2Visible = true
        delay(80)
        card3Visible = true
        delay(80)
        chartVisible = true
        delay(80)
        formulaVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Counters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val total = viewModel.issues.size
            val open = viewModel.issues.count { it.status == IssueStatus.OPEN }
            val resolved = viewModel.issues.count { it.status == IssueStatus.RESOLVED }

            AnimatedVisibility(
                visible = card1Visible,
                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(400)),
                modifier = Modifier.weight(1f)
            ) {
                StatCard(
                    title = "Total", value = "$total",
                    icon = Icons.Default.Description, color = CivicColors.TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = card2Visible,
                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(400)),
                modifier = Modifier.weight(1f)
            ) {
                StatCard(
                    title = "Open", value = "$open",
                    icon = Icons.Default.Error, color = Color(0xFFFF9800),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = card3Visible,
                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(400)),
                modifier = Modifier.weight(1f)
            ) {
                StatCard(
                    title = "Resolved", value = "$resolved",
                    icon = Icons.Default.CheckCircle, color = CivicColors.Primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Priority Distribution Chart
        val infiniteTransition = rememberInfiniteTransition(label = "borderFloatChart")
        val borderAngleChart by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(15000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "borderAngleChart"
        )

        AnimatedVisibility(
            visible = chartVisible,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { 60 }, animationSpec = tween(500))
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
                    .drawWithContent {
                        drawContent()
                        val angleRad = Math.toRadians(borderAngleChart.toDouble()).toFloat()
                        val cosVal = cos(angleRad)
                        val sinVal = sin(angleRad)
                        val startOffset = Offset(
                            x = size.width / 2f + cosVal * size.width / 2f,
                            y = size.height / 2f + sinVal * size.height / 2f
                        )
                        val endOffset = Offset(
                            x = size.width / 2f - cosVal * size.width / 2f,
                            y = size.height / 2f - sinVal * size.height / 2f
                        )
                        val brush = Brush.linearGradient(
                            colors = listOf(
                                CivicColors.Primary.copy(alpha = 0.7f),
                                Color.Transparent,
                                CivicColors.Primary.copy(alpha = 0.7f)
                            ),
                            start = startOffset,
                            end = endOffset
                        )
                        drawOutline(
                            outline = Outline.Rounded(
                                RoundRect(
                                    left = 0f,
                                    top = 0f,
                                    right = size.width,
                                    bottom = size.height,
                                    cornerRadius = CornerRadius(20.dp.toPx())
                                )
                            ),
                            brush = brush,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Priority Level Distribution",
                        fontWeight = FontWeight.Bold,
                        color = CivicColors.TextPrimary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val critical = viewModel.issues.count { it.priorityLevel == PriorityLevel.CRITICAL }
                    val high = viewModel.issues.count { it.priorityLevel == PriorityLevel.HIGH }
                    val medium = viewModel.issues.count { it.priorityLevel == PriorityLevel.MEDIUM }
                    val low = viewModel.issues.count { it.priorityLevel == PriorityLevel.LOW }
                    val maxVal = maxOf(critical, high, medium, low, 1)

                    var animationPlayed by remember { mutableStateOf(false) }
                    LaunchedEffect(key1 = true) {
                        animationPlayed = true
                    }

                    val bars = listOf(
                        Triple("Critical", critical, Color(0xFFE53935) to Brush.horizontalGradient(listOf(Color(0xFFE53935), Color(0xFFFF8A80)))),
                        Triple("High", high, Color(0xFFFF9800) to Brush.horizontalGradient(listOf(Color(0xFFFF9800), Color(0xFFFFE082)))),
                        Triple("Medium", medium, Color(0xFFFDD835) to Brush.horizontalGradient(listOf(Color(0xFFFDD835), Color(0xFFFFF59D)))),
                        Triple("Low", low, Color(0xFF4CAF50) to Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFFA5D6A7))))
                    )

                    bars.forEach { (label, count, pair) ->
                        val (color, brush) = pair
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                color = CivicColors.TextSecondary,
                                modifier = Modifier.width(60.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.05f),
                                        RoundedCornerShape(4.dp)
                                    )
                            ) {
                                val fraction = if (maxVal > 0) count.toFloat() / maxVal else 0f
                                val animatedFraction by animateFloatAsState(
                                    targetValue = if (animationPlayed) fraction else 0f,
                                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                                    label = "BarChartAnimation"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animatedFraction)
                                        .background(brush = brush, shape = RoundedCornerShape(4.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                    }
                }
            }
        }


    }
}

@Composable
private fun StatCard(
    title: String, value: String, icon: ImageVector, color: Color,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    val targetVal = value.toIntOrNull() ?: 0
    val animatedValue by animateIntAsState(
        targetValue = if (animationPlayed) targetVal else 0,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "StatCardCount"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "borderFloat")
    val borderAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "borderAngle"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(15.dp),
        modifier = modifier
            .shadow(
                elevation = 8.dp, shape = RoundedCornerShape(15.dp),
                ambientColor = color.copy(alpha = 0.3f),
                spotColor = color.copy(alpha = 0.3f)
            )
            .drawWithContent {
                drawContent()
                val angleRad = Math.toRadians(borderAngle.toDouble()).toFloat()
                val cosVal = cos(angleRad)
                val sinVal = sin(angleRad)
                val startOffset = Offset(
                    x = size.width / 2f + cosVal * size.width / 2f,
                    y = size.height / 2f + sinVal * size.height / 2f
                )
                val endOffset = Offset(
                    x = size.width / 2f - cosVal * size.width / 2f,
                    y = size.height / 2f - sinVal * size.height / 2f
                )
                val brush = Brush.linearGradient(
                    colors = listOf(
                        color.copy(alpha = 0.8f),
                        color.copy(alpha = 0.2f),
                        Color.Transparent,
                        color.copy(alpha = 0.8f)
                    ),
                    start = startOffset,
                    end = endOffset
                )
                drawOutline(
                    outline = Outline.Rounded(
                        RoundRect(
                            left = 0f,
                            top = 0f,
                            right = size.width,
                            bottom = size.height,
                            cornerRadius = CornerRadius(15.dp.toPx())
                        )
                    ),
                    brush = brush,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Text("$animatedValue", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CivicColors.TextPrimary)
            Text(title, fontSize = 12.sp, color = CivicColors.TextSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistryTab(
    viewModel: AppViewModel,
    navController: NavController,
    searchText: String,
    onSearchChange: (String) -> Unit,
    filterStatus: IssueStatus?,
    onFilterChange: (IssueStatus?) -> Unit,
    sortMode: Int,
    onSortChange: (Int) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            placeholder = { Text("Search issues, reporters...", color = CivicColors.TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedBorderColor = CivicColors.Primary.copy(alpha = 0.3f),
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        // Sort & Filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                TextButton(onClick = { showSortMenu = true }) {
                    Text(
                        if (sortMode == 0) "Highest Priority" else "Newest",
                        color = CivicColors.Primary,
                        fontSize = 13.sp
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CivicColors.Primary)
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Highest Priority") },
                        onClick = { onSortChange(0); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Newest") },
                        onClick = { onSortChange(1); showSortMenu = false }
                    )
                }
            }

            Box {
                TextButton(onClick = { showFilterMenu = true }) {
                    Text(
                        filterStatus?.displayName ?: "All",
                        color = CivicColors.Primary,
                        fontSize = 13.sp
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CivicColors.Primary)
                }
                DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = { onFilterChange(null); showFilterMenu = false }
                    )
                    IssueStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.displayName) },
                            onClick = { onFilterChange(status); showFilterMenu = false }
                        )
                    }
                }
            }
        }

        // Issues list
        val filteredIssues = remember(viewModel.issues.toList(), searchText, filterStatus, sortMode) {
            var result = viewModel.issues.toList()

            filterStatus?.let { status ->
                result = result.filter { it.status == status }
            }

            if (searchText.isNotEmpty()) {
                result = result.filter {
                    it.title.contains(searchText, ignoreCase = true) ||
                            it.description.contains(searchText, ignoreCase = true) ||
                            it.reporterName.contains(searchText, ignoreCase = true)
                }
            }

            if (sortMode == 0) {
                result.sortedWith(Comparator { a, b -> Issue.sort(a, b) })
            } else {
                result.sortedByDescending { it.dateReported }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(filteredIssues, key = { _, it -> it.id }) { index, issue ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(key1 = issue.id) {
                    kotlinx.coroutines.delay(index * 50L) // Staggered entrance
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { 60 }, animationSpec = tween(400)),
                    exit = fadeOut()
                ) {
                    IssueRowCard(
                        issue = issue,
                        viewModel = viewModel,
                        onClick = { navController.navigate("issue_detail/${issue.id}") }
                    )
                }
            }
        }
    }
}
