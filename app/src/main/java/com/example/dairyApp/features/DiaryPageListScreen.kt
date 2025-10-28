package com.example.dairyApp.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.example.dairyApp.Screen
import com.example.dairyApp.diary.DiaryPageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryPageListScreen(
    navController: NavController,
    eventId: String,
    navBackStackEntry: NavBackStackEntry,
    diaryPageViewModel: DiaryPageViewModel
) {
    val uiState by diaryPageViewModel.uiState.collectAsState()
    var pageToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.event?.name ?: "日记") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.PhotoCaption.createRoute(eventId)) }) {
                Icon(Icons.Filled.Add, contentDescription = "添加新的日记条目")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = "错误: ${uiState.error}",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.diaryPageNames.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "这个事件还没有任何日记页。")
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.diaryPageNames) { pageName ->
                            ListItem(
                                headlineContent = { Text(pageName) },
                                modifier = Modifier.clickable {
                                    navController.navigate(Screen.DiaryEntryList.createRoute(eventId, pageName))
                                },
                                trailingContent = {
                                    IconButton(onClick = { pageToDelete = pageName }) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除日记页")
                                    }
                                }
                            )
                            Divider()
                        }
                    }
                }
            }
        }

        pageToDelete?.let { pageName ->
            DeleteConfirmationDialog(
                onConfirm = { 
                    diaryPageViewModel.deletePage(pageName)
                    pageToDelete = null
                },
                onDismiss = { pageToDelete = null },
                itemName = "日记页"
            )
        }
    }
}
