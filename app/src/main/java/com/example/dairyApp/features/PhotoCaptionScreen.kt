package com.example.dairyApp.features

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // For LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items // For LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dairyApp.diary.DiaryEvent // Updated import
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCaptionScreen(
    navController: NavController, 
    initialEventId: String?,      
    viewModel: PhotoCaptionViewModel = viewModel(
        factory = PhotoCaptionViewModel.provideFactory(
            application = LocalContext.current.applicationContext as Application,
            owner = LocalContext.current as androidx.savedstate.SavedStateRegistryOwner,
            initialEventId = initialEventId
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var diaryPageName by remember { mutableStateOf("") } 
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isEventDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveMessage) {
        uiState.saveMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSaveMessage()
                if (it == "已保存到日记！") { 
                    navController.popBackStack()
                }
            }
        }
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onPhotosSelected(uris)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("图片日记条目") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Text("选择照片")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.selectedPhotoUris.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (uiState.selectedPhotoUris.size <= 3) 120.dp else 230.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.selectedPhotoUris) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected image for preview",
                            modifier = Modifier
                                .size(100.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.isLoading && uiState.generatedCaption.isBlank()) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.generateCaption() },
                    enabled = uiState.selectedPhotoUris.isNotEmpty() && !uiState.isLoading
                ) {
                    Text(if (uiState.generatedCaption.isNotBlank()) "重新生成文案" else "生成文案")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.generatedCaption,
                onValueChange = { newCaption -> viewModel.onCaptionChanged(newCaption) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("编辑文案") },
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.selectedPhotoUris.isNotEmpty() && uiState.generatedCaption.isNotBlank()) {
                OutlinedTextField(
                    value = diaryPageName,
                    onValueChange = { diaryPageName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("日记页名称 (可选)") }, 
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = isEventDropdownExpanded,
                    onExpandedChange = { isEventDropdownExpanded = !isEventDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val selectedEventName = uiState.availableEvents.find { it.id == uiState.selectedEventId }?.name
                    OutlinedTextField(
                        value = selectedEventName ?: "选择一个事件 (可选)", 
                        onValueChange = {}, 
                        readOnly = true,
                        label = { Text("归属于事件") }, 
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEventDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isEventDropdownExpanded,
                        onDismissRequest = { isEventDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("不指定事件") }, 
                            onClick = {
                                viewModel.onEventSelected(null) 
                                isEventDropdownExpanded = false
                            }
                        )
                        uiState.availableEvents.forEach { event: DiaryEvent -> // Explicit type here for clarity
                            DropdownMenuItem(
                                text = { Text(event.name) },
                                onClick = {
                                    viewModel.onEventSelected(event.id) 
                                    isEventDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        viewModel.saveDiaryEntry(
                            diaryPageName = diaryPageName.ifBlank { null },
                            eventId = uiState.selectedEventId 
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    Text("保存到图片日记")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.generatedCaption.isNotBlank() && uiState.selectedPhotoUris.isNotEmpty()) {
                FormattedOutputCard(
                    images = uiState.selectedPhotoUris,
                    caption = uiState.generatedCaption
                )
            }
            Spacer(modifier = Modifier.height(16.dp)) 
        }
    }
}

@Composable
fun FormattedOutputCard(images: List<Uri>, caption: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp) 
    ) {
        Column(
            modifier = Modifier.padding(all = 20.dp) 
        ) {
            if (images.isNotEmpty()) {
                if (images.size == 1) {
                    AsyncImage(
                        model = images.first(),
                        contentDescription = "Formatted output image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f) 
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp) 
                    ) {
                        items(images) { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Formatted output image",
                                modifier = Modifier
                                    .height(180.dp)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)), 
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                text = caption,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
        }
    }
}
