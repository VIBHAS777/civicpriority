package com.civic.priority.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.civic.priority.data.UserRole
import com.civic.priority.ui.theme.CivicColors
import com.civic.priority.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val currentUser by viewModel.currentUser

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            currentUser?.let { user ->
                // User Details
                Card(
                    colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("User Details", fontWeight = FontWeight.Bold, color = CivicColors.TextSecondary, fontSize = 13.sp)

                        ProfileRow("Username", user.username)
                        ProfileRow("Email", user.email)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Role", color = CivicColors.TextPrimary)
                            Text(
                                user.role.displayName,
                                fontWeight = FontWeight.SemiBold,
                                color = when (user.role) {
                                    UserRole.COMMUNITY -> Color(0xFF4FC3F7)
                                    UserRole.ADMIN -> Color(0xFFFF9800)
                                    UserRole.SYSTEM_ADMIN -> Color.Red
                                }
                            )
                        }
                        ProfileRow("Member Since", formatDate(user.joinDate))
                    }
                }

                // Activity Stats
                Card(
                    colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Your Activity", fontWeight = FontWeight.Bold, color = CivicColors.TextSecondary, fontSize = 13.sp)

                        val reportedCount = viewModel.issues.count { it.reporterId == user.id }
                        val votedCount = viewModel.issues.count { it.votes.contains(user.id) }
                        val commentCount = viewModel.issues.flatMap { it.comments }.count { it.authorId == user.id }

                        ActivityStatRow(Icons.Default.Description, "Issues Reported", "$reportedCount", Color(0xFFFF9800))
                        ActivityStatRow(Icons.Default.ThumbUp, "Votes Cast", "$votedCount", Color(0xFF4FC3F7))
                        ActivityStatRow(Icons.Default.ChatBubble, "Comments Made", "$commentCount", Color.Green)
                    }
                }
            }

            // App Information
            Card(
                colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("App Information", fontWeight = FontWeight.Bold, color = CivicColors.TextSecondary, fontSize = 13.sp)

                    Text(
                        "CivicPriority helps communities report issues and optimizes civic resource allocation using a dynamic weighted scoring algorithm.",
                        fontSize = 13.sp,
                        color = CivicColors.TextSecondary
                    )

                    ProfileRow("Version", "1.0.0")
                    ProfileRow("Total Registered Users", "${viewModel.users.size}")
                }
            }

            // Logout Button
            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f))
            ) {
                Text("Log Out", color = Color.Red, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = CivicColors.TextPrimary)
        Text(value, color = CivicColors.TextSecondary)
    }
}

@Composable
private fun ActivityStatRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = CivicColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold, color = CivicColors.Primary)
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
