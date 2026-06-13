package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.NeighborCartDatabase
import com.example.data.NeighborCartRepository
import com.example.ui.NeighborCartApp
import com.example.ui.NeighborCartViewModel
import com.example.ui.NeighborCartViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Spin up database and repository
        val database = NeighborCartDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = NeighborCartRepository(database)
        
        // Instantiate the ViewModel using custom factory
        val viewModel = ViewModelProvider(
            this,
            NeighborCartViewModelFactory(application, repository)
        )[NeighborCartViewModel::class.java]

        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                NeighborCartApp(viewModel = viewModel)
            }
        }
    }
}
