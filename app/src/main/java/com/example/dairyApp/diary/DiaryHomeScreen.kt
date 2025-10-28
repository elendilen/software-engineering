package com.example.dairyApp.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage // For displaying cover images
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryHomeScreen(
    diaryViewModel: DiaryViewModel = viewModel(),
    onEventClick: (String) -> Unit, // Renamed from onTripClick
) {
    val uiState by diaryViewModel.diaryHomeUiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的图片日记") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "创建新的日记事件")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = "加载失败: ${uiState.error}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else if (uiState.events.isEmpty()) { // Changed to use uiState.events
                Text(
                    text = "还没有日记。点击 ‘+’ 创建一个吧！",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.events) { event -> // Changed to iterate over events
                        DiaryEventCard(
                            event = event, 
                            onClick = { onEventClick(event.id) },
                            onDeleteClick = { /* Obsolete screen, no-op */ }
                        )
                    }
                }
            }

            if (showDialog) {
                CreateEventDialog( // Renamed to CreateEventDialog
                    onDismissRequest = { showDialog = false },
                    onConfirm = { eventName -> // Renamed to eventName
                        diaryViewModel.addEvent(eventName) // Use addEvent method
                        showDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun DiaryEventCard(
    event: DiaryEvent, 
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) { 
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                event.coverImageUri?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = "${event.name} cover image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = event.name, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "创建于: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(event.timestamp))}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "包含 ${event.entryIds.size} 个条目",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "删除事件")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventDialog( // Renamed from CreateTripDialog
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var eventName by remember { mutableStateOf("") } // Renamed to eventName

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("创建新的日记事件") },
        text = {
            OutlinedTextField(
                value = eventName,
                onValueChange = { eventName = it },
                label = { Text("事件名称") }, // Updated label
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (eventName.isNotBlank()) {
                        onConfirm(eventName)
                    }
                }
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            Button(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}
