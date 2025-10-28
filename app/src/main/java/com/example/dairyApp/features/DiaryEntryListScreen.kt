package com.example.dairyApp.features

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.example.dairyApp.diary.DiaryEntry
import com.example.dairyApp.diary.DiaryEntryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEntryListScreen(
    navController: NavController,
    eventId: String,
    diaryPageName: String,
    navBackStackEntry: NavBackStackEntry
) {
    val viewModel: DiaryEntryListViewModel = viewModel(
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
                        DiaryEntryCard(
                            entry = entry, 
                            onClick = { 
                                // TODO: Handle entry click if needed
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
