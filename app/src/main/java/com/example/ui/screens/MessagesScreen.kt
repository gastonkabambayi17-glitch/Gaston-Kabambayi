package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MessageEntity
import com.example.data.local.UserEntity
import com.example.ui.components.getDrawablePainter
import com.example.ui.theme.*
import com.example.ui.viewmodel.DatingViewModel
import com.example.ui.viewmodel.MatchWithUser
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    datingViewModel: DatingViewModel,
    currentUser: UserEntity,
    onNavigateToDiscover: () -> Unit
) {
    val state by datingViewModel.uiState.collectAsState()
    val activeChat = state.activeChatMatch

    if (activeChat != null) {
        // Individual Conversation View
        ChatRoomView(
            matchWithUser = activeChat,
            currentUser = currentUser,
            messages = state.chatMessages,
            onBack = { datingViewModel.closeChat() },
            onSendMessage = { text, media -> datingViewModel.sendMessage(text, media) },
            onDeleteConversation = { datingViewModel.deleteConversation(activeChat.match.id) },
            onReportUser = { datingViewModel.openReportDialog(activeChat.otherUser) },
            onBlockUser = { datingViewModel.blockUser(activeChat.otherUser.id) }
        )
    } else {
        // Conversations List
        ConversationsListView(
            matches = state.matches,
            onOpenChat = { datingViewModel.openChat(it) },
            onNavigateToDiscover = onNavigateToDiscover
        )
    }
}

@Composable
private fun ConversationsListView(
    matches: List<MatchWithUser>,
    onOpenChat: (MatchWithUser) -> Unit,
    onNavigateToDiscover: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredMatches = matches.filter {
        searchQuery.isBlank() || it.otherUser.fullName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
                tint = CrimsonPrimary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Messagerie Privée",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher une conversation...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = CrimsonPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Aucune discussion active",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Les conversations s'ouvrent dès que vous avez un Match mutuel. Allez faire de nouvelles rencontres !",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onNavigateToDiscover,
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Découvrir des profils")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMatches) { matchWithUser ->
                    ConversationItemCard(
                        matchWithUser = matchWithUser,
                        onClick = { onOpenChat(matchWithUser) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItemCard(
    matchWithUser: MatchWithUser,
    onClick: () -> Unit
) {
    val user = matchWithUser.otherUser
    val lastMsg = matchWithUser.lastMessage

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Online dot
            Box(modifier = Modifier.size(54.dp)) {
                Image(
                    painter = getDrawablePainter(user.avatarRes),
                    contentDescription = user.fullName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(1.5.dp, CrimsonPrimary, CircleShape),
                    contentScale = ContentScale.Crop
                )
                if (user.isOnline) {
                    Surface(
                        color = EmeraldOnline,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                    ) {}
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (user.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (lastMsg != null) {
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        Text(
                            text = timeFormat.format(Date(lastMsg.timestamp)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = lastMsg?.content ?: "Nouveau Match ! Dites bonjour 👋",
                    fontSize = 13.sp,
                    color = if (lastMsg != null) MaterialTheme.colorScheme.onSurfaceVariant else CrimsonPrimary,
                    maxLines = 1,
                    fontWeight = if (lastMsg == null) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatRoomView(
    matchWithUser: MatchWithUser,
    currentUser: UserEntity,
    messages: List<MessageEntity>,
    onBack: () -> Unit,
    onSendMessage: (content: String, media: String?) -> Unit,
    onDeleteConversation: () -> Unit,
    onReportUser: () -> Unit,
    onBlockUser: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showPhotoPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val user = matchWithUser.otherUser

    val quickEmojis = listOf("❤️", "🔥", "😍", "🌹", "✨", "😊", "🥂", "💋")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp)) {
                            Image(
                                painter = getDrawablePainter(user.avatarRes),
                                contentDescription = user.fullName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            if (user.isOnline) {
                                Surface(
                                    color = EmeraldOnline,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(Alignment.BottomEnd)
                                ) {}
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (user.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (user.isOnline) "En ligne actuellement" else "Vu récemment",
                                fontSize = 11.sp,
                                color = if (user.isOnline) EmeraldOnline else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Signaler ce profil") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = CrimsonPrimary) },
                            onClick = {
                                showMenu = false
                                onReportUser()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bloquer l'utilisateur") },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onBlockUser()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Supprimer la conversation") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = {
                                showMenu = false
                                onDeleteConversation()
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            // Messages Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Welcome Match Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CrimsonPrimary.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✨ Vous avez matché avec ${user.fullName} !",
                                fontWeight = FontWeight.Bold,
                                color = CrimsonPrimary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Gaston Love protège vos échanges. Restez respectueux et courtois.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                items(messages) { msg ->
                    val isMine = (msg.senderId == currentUser.id)
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val timeStr = timeFormat.format(Date(msg.timestamp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Column(
                            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMine) 16.dp else 4.dp,
                                    bottomEnd = if (isMine) 4.dp else 16.dp
                                ),
                                color = if (isMine) CrimsonPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Optional attached photo
                                    msg.mediaAttachmentRes?.let { mediaRes ->
                                        Image(
                                            painter = getDrawablePainter(mediaRes),
                                            contentDescription = "Photo partagée",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                    if (msg.content.isNotBlank()) {
                                        Text(
                                            text = msg.content,
                                            color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            Text(
                                text = timeStr,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Quick Emojis Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickEmojis) { emoji ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                onSendMessage(emoji, null)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 18.sp)
                        }
                    }
                }
            }

            // Photo attachment selector dialog
            if (showPhotoPicker) {
                AlertDialog(
                    onDismissRequest = { showPhotoPicker = false },
                    title = { Text("Partager une photo", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Sélectionnez une photo à envoyer dans la discussion :", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Card(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clickable {
                                            onSendMessage("", "img_landing_couple")
                                            showPhotoPicker = false
                                        },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Image(
                                        painter = getDrawablePainter("img_landing_couple"),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Card(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clickable {
                                            onSendMessage("", "img_profile_sophie")
                                            showPhotoPicker = false
                                        },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Image(
                                        painter = getDrawablePainter("img_profile_sophie"),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showPhotoPicker = false }) {
                            Text("Annuler")
                        }
                    }
                )
            }

            // Input Bar
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showPhotoPicker = true }) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Joindre une photo",
                            tint = CrimsonPrimary
                        )
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Votre message...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                onSendMessage(messageText, null)
                                messageText = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(CrimsonPrimary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Envoyer",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
