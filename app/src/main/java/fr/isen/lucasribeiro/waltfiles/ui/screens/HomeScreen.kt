package fr.isen.lucasribeiro.waltfiles.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onNavigateToProfile: () -> Unit, onNavigateToExplore: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to WaltFiles", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigateToExplore,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text(text = "Explore Movies")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToProfile,
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text(text = "Go to Profile")
        }
    }
}
