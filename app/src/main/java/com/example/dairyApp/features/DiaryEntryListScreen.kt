package com.example.dairyApp.features

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dairyApp.diary.DiaryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEntryListScreen(
    navController: NavController,
    eventId: String,
    diaryPageName: String,
    navBackStackEntry: NavBackStackEntry
) {
    val viewModel: DiaryEntryListViewModel = viewModel(
        modelClass = DiaryEntryListViewModel::class.java,
        viewModelStoreOwner = navBackStackEntry,
        factory = DiaryEntryListViewModel.provideFactory(
            application = LocalContext.current.applicationContext as Application,
            owner = navBackStackEntry,
            eventId = eventId,
            diaryPageName = diaryPageName
        )
    )

    val entries by viewModel.entries.collectAsState()
    var entryToDelete by remember { mutableStateOf<DiaryEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(diaryPageName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
        ,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Create a new entry directly inside this diary page
                navController.navigate(
                    com.example.dairyApp.Screen.PhotoCaption.createRoute(
                        eventId = eventId,
                        diaryPageName = diaryPageName
                    )
                )
            }) {
                Icon(Icons.Filled.Add, contentDescription = "添加新的日记条目")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "这个日记页还没有任何条目。")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(entries) { entry ->
                        EntryCard(
                            entry = entry,
                            onClick = {
                                navController.navigate(
                                    com.example.dairyApp.Screen.PhotoCaption.createRoute(
                                        eventId = eventId,
                                        diaryPageName = diaryPageName,
                                        entryId = entry.id
                                    )
                                )
                            },
                            onDeleteClick = { entryToDelete = entry }
                        )
                    }
                }
            }
        }

        entryToDelete?.let { entry ->
            DeleteConfirmationDialog(
                onConfirm = {
                    viewModel.delete(entry)
                    entryToDelete = null
                },
                onDismiss = { entryToDelete = null },
                itemName = "日记条目"
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    itemName: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("您确定要删除这个 $itemName 吗？此操作无法撤销。") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun EntryCard(entry: DiaryEntry, onClick: () -> Unit, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // images
            if (entry.imageUris.isNotEmpty()) {
                val images = entry.imageUris.map { Uri.parse(it) }
                val maxImages = minOf(images.size, 9)
                val displayImages = images.take(maxImages)

                if (displayImages.size == 1) {
                    AsyncImage(
                        model = displayImages.first(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val columns = if (displayImages.size == 4) 2 else 3
                    val rows = (displayImages.size + columns - 1) / columns
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val totalSpacing = 8.dp * (columns - 1).toFloat()
                        val itemSize = (maxWidth - totalSpacing) / columns.toFloat()
                        val gridHeight = itemSize * rows.toFloat() + 8.dp * (rows - 1).toFloat()

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(gridHeight),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            items(displayImages) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(itemSize)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(text = entry.caption, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        }
    }
}
 
