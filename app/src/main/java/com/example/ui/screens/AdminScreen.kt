package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.ui.components.getDrawablePainter
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.ReportWithUsers
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val state by adminViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dashboard/Users, 1: Reports, 2: Settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoldAccent,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "ADMIN",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("Gaston Love • Administration", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Notification toast for admin actions
            state.message?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GoldAccent.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        IconButton(onClick = { adminViewModel.clearMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Stats Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard(
                    title = "Membres",
                    value = "${state.totalUsers}",
                    icon = Icons.Default.People,
                    color = CrimsonPrimary,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Matchs",
                    value = "${state.totalMatches}",
                    icon = Icons.Default.Favorite,
                    color = RoseSecondary,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Messages",
                    value = "${state.totalMessages}",
                    icon = Icons.Default.Forum,
                    color = GoldAccent,
                    modifier = Modifier.weight(1f)
                )
                AdminStatCard(
                    title = "Alertes",
                    value = "${state.pendingReportsCount}",
                    icon = Icons.Default.Shield,
                    color = if (state.pendingReportsCount > 0) Color(0xFFEF4444) else EmeraldOnline,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tabs Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(
                    selected = (selectedTab == 0),
                    onClick = { selectedTab = 0 },
                    text = { Text("Utilisateurs (${state.allUsers.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = (selectedTab == 1),
                    onClick = { selectedTab = 1 },
                    text = { Text("Signalements (${state.reports.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = (selectedTab == 2),
                    onClick = { selectedTab = 2 },
                    text = { Text("Paramètres", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TAB 0: User Management
            if (selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search & Filter
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { adminViewModel.setSearchQuery(it) },
                        placeholder = { Text("Rechercher nom, e-mail ou ville...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("TOUS" to "Tous", "VERIFIES" to "Vérifiés", "SUSPENDUS" to "Suspendus").forEach { (code, label) ->
                            FilterChip(
                                selected = (state.filterStatus == code),
                                onClick = { adminViewModel.setFilterStatus(code) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CrimsonPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(state.filteredUsers) { user ->
                            AdminUserItemCard(
                                user = user,
                                onToggleBan = { adminViewModel.toggleBanUser(user) },
                                onToggleVerify = { adminViewModel.toggleVerifyUser(user) },
                                onDelete = { adminViewModel.deleteUser(user) }
                            )
                        }
                    }
                }
            }

            // TAB 1: Reports Moderation
            else if (selectedTab == 1) {
                if (state.reports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Aucun signalement en attente. La communauté est saine ! 🎉",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(state.reports) { reportWithUsers ->
                            AdminReportItemCard(
                                report = reportWithUsers,
                                onResolve = { adminViewModel.updateReport(reportWithUsers.report.id, "RESOLVED") },
                                onDismiss = { adminViewModel.updateReport(reportWithUsers.report.id, "DISMISSED") },
                                onBan = { adminViewModel.banReportedUser(reportWithUsers) }
                            )
                        }
                    }
                }
            }

            // TAB 2: Platform Settings
            else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 20.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("🛡️ Sécurité & Règles 18+", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Contrôle d'âge strict (18 ans révolus)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Rejet automatique des inscriptions de mineurs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = true, onCheckedChange = {})
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Protection des coordonnées privées", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Masquage e-mails et mots de passe dans l'API publique", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = true, onCheckedChange = {})
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📊 Version & Environnement", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Application : Gaston Love v1.0.0", fontSize = 12.sp)
                            Text("Base de données : Room SQLite Local Persistant", fontSize = 12.sp)
                            Text("Chiffrement : Sessions sécurisées", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdminUserItemCard(
    user: UserEntity,
    onToggleBan: () -> Unit,
    onToggleVerify: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (user.isBanned) Color(0xFFEF4444).copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = getDrawablePainter(user.avatarRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${user.fullName} (${user.age} ans)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (user.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                        }
                        if (user.isBanned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(color = Color(0xFFEF4444), shape = RoundedCornerShape(4.dp)) {
                                Text("SUSPENDU", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                    Text(text = "${user.email} • ${user.city}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleVerify) {
                    Text(if (user.isVerified) "Retirer badge" else "Vérifier", fontSize = 11.sp, color = GoldAccent)
                }

                TextButton(onClick = onToggleBan) {
                    Text(if (user.isBanned) "Réactiver" else "Suspendre", fontSize = 11.sp, color = if (user.isBanned) EmeraldOnline else Color(0xFFEF4444))
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AdminReportItemCard(
    report: ReportWithUsers,
    onResolve: () -> Unit,
    onDismiss: () -> Unit,
    onBan: () -> Unit
) {
    val timeFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(report.report.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "MOTIF : ${report.report.reason.uppercase()}",
                        color = Color(0xFFEF4444),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(text = timeStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Utilisateur signalé : ${report.reportedUser?.fullName ?: "Inconnu"} (${report.reportedUser?.email ?: ""})",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Text(
                text = "Signalé par : ${report.reporterUser?.fullName ?: "Anonyme"}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (report.report.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Précisions : \"${report.report.details}\"",
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBan,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("Suspendre", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onResolve,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("Résolu", fontSize = 11.sp)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Text("Classer", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}
