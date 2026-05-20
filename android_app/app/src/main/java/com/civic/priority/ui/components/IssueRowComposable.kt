package com.civic.priority.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civic.priority.data.*
import com.civic.priority.ui.theme.CivicColors
import com.civic.priority.viewmodel.AppViewModel
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueRowCard(
    issue: Issue,
    viewModel: AppViewModel,
    onClick: () -> Unit
) {
    val currentUser by viewModel.currentUser
    val hasVoted = currentUser?.let { issue.votes.contains(it.id) } ?: false
    val priorityColor = issue.priorityLevel.getColor()

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = priorityColor.copy(alpha = 0.25f),
                spotColor = priorityColor.copy(alpha = 0.25f)
            )
            .drawWithContent {
                drawContent()
                val brush = Brush.linearGradient(
                    colors = listOf(
                        priorityColor.copy(alpha = 0.6f),
                        Color.Transparent,
                        priorityColor.copy(alpha = 0.6f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                drawOutline(
                    outline = Outline.Rounded(
                        RoundRect(
                            left = 0f,
                            top = 0f,
                            right = size.width,
                            bottom = size.height,
                            cornerRadius = CornerRadius(12.dp.toPx())
                        )
                    ),
                    brush = brush,
                    style = Stroke(width = 1.dp.toPx())
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title row with score badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        issue.title,
                        fontWeight = FontWeight.Bold,
                        color = CivicColors.TextPrimary,
                        maxLines = 2,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Category badge
                        Text(
                            issue.category.displayName,
                            fontSize = 11.sp,
                            color = Color(0xFF4FC3F7),
                            modifier = Modifier
                                .background(
                                    Color(0xFF4FC3F7).copy(alpha = 0.1f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        // Overridden badge
                        if (issue.isOverridden) {
                            Text(
                                "Overridden",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                modifier = Modifier
                                    .background(
                                        Color.Red.copy(alpha = 0.1f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Score capsule
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        String.format("%.1f", issue.finalScore),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(50),
                                ambientColor = CivicColors.scoreColor(issue.finalScore).copy(alpha = 0.5f),
                                spotColor = CivicColors.scoreColor(issue.finalScore).copy(alpha = 0.5f)
                            )
                            .background(
                                CivicColors.scoreColor(issue.finalScore),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        issue.priorityLevel.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = issue.priorityLevel.getColor()
                    )
                }
            }

            // Description
            Text(
                issue.description,
                fontSize = 14.sp,
                color = CivicColors.TextSecondary,
                maxLines = 2
            )

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${issue.severity.toInt()}", fontSize = 12.sp, color = Color(0xFFFF9800))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF4FC3F7), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${issue.affectedPeople.toInt()}", fontSize = 12.sp, color = Color(0xFF4FC3F7))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFCE93D8), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(issue.locationType.displayName, fontSize = 11.sp, color = Color(0xFFCE93D8))
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            // Status + actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    Text("Status: ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CivicColors.TextPrimary)
                    Text(
                        issue.status.displayName,
                        fontSize = 12.sp,
                        color = statusColor(issue.status)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${issue.comments.size}", fontSize = 12.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (hasVoted) Icons.Default.ThumbUp else Icons.Default.ThumbUp,
                            contentDescription = null,
                            tint = if (hasVoted) Color.Green else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${issue.votes.size}", fontSize = 12.sp, color = if (hasVoted) Color.Green else Color.Gray)
                    }
                }
            }
        }
    }
}

fun statusColor(status: IssueStatus): Color {
    return when (status) {
        IssueStatus.OPEN -> Color(0xFFFF9800)
        IssueStatus.IN_PROGRESS -> Color(0xFF4FC3F7)
        IssueStatus.RESOLVED -> Color.Green
        IssueStatus.DEFERRED -> Color.Red
    }
}
