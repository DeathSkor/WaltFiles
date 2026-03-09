package fr.isen.lucasribeiro.waltfiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.isen.lucasribeiro.waltfiles.ui.screens.LoginScreen
import fr.isen.lucasribeiro.waltfiles.ui.screens.HomeScreen
import fr.isen.lucasribeiro.waltfiles.ui.screens.ProfileScreen
import fr.isen.lucasribeiro.waltfiles.ui.theme.WaltFilesTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaltFilesTheme {
                val navController = rememberNavController()
                var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (!isLoggedIn) {
                            LoginScreen(onLoginSuccess = {
                                isLoggedIn = true
                            })
                        } else {
                            NavHost(navController = navController, startDestination = "home") {
                                composable("home") {
                                    HomeScreen(onNavigateToProfile = {
                                        navController.navigate("profile")
                                    })
                                }
                                composable("profile") {
                                    ProfileScreen(
                                        onLogout = {
                                            isLoggedIn = false
                                        },
                                        onNavigateBack = {
                                            navController.popBackStack()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
