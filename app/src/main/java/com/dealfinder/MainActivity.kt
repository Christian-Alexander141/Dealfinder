package com.dealfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dealfinder.di.AppModule
import com.dealfinder.ui.components.MainAppScaffold
import com.dealfinder.ui.theme.DealfinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize AppModule
        AppModule.initialize(this)
        
        setContent {
            DealfinderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainAppScaffold()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DealfinderTheme {
        Surface {
            // Preview content
        }
    }
}
