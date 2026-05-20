package com.civic.priority.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civic.priority.data.*
import com.civic.priority.ui.theme.CivicColors
import com.civic.priority.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(viewModel: AppViewModel, navController: NavController, issueId: String) {
    val issue = viewModel.getIssueById(issueId) ?: return
    var commentText by remember { mutableStateOf("") }
    val currentUser by viewModel.currentUser
    val hasVoted = currentUser?.let { issue.votes.contains(it.id) } ?: false

    val coroutineScope = rememberCoroutineScope()
    var isVoting by remember { mutableStateOf(false) }
    val voteScale by animateFloatAsState(
        targetValue = if (isVoting) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
        label = "VoteScale"
    )

    val priorityColor = issue.priorityLevel.getColor()
    val infiniteTransition = rememberInfiniteTransition(label = "borderFloatDetail")
    val borderAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "borderAngle"
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Issue Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CivicColors.TextPrimary)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    String.format("%.1f", issue.finalScore),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            CivicColors.scoreColor(issue.finalScore),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(issue.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CivicColors.TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(issue.status.displayName, fontSize = 14.sp, color = CivicColors.TextSecondary)
                        if (issue.isOverridden) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("(Admin Overridden)", fontSize = 12.sp, color = Color.Red)
                        }
                    }
                }
            }

            // Score Breakdown Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = priorityColor.copy(alpha = 0.25f), spotColor = priorityColor.copy(alpha = 0.25f))
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
                                priorityColor.copy(alpha = 0.6f),
                                Color.Transparent,
                                priorityColor.copy(alpha = 0.6f)
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Score Breakdown", fontWeight = FontWeight.Bold, color = CivicColors.Primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailRow(Icons.Default.BarChart, "Base Score", String.format("%.1f", issue.baseScore))
                        DetailRow(Icons.Default.ThumbUp, "Vote Score", String.format("+%.1f", issue.voteScore))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailRow(Icons.Default.Star, "Category Bonus", String.format("+%.1f", issue.category.bonus))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = issue.priorityLevel.getColor(), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Priority", fontSize = 12.sp, color = CivicColors.TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(issue.priorityLevel.displayName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = issue.priorityLevel.getColor())
                        }
                    }
                }
            }

            // Details Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = priorityColor.copy(alpha = 0.25f), spotColor = priorityColor.copy(alpha = 0.25f))
                    .drawWithContent {
                        drawContent()
                        val angleRad = Math.toRadians((borderAngle + 180f).toDouble() % 360.0).toFloat()
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
                                priorityColor.copy(alpha = 0.6f),
                                Color.Transparent,
                                priorityColor.copy(alpha = 0.6f)
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(Icons.Default.Label, "Category", issue.category.displayName)
                    DetailRow(Icons.Default.Warning, "Severity", "${issue.severity.toInt()} / 5")
                    DetailRow(Icons.Default.Groups, "Affected People", "${issue.affectedPeople.toInt()}")
                    DetailRow(Icons.Default.LocationOn, "Location", issue.locationType.displayName)
                    val daysPending = maxOf(0, ((System.currentTimeMillis() - issue.dateReported) / 86400000).toInt())
                    DetailRow(Icons.Default.Schedule, "Days Pending", "$daysPending")
                    DetailRow(Icons.Default.Person, "Reported By", issue.reporterName)
                }
            }

            // Photo Evidence
            issue.imageData?.let { data ->
                val bitmap = remember(data) {
                    BitmapFactory.decodeByteArray(data, 0, data.size)
                }
                bitmap?.let {
                    Column {
                        Text("Photo Evidence", fontWeight = FontWeight.Bold, color = CivicColors.TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Issue photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }

            // Description
            Column {
                Text("Description", fontWeight = FontWeight.Bold, color = CivicColors.TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(issue.description, color = CivicColors.TextPrimary)
            }

            // Deferral Reason
            issue.deferralReason?.let { reason ->
                Text(
                    "Deferral Reason: $reason",
                    fontSize = 13.sp,
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
            }

            // Vote Button
            Button(
                onClick = {
                    coroutineScope.launch {
                        isVoting = true
                        viewModel.toggleVote(issueId)
                        delay(120)
                        isVoting = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .graphicsLayer {
                        scaleX = voteScale
                        scaleY = voteScale
                    },
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CivicColors.Primary.copy(alpha = if (hasVoted) 0.7f else 1f)
                )
            ) {
                Icon(
                    if (hasVoted) Icons.Default.ThumbUp else Icons.Default.ThumbUp,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (hasVoted) "Voted (${issue.votes.size})" else "Vote (${issue.votes.size})",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            // Comments Section
            Text("Comments", fontWeight = FontWeight.Bold, color = CivicColors.TextPrimary)

            issue.comments.forEach { comment ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CivicColors.TextPrimary)
                            Text(
                                formatDate(comment.date),
                                fontSize = 12.sp,
                                color = CivicColors.TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(comment.text, color = CivicColors.TextPrimary)
                    }
                }
            }

            // Add Comment
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment...", color = CivicColors.TextSecondary) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CivicColors.Primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = CivicColors.Primary
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (commentText.isNotEmpty()) {
                        viewModel.addComment(issueId, commentText)
                        commentText = ""
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = CivicColors.Primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = CivicColors.TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(title, fontSize = 12.sp, color = CivicColors.TextSecondary)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CivicColors.TextPrimary)
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
