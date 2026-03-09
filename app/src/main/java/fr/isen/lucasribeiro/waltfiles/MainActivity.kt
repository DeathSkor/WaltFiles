package fr.isen.lucasribeiro.waltfiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import fr.isen.lucasribeiro.waltfiles.ui.LoginScreen
import fr.isen.lucasribeiro.waltfiles.ui.theme.WaltFilesTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaltFilesTheme {
                var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isLoggedIn) {
                        Greeting(
                            name = FirebaseAuth.getInstance().currentUser?.email ?: "User",
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        Box(modifier = Modifier.padding(innerPadding)) {
                            LoginScreen(onLoginSuccess = {
                                isLoggedIn = true
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
