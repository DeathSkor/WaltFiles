package fr.isen.lucasribeiro.waltfiles.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import fr.isen.lucasribeiro.waltfiles.R
import fr.isen.lucasribeiro.waltfiles.data.*

sealed class ExploreLevel {
    object Categories : ExploreLevel()
    data class Franchises(val category: Category) : ExploreLevel()
    data class Films(val franchise: Franchise) : ExploreLevel()
    data class FilmInfo(val film: Film) : ExploreLevel()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onNavigateToProfile: () -> Unit, onNavigateBack: () -> Unit) {
    var categories by remember { mutableStateOf<List<Category>?>(null) }
    var currentLevel by remember { mutableStateOf<ExploreLevel>(ExploreLevel.Categories) }
    val levelStack = remember { mutableStateListOf<ExploreLevel>() }
    var userTags by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        DatabaseService.fetchCategories {
            categories = it
        }
        DatabaseService.fetchUserTags {
            userTags = it
        }
    }

    val allFilms = remember(categories) {
        categories?.flatMap { category ->
            category.franchises?.flatMap { franchise ->
                val films = franchise.films?.toMutableList() ?: mutableListOf()
                franchise.sous_sagas?.forEach { saga ->
                    saga.films?.let { films.addAll(it) }
                }
                films
            } ?: emptyList()
        }?.distinctBy { it.titre } ?: emptyList()
    }

    val filteredFilms = remember(searchQuery, allFilms) {
        if (searchQuery.isEmpty()) emptyList()
        else allFilms.filter { it.titre?.contains(searchQuery, ignoreCase = true) == true }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search films...", style = MaterialTheme.typography.bodyLarge) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (val level = currentLevel) {
                                is ExploreLevel.Categories -> "WALT FILES"
                                is ExploreLevel.Franchises -> level.category.categorie ?: "Franchises"
                                is ExploreLevel.Films -> level.franchise.nom ?: "Films"
                                is ExploreLevel.FilmInfo -> level.film.titre ?: "Film Details"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                        )
                    },
                    navigationIcon = {
                        if (currentLevel is ExploreLevel.Categories) {
                            IconButton(onClick = onNavigateToProfile) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            IconButton(onClick = {
                                if (levelStack.isNotEmpty()) {
                                    currentLevel = levelStack.removeAt(levelStack.size - 1)
                                } else {
                                    onNavigateBack()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { padding ->
        val safeCategories = categories
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)) {
            if (isSearchActive && searchQuery.isNotEmpty()) {
                if (filteredFilms.isEmpty()) {
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        Text("No films match your search.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredFilms) { film ->
                            val sanitizedTitle = film.titre?.replace(".", "_")?.replace("#", "_")?.replace("$", "_")?.replace("[", "_")?.replace("]", "_") ?: ""
                            FilmCard(film, userTags[sanitizedTitle]) {
                                levelStack.add(currentLevel)
                                currentLevel = ExploreLevel.FilmInfo(film)
                                isSearchActive = false
                                searchQuery = ""
                            }
                        }
                    }
                }
            } else if (safeCategories == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (safeCategories.isEmpty()) {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    Text("No categories found.")
                }
            } else {
                when (val level = currentLevel) {
                    is ExploreLevel.Categories -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                            )
                            CategoryGrid(safeCategories) { category ->
                                levelStack.add(currentLevel)
                                currentLevel = ExploreLevel.Franchises(category)
                            }
                        }
                    }
                    is ExploreLevel.Franchises -> {
                        FranchiseList(level.category.franchises ?: emptyList()) { franchise ->
                            levelStack.add(currentLevel)
                            currentLevel = ExploreLevel.Films(franchise)
                        }
                    }
                    is ExploreLevel.Films -> {
                        FilmDetailList(level.franchise, userTags) { film ->
                            levelStack.add(currentLevel)
                            currentLevel = ExploreLevel.FilmInfo(film)
                        }
                    }
                    is ExploreLevel.FilmInfo -> {
                        val sanitizedTitle = level.film.titre?.replace(".", "_")?.replace("#", "_")?.replace("$", "_")?.replace("[", "_")?.replace("]", "_") ?: ""
                        val currentTag = userTags[sanitizedTitle]
                        
                        FilmInfoPage(level.film, currentTag) { newTag ->
                            DatabaseService.saveUserTag(level.film.titre ?: "", newTag)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryGrid(categories: List<Category>, onCategoryClick: (Category) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(categories) { category ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { onCategoryClick(category) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = category.categorie ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                        Surface(
                            modifier = Modifier.size(4.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
fun FranchiseList(franchises: List<Franchise>, onFranchiseClick: (Franchise) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(franchises) { franchise ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFranchiseClick(franchise) },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                ListItem(
                    headlineContent = { 
                        Text(
                            franchise.nom ?: "Unknown", 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        ) 
                    },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp).rotate(180f)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun FilmCard(film: Film, tag: String?, onClick: () -> Unit) {
    val placeholder = painterResource(id = R.drawable.`cat`)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = film.image,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp, 120.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = placeholder,
                error = placeholder
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = film.titre ?: "Unknown Film",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${film.annee} • ${film.genre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (tag != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text(tag, fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = null
                    )
                }
            }
        }
    }
}

@Composable
fun FilmDetailList(franchise: Franchise, userTags: Map<String, String>, onFilmClick: (Film) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val directFilms = franchise.films
        if (directFilms != null && directFilms.isNotEmpty()) {
            val sortedDirectFilms = directFilms.sortedWith(compareBy({ it.numero }, { it.annee }))
            items(sortedDirectFilms) { film ->
                val sanitizedTitle = film.titre?.replace(".", "_")?.replace("#", "_")?.replace("$", "_")?.replace("[", "_")?.replace("]" , "_") ?: ""
                FilmCard(film, userTags[sanitizedTitle]) { onFilmClick(film) }
            }
        }

        franchise.sous_sagas?.forEach { sousSaga ->
            val films = sousSaga.films
            if (films != null && films.isNotEmpty()) {
                item {
                    Text(
                        text = sousSaga.nom ?: "Unnamed Section",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                val sortedFilms = films.sortedWith(compareBy({ it.numero }, { it.annee }))
                
                items(sortedFilms) { film ->
                    val sanitizedTitle = film.titre?.replace(".", "_")?.replace("#", "_")?.replace("$", "_")?.replace("[", "_")?.replace("]", "_") ?: ""
                    FilmCard(film, userTags[sanitizedTitle]) { onFilmClick(film) }
                }
            }
        }
    }
}

@Composable
fun FilmInfoPage(film: Film, currentTag: String?, onTagSelected: (String?) -> Unit) {
    val placeholder = painterResource(id = R.drawable.`cat`)
    val tags = listOf("Watched", "Want to watch", "Own on DVD-Blu-Ray", "Want to get rid of")
    var globalStats by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var selectedUsersList by remember { mutableStateOf<Pair<String, List<String>>?>(null) }

    LaunchedEffect(film.titre) {
        DatabaseService.fetchGlobalTagStats(film.titre ?: "") { stats ->
            globalStats = stats
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)) {
            AsyncImage(
                model = film.image,
                contentDescription = film.titre,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = placeholder,
                error = placeholder
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 300f
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = film.titre ?: "Unknown",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
                Text(
                    text = "${film.annee} • ${film.genre}",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White.copy(alpha = 0.8f))
                )
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Community Stats",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.filter { it != "Watched" && it != "Want to watch" }.forEach { tag ->
                    val users = globalStats[tag] ?: emptyList()
                    FilterChip(
                        selected = false,
                        onClick = { if (users.isNotEmpty()) selectedUsersList = tag to users },
                        label = { Text("$tag (${users.size})", style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "My Status",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    FilterChip(
                        selected = currentTag == tag,
                        onClick = { 
                            if (currentTag == tag) onTagSelected(null)
                            else onTagSelected(tag)
                        },
                        label = { Text(tag) },
                        leadingIcon = if (currentTag == tag) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Detailed Info",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(label = "Original Title", value = film.titre ?: "N/A")
                    InfoRow(label = "Release Year", value = film.annee?.toString() ?: "N/A")
                    InfoRow(label = "Genre", value = film.genre ?: "N/A")
                    film.numero?.let { InfoRow(label = "Sequence", value = "#$it") }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    selectedUsersList?.let { pair ->
        val tag = pair.first
        val users = pair.second
        AlertDialog(
            onDismissRequest = { selectedUsersList = null },
            title = { Text("Community: $tag", style = MaterialTheme.typography.titleLarge) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(users) { userEmail ->
                        ListItem(
                            headlineContent = { Text(userEmail) },
                            leadingContent = { 
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(4.dp).size(20.dp))
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedUsersList = null }) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}
