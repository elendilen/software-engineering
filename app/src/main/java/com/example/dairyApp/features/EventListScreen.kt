package com.example.dairyApp.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.example.dairyApp.diary.DiaryEvent
import com.example.dairyApp.diary.DiaryEventCard
import com.example.dairyApp.diary.EventViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    eventViewModel: EventViewModel,
    onEventClick: (String) -> Unit,
    onCreateEventClick: () -> Unit
) {
    val events by eventViewModel.events.collectAsState()
    var eventToDelete by remember { mutableStateOf<DiaryEvent?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的日记事件") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateEventClick) {
                Icon(Icons.Filled.Add, contentDescription = "创建新的日记事件")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "还没有事件。点击 ‘+’ 创建一个吧！")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(events) { event ->
                        DiaryEventCard(
                            event = event,
                            onClick = { onEventClick(event.id) },
                            onDeleteClick = { eventToDelete = event }
                        )
                    }
                }
            }
        }

        eventToDelete?.let { event ->
            DeleteConfirmationDialog(
                onConfirm = { 
                    eventViewModel.delete(event)
                    eventToDelete = null 
                },
                onDismiss = { eventToDelete = null },
                itemName = "事件"
            )
        }
    }
}
