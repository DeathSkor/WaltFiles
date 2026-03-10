package fr.isen.lucasribeiro.waltfiles.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun ExploreScreen(onNavigateBack: () -> Unit) {
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

    // Flatten all films for search
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
                            placeholder = { Text("Search films...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
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
                TopAppBar(
                    title = {
                        Text(
                            when (val level = currentLevel) {
                                is ExploreLevel.Categories -> "Categories"
                                is ExploreLevel.Franchises -> level.category.categorie ?: "Franchises"
                                is ExploreLevel.Films -> level.franchise.nom ?: "Films"
                                is ExploreLevel.FilmInfo -> level.film.titre ?: "Film Details"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (levelStack.isNotEmpty()) {
                                currentLevel = levelStack.removeAt(levelStack.size - 1)
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }
        }
    ) { padding ->
        val safeCategories = categories
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isSearchActive && searchQuery.isNotEmpty()) {
                if (filteredFilms.isEmpty()) {
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        Text("No films match your search.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredFilms) { film ->
                            val sanitizedTitle = film.titre?.replace(".", "_")?.replace("#", "_")?.replace("$", "_")?.replace("[", "_")?.replace("]", "_") ?: ""
                            FilmItem(film, userTags[sanitizedTitle]) {
                                levelStack.add(currentLevel)
                                currentLevel = ExploreLevel.FilmInfo(film)
                                isSearchActive = false
                                searchQuery = ""
                            }
                            HorizontalDivider()
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
                        CategoryGrid(safeCategories) { category ->
                            levelStack.add(currentLevel)
                            currentLevel = ExploreLevel.Franchises(category)
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
                    .clickable { onCategoryClick(category) },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.categorie ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FranchiseList(franchises: List<Franchise>, onFranchiseClick: (Franchise) -> Unit) {
    LazyColumn {
        items(franchises) { franchise ->
            ListItem(
                headlineContent = { Text(franchise.nom ?: "Unknown", fontWeight = FontWeight.Medium) },
                modifier = Modifier.clickable { onFranchiseClick(franchise) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun FilmItem(film: Film, tag: String?, onClick: () -> Unit) {
    val placeholder = painterResource(id = R.drawable.`cat`)

    ListItem(
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(film.titre ?: "Unknown Film")
                if (tag != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = tag,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        supportingContent = { Text("${film.annee} • ${film.genre}") },
        trailingContent = { film.numero?.let { Text("#$it") } },
        leadingContent = {
            if (!film.image.isNullOrEmpty()) {
                AsyncImage(
                    model = film.image,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = placeholder,
                    error = placeholder
                )
            } else {
                Image(
                    painter = placeholder,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        },
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    )
}

@Composable
fun FilmDetailList(franchise: Franchise, userTags: Map<String, String>, onFilmClick: (Film) -> Unit) {
    LazyColumn {
        val directFilms = franchise.films
        if (directFilms != null && directFilms.isNotEmpty()) {
            val sortedDirectFilms = directFilms.sortedWith(compareBy({ it.numero }, { it.annee }))
            items(sortedDirectFilms) { film ->
                val sanitizedTitle = film.titre?.replace(".", "_")?.replace("#", "_")?.replace("$", "_")?.replace("[", "_")?.replace("]", "_") ?: ""
                FilmItem(film, userTags[sanitizedTitle]) { onFilmClick(film) }
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
            }
        }

        franchise.sous_sagas?.forEach { sousSaga ->
            val films = sousSaga.films
            if (films != null && films.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = sousSaga.nom ?: "Unnamed Section",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                val sortedFilms = films.sortedWith(compareBy({ it.numero }, { it.annee }))
                
                items(sortedFilms) { film ->
                    val sanitizedTitle = film.titre?.replace(".", "_")?.replace("#", "_")?.replace("$", "_")?.replace("[", "_")?.replace("]", "_") ?: ""
                    FilmItem(film, userTags[sanitizedTitle]) { onFilmClick(film) }
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
        
        val hasNoFilms = (directFilms == null || directFilms.isEmpty()) && 
                        (franchise.sous_sagas?.none { it.films?.isNotEmpty() == true } ?: true)

        if (hasNoFilms) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No films found for this franchise.")
                }
            }
        }
    }
}

@Composable
fun FilmInfoPage(film: Film, currentTag: String?, onTagSelected: (String?) -> Unit) {
    val placeholder = painterResource(id = R.drawable.`cat`)
    val tags = listOf("Watched", "Want to watch", "Own on DVD/Blu-Ray", "Want to get rid of")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!film.image.isNullOrEmpty()) {
            AsyncImage(
                model = film.image,
                contentDescription = film.titre,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit,
                placeholder = placeholder,
                error = placeholder
            )
        } else {
            Image(
                painter = placeholder,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = film.titre ?: "Unknown",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "My Status",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
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

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Film Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(label = "Title", value = film.titre ?: "N/A")
                InfoRow(label = "Release Year", value = film.annee?.toString() ?: "N/A")
                InfoRow(label = "Genre", value = film.genre ?: "N/A")
                film.numero?.let { InfoRow(label = "Sequence Number", value = it.toString()) }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
