package com.example.dairyApp.diary

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryPageScreen( // Renamed from TripDetailScreen
    eventId: String, // Renamed from tripId
    navController: NavController,
    diaryPageViewModel: DiaryPageViewModel = viewModel( // Use DiaryPageViewModel
        factory = DiaryPageViewModel.provideFactory(
            application = LocalContext.current.applicationContext as Application,
            owner = navController.currentBackStackEntry!!,
            eventId = eventId
        )
    )
) {
    val uiState by diaryPageViewModel.uiState.collectAsState()
    val event = uiState.event // Use event from uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(event?.name ?: "日记页详情") }, // Updated title
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
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
                event == null -> {
                    Text(
                        text = "未找到此事件记录。", // Updated text
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            val coverImageUri = event.coverImageUri
                            if (coverImageUri != null) {
                                AsyncImage(
                                    model = coverImageUri,
                                    contentDescription = "${event.name} cover image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        val entries = uiState.entries // Corrected to use uiState.entries
                        if (entries.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "这个日记页还没有任何条目。", // Updated text
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        } else {
                            items(entries) { entry ->
                                DiaryEntryCard(
                                    entry = entry, 
                                    onClick = {
                                        // TODO: Navigate to an Entry detail screen or edit screen if needed
                                    },
                                    onDeleteClick = { /* This screen doesn't support deletion, so no-op */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiaryEntryCard(
    entry: DiaryEntry, 
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
            Column(modifier = Modifier.padding(16.dp)) {
                if (entry.imageUris.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(entry.imageUris.take(3)) { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Diary entry image",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (entry.imageUris.size > 3) {
                            item { 
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+${entry.imageUris.size - 3}", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(text = entry.caption, style = MaterialTheme.typography.bodyLarge, maxLines = 3)
                Spacer(modifier = Modifier.height(8.dp))

                val diaryPageName = entry.diaryPageName // Use the correct property name
                if (diaryPageName != null) {
                    Text(text = "日记页: $diaryPageName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = "记录于: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entry.timestamp))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "删除条目")
            }
        }
    }
}
