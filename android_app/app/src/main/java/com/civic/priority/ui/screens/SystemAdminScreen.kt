package com.civic.priority.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun SystemAdminScreen(viewModel: AppViewModel) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("User Management", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = CivicColors.TextPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Role Summary Cards
            item {
                val commCount = viewModel.users.count { it.role == UserRole.COMMUNITY }
                val adminCount = viewModel.users.count { it.role == UserRole.ADMIN }
                val sysCount = viewModel.users.count { it.role == UserRole.SYSTEM_ADMIN }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RoleStatCard("Members", commCount, Color(0xFF4FC3F7), Modifier.weight(1f))
                    RoleStatCard("Admins", adminCount, Color(0xFFFF9800), Modifier.weight(1f))
                    RoleStatCard("SysRoots", sysCount, Color.Red, Modifier.weight(1f))
                }
            }

            item {
                Text(
                    "Registered Users (Read-Only)",
                    fontWeight = FontWeight.Bold,
                    color = CivicColors.TextPrimary,
                    fontSize = 16.sp
                )
            }

            // User list
            items(viewModel.users) { user ->
                val currentUser by viewModel.currentUser
                val roleColor = roleColor(user.role)

                Card(
                    colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(roleColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                user.username.first().uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = roleColor,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    user.username,
                                    fontWeight = FontWeight.Bold,
                                    color = CivicColors.TextPrimary
                                )
                                if (user.id == currentUser?.id) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("(You)", fontSize = 11.sp, color = CivicColors.TextSecondary)
                                }
                            }
                            Text(user.email, fontSize = 12.sp, color = Color.Gray)
                            Text(
                                "Joined: ${formatDate(user.joinDate)}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        // Role badge
                        Text(
                            user.role.displayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = roleColor,
                            modifier = Modifier
                                .background(roleColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleStatCard(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CivicColors.CardBackground),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(title, fontSize = 12.sp, color = CivicColors.TextSecondary)
        }
    }
}

private fun roleColor(role: UserRole): Color {
    return when (role) {
        UserRole.COMMUNITY -> Color(0xFF4FC3F7)
        UserRole.ADMIN -> Color(0xFFFF9800)
        UserRole.SYSTEM_ADMIN -> Color.Red
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
