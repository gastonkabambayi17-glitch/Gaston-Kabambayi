package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.ui.components.getDrawablePainter
import com.example.ui.theme.*
import com.example.ui.viewmodel.DatingUiState
import com.example.ui.viewmodel.DatingViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    datingViewModel: DatingViewModel,
    currentUser: UserEntity
) {
    val state by datingViewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var detailedProfile by remember { mutableStateOf<UserEntity?>(null) }

    // Swiping drag offset state
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val currentProfile = state.filteredProfiles.firstOrNull()

    if (showFilterSheet) {
        DiscoverFilterSheet(
            state = state,
            onApply = { minAge, maxAge, gender, city, interest ->
                datingViewModel.setFilters(minAge, maxAge, gender, city, interest)
                showFilterSheet = false
            },
            onReset = {
                datingViewModel.resetFilters()
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    detailedProfile?.let { profile ->
        ProfileDetailModal(
            profile = profile,
            onDismiss = { detailedProfile = null },
            onLike = {
                datingViewModel.likeUser(profile.id)
                detailedProfile = null
            },
            onPass = {
                datingViewModel.passUser(profile.id)
                detailedProfile = null
            },
            onReport = {
                datingViewModel.openReportDialog(profile)
                detailedProfile = null
            },
            onBlock = {
                datingViewModel.blockUser(profile.id)
                detailedProfile = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Découverte",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                }
                Text(
                    text = "${state.filteredProfiles.size} profil(s) disponible(s)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Filter button with active indicator
            OutlinedButton(
                onClick = { showFilterSheet = true },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.filterGender != "Tous" || state.filterCity.isNotBlank() || state.filterInterest.isNotBlank()) CrimsonPrimary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filtres",
                    modifier = Modifier.size(18.dp),
                    tint = if (state.filterGender != "Tous" || state.filterCity.isNotBlank()) CrimsonPrimary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Filtres",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Swipe Deck Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (currentProfile != null) {
                // Background Card (peek of next card if available)
                if (state.filteredProfiles.size > 1) {
                    val nextProfile = state.filteredProfiles[1]
                    Card(
                        modifier = Modifier
                            .fillMaxSize(0.95f)
                            .offset(y = 12.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = getDrawablePainter(nextProfile.avatarRes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // Front Active Swipe Card
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .rotate(offsetX / 45f)
                        .pointerInput(currentProfile.id) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (offsetX > 250f) {
                                        // Swiped Right -> Like
                                        datingViewModel.likeUser(currentProfile.id)
                                    } else if (offsetX < -250f) {
                                        // Swiped Left -> Pass
                                        datingViewModel.passUser(currentProfile.id)
                                    }
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                onDragCancel = {
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetX += dragAmount.x
                                    offsetY += dragAmount.y
                                }
                            )
                        }
                        .shadow(12.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Image
                        Image(
                            painter = getDrawablePainter(currentProfile.avatarRes),
                            contentDescription = currentProfile.fullName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Top swipe hints (LIKE / NOPE)
                        if (offsetX > 60f) {
                            Surface(
                                color = CrimsonPrimary.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(20.dp)
                                    .rotate(-12f)
                            ) {
                                Text(
                                    text = "LIKE ❤️",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        } else if (offsetX < -60f) {
                            Surface(
                                color = Color(0xFFEF4444).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(20.dp)
                                    .rotate(12f)
                            ) {
                                Text(
                                    text = "PASSER ❌",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Gradient protection for text
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color(0x33000000),
                                            Color(0xCC09090B),
                                            Color(0xF009090B)
                                        )
                                    )
                                )
                        )

                        // Online Status Badge
                        if (currentProfile.isOnline) {
                            Surface(
                                color = EmeraldOnline,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "En ligne",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Info Details at Bottom
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${currentProfile.fullName}, ${currentProfile.age}",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    )
                                    if (currentProfile.isVerified) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Vérifié",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { detailedProfile = currentProfile },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Détails",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = RoseSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${currentProfile.city}, ${currentProfile.country}",
                                    color = Color(0xFFE4E4E7),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (currentProfile.bio.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentProfile.bio,
                                    color = Color(0xFFD4D4D8),
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    lineHeight = 17.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Interest Badges
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(currentProfile.interests.take(4)) { interest ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = CrimsonPrimary.copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, CrimsonPrimary.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = interest,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty Deck State
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CrimsonPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(70.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Plus aucun profil dans vos critères",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Élargissez vos filtres d'âge, de ville ou de centres d'intérêt pour découvrir plus de célibataires.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { datingViewModel.resetFilters() },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Réinitialiser les filtres")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row (Pass, Super Like, Like)
        if (currentProfile != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pass Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .clickable { datingViewModel.passUser(currentProfile.id) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Passer",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Super Like Button
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable { datingViewModel.likeUser(currentProfile.id, isSuperLike = true) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Super Like",
                            tint = GoldAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Like Button
                Surface(
                    shape = CircleShape,
                    color = CrimsonPrimary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable { datingViewModel.likeUser(currentProfile.id) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "J'aime",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverFilterSheet(
    state: DatingUiState,
    onApply: (minAge: Int, maxAge: Int, gender: String, city: String, interest: String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var minAge by remember { mutableFloatStateOf(state.filterMinAge.toFloat()) }
    var maxAge by remember { mutableFloatStateOf(state.filterMaxAge.toFloat()) }
    var gender by remember { mutableStateOf(state.filterGender) }
    var city by remember { mutableStateOf(state.filterCity) }
    var interest by remember { mutableStateOf(state.filterInterest) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtres de recherche",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onReset) {
                    Text("Réinitialiser", color = CrimsonPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Age Range Slider
            Text(
                text = "Tranche d'âge : ${minAge.roundToInt()} - ${maxAge.roundToInt()} ans",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            RangeSlider(
                value = minAge..maxAge,
                onValueChange = { range ->
                    minAge = range.start
                    maxAge = range.endInclusive
                },
                valueRange = 18f..75f,
                steps = 56,
                colors = SliderDefaults.colors(
                    thumbColor = CrimsonPrimary,
                    activeTrackColor = CrimsonPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Gender Preference
            Text(
                text = "Je souhaite voir :",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tous", "Femme", "Homme").forEach { g ->
                    FilterChip(
                        selected = (gender == g),
                        onClick = { gender = g },
                        label = { Text(g) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // City Filter
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("Filtrer par ville") },
                placeholder = { Text("ex: Paris, Lyon, Bordeaux...") },
                leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Interest Filter
            OutlinedTextField(
                value = interest,
                onValueChange = { interest = it },
                label = { Text("Filtrer par centre d'intérêt") },
                placeholder = { Text("ex: Voyage, Cuisine, Musique...") },
                leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onApply(minAge.roundToInt(), maxAge.roundToInt(), gender, city, interest)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Appliquer les filtres", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDetailModal(
    profile: UserEntity,
    onDismiss: () -> Unit,
    onLike: () -> Unit,
    onPass: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit
) {
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
            // Profile Main Photo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Image(
                    painter = getDrawablePainter(profile.avatarRes),
                    contentDescription = profile.fullName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name & Age Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${profile.fullName}, ${profile.age}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (profile.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Vérifié",
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "📍 ${profile.city}, ${profile.country}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                if (profile.isOnline) {
                    Surface(
                        color = EmeraldOnline,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "En ligne",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bio Section
            Text(
                text = "À propos de moi",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = profile.bio.ifBlank { "Aucune description pour le moment." },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interests Section
            Text(
                text = "Centres d'intérêt",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(profile.interests) { interest ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(interest) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons (Like / Pass)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onPass,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Passer", color = Color(0xFFEF4444))
                }

                Button(
                    onClick = onLike,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("J'aime", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Safety Actions (Report / Block)
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onReport) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Signaler ce profil", color = CrimsonPrimary, fontSize = 12.sp)
                }

                TextButton(onClick = onBlock) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bloquer", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}
