package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.ui.components.CommunityGuidelinesSheet
import com.example.ui.components.getDrawablePainter
import com.example.ui.theme.*
import com.example.ui.viewmodel.DatingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    currentUser: UserEntity,
    datingViewModel: DatingViewModel,
    onLogout: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showGuidelines by remember { mutableStateOf(false) }
    var showPrivacyNotice by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    if (showGuidelines) {
        CommunityGuidelinesSheet(onDismiss = { showGuidelines = false })
    }

    if (showPrivacyNotice) {
        AlertDialog(
            onDismissRequest = { showPrivacyNotice = false },
            title = { Text("Protection des Données Personnelles", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Sur Gaston Love, votre vie privée est une priorité absolue :\n\n" +
                                "• Votre adresse e-mail et votre mot de passe ne sont JAMAIS affichés publiquement.\n" +
                                "• Les échanges et messages privés sont strictement confidentiels.\n" +
                                "• Vous pouvez supprimer votre compte ou bloquer n'importe quel membre en 1 clic.\n" +
                                "• Conforme aux standards de sécurité et réservé aux adultes de 18 ans et plus.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyNotice = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Compris")
                }
            }
        )
    }

    if (showEditProfileSheet) {
        EditProfileModal(
            currentUser = currentUser,
            onDismiss = { showEditProfileSheet = false },
            onSave = { bio, city, interests, extraPhotos, interestedIn ->
                datingViewModel.updateProfile(bio, city, interests, extraPhotos, interestedIn)
                showEditProfileSheet = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Title
        Text(
            text = "Mon Profil",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Profile Card Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar with verification badge
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(3.dp, CrimsonPrimary, CircleShape)
                ) {
                    Image(
                        painter = getDrawablePainter(currentUser.avatarRes),
                        contentDescription = currentUser.fullName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${currentUser.fullName}, ${currentUser.age}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    if (currentUser.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Profil vérifié",
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "📍 ${currentUser.city}, ${currentUser.country}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showEditProfileSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Modifier mon profil", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bio Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ma Description",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentUser.bio.ifBlank { "Ajoutez une description pour que les autres membres vous connaissent mieux." },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Centres d'intérêt",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(currentUser.interests) { interest ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(interest, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Photos Gallery Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mes Photos de Profil",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = getDrawablePainter(currentUser.avatarRes),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Card(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = getDrawablePainter("img_landing_couple"),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Menu Actions List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                ListItem(
                    headlineContent = { Text("Charte communautaire & Règles 18+") },
                    leadingContent = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = CrimsonPrimary) },
                    modifier = Modifier.clickable { showGuidelines = true }
                )
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Protection des Données Personnelles") },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null, tint = RoseSecondary) },
                    modifier = Modifier.clickable { showPrivacyNotice = true }
                )

                // Admin portal link if user is admin or for testing
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Espace Administration & Modération") },
                    supportingContent = { Text("Gestion utilisateurs, statistiques et signalements") },
                    leadingContent = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldAccent) },
                    modifier = Modifier.clickable { onOpenAdmin() }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Logout Button
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
        ) {
            Icon(imageVector = Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Se déconnecter", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileModal(
    currentUser: UserEntity,
    onDismiss: () -> Unit,
    onSave: (bio: String, city: String, interests: List<String>, extraPhotos: List<String>, interestedIn: String) -> Unit
) {
    var bio by remember { mutableStateOf(currentUser.bio) }
    var city by remember { mutableStateOf(currentUser.city) }
    var interestedIn by remember { mutableStateOf(currentUser.interestedIn) }
    var selectedInterests by remember { mutableStateOf(currentUser.interests) }

    val allInterests = listOf(
        "Voyage", "Cuisine", "Musique", "Cinéma", "Photographie",
        "Sport", "Art & Musées", "Lecture", "Nature", "Mode",
        "Technologie", "Soirées", "Randonnée", "Yoga", "Gastronomie"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Modifier mon profil",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("Ville") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Description / Bio") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(text = "Je cherche :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Homme", "Femme", "Tous").forEach { i ->
                    FilterChip(
                        selected = (interestedIn == i),
                        onClick = { interestedIn = i },
                        label = { Text(i) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(text = "Centres d'intérêt :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(allInterests) { interest ->
                    val isSelected = selectedInterests.contains(interest)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedInterests = if (isSelected) {
                                selectedInterests - interest
                            } else {
                                selectedInterests + interest
                            }
                        },
                        label = { Text(interest) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(bio, city, selectedInterests, currentUser.extraPhotos, interestedIn)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Enregistrer les modifications", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
