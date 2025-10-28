package com.example.dairyApp.features

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dairyApp.diary.EventViewModel // Updated import if ViewModel file is also renamed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    navController: NavController,
    eventViewModel: EventViewModel = viewModel( // Use EventViewModel type
        factory = EventViewModel.provideFactory(LocalContext.current.applicationContext as Application)
    )
) {
    var eventName by remember { mutableStateOf("") } 

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("创建事件") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = eventName,
                onValueChange = { eventName = it },
                label = { Text("事件名称") }, 
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (eventName.isNotBlank()) {
                        eventViewModel.createEvent(eventName) // Corrected method call
                        navController.popBackStack() 
                    }
                },
                enabled = eventName.isNotBlank()
            ) {
                Text("创建") 
            }
        }
    }
}
