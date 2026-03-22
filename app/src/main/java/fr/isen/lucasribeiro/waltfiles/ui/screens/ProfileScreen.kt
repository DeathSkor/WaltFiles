package fr.isen.lucasribeiro.waltfiles.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import fr.isen.lucasribeiro.waltfiles.data.Category
import fr.isen.lucasribeiro.waltfiles.data.DatabaseService
import fr.isen.lucasribeiro.waltfiles.data.Film

@Composable
fun ProfileScreen(onLogout: () -> Unit, onNavigateBack: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    var userStatus by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var savedStatus by remember { mutableStateOf("No status yet") }
    
    val tags = listOf("Watched", "Want to watch", "Own on DVD/Blu-Ray", "Want to get rid of")
    var selectedTag by remember { mutableStateOf(tags[0]) }
    var userTags by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var allCategories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var username by remember { mutableStateOf("Loading...") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        avatarUri = uri
    }

    LaunchedEffect(Unit) {
        DatabaseService.fetchUserTags { userTags = it }
        DatabaseService.fetchCategories { allCategories = it }
        DatabaseService.fetchUsername { name ->
            username = name ?: "Anonymous"
        }
    }

    // Find all film objects that match the tagged titles
    val filteredFilms = remember(selectedTag, userTags, allCategories) {
        val taggedTitles = userTags.filter { it.value == selectedTag }.keys.map { it.replace("_", ".") }
        val results = mutableListOf<Film>()
        
        allCategories.forEach { category ->
            category.franchises?.forEach { franchise ->
                franchise.films?.forEach { film ->
                    if (taggedTitles.contains(film.titre)) results.add(film)
                }
                franchise.sous_sagas?.forEach { sousSaga ->
                    sousSaga.films?.forEach { film ->
                        if (taggedTitles.contains(film.titre)) results.add(film)
                    }
                }
            }
        }
        results.distinctBy { it.titre }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Component
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar + Name Column (Left)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUri != null) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = username,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Description Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(100.dp)
                    ) {
                        if (isEditing) {
                            OutlinedTextField(
                                value = userStatus,
                                onValueChange = { userStatus = it },
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Box(
                                    modifier = Modifier.padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = if (savedStatus.isEmpty()) "Status..." else savedStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (savedStatus.isEmpty() || savedStatus == "No status yet") 
                                            MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 4
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (isEditing) {
                                    savedStatus = userStatus
                                } else {
                                    userStatus = savedStatus
                                }
                                isEditing = !isEditing
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tag Filter Component
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "My Collection:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (filteredFilms.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No films tagged yet",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredFilms) { film ->
                            FilmCard(film = film, tag = null, onClick = { /* Navigate if needed */ })
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Logged in as: ${user?.email ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
        
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Logout")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Home")
            }
        }
    }
}
