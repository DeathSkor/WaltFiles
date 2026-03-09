package fr.isen.lucasribeiro.waltfiles.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import fr.isen.lucasribeiro.waltfiles.R
import fr.isen.lucasribeiro.waltfiles.data.*

sealed class ExploreLevel {
    object Categories : ExploreLevel()
    data class Franchises(val category: Category) : ExploreLevel()
    data class Films(val franchise: Franchise) : ExploreLevel()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(onNavigateBack: () -> Unit) {
    var categories by remember { mutableStateOf<List<Category>?>(null) }
    var currentLevel by remember { mutableStateOf<ExploreLevel>(ExploreLevel.Categories) }
    val levelStack = remember { mutableStateListOf<ExploreLevel>() }

    LaunchedEffect(Unit) {
        DatabaseService.fetchCategories {
            categories = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (val level = currentLevel) {
                            is ExploreLevel.Categories -> "Categories"
                            is ExploreLevel.Franchises -> level.category.categorie ?: "Franchises"
                            is ExploreLevel.Films -> level.franchise.nom ?: "Films"
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
                }
            )
        }
    ) { padding ->
        val safeCategories = categories
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (safeCategories == null) {
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
                        FilmDetailList(level.franchise)
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
fun FilmItem(film: Film) {
    // Accessing resource with hyphen in name requires R.drawable.`name`
    val placeholder = painterResource(id = R.drawable.`cat`)

    ListItem(
        headlineContent = { Text(film.titre ?: "Unknown Film") },
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
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun FilmDetailList(franchise: Franchise) {
    LazyColumn {
        val directFilms = franchise.films
        if (directFilms != null && directFilms.isNotEmpty()) {
            val sortedDirectFilms = directFilms.sortedWith(compareBy({ it.numero }, { it.annee }))
            items(sortedDirectFilms) { film ->
                FilmItem(film)
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
                    FilmItem(film)
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
