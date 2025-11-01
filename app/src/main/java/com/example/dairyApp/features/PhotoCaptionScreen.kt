package com.example.dairyApp.features

import android.app.Application
import android.net.Uri
import androidx.navigation.NavBackStackEntry
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.Intent
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
import com.example.dairyApp.utils.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCaptionScreen(
    navController: NavController,
    initialEventId: String?,
    initialDiaryPageName: String? = null,
    entryIdToEdit: String? = null,
    navBackStackEntry: NavBackStackEntry
) {
    // Scope the ViewModel to the NavBackStackEntry so each navigation destination gets its own savedState
    val viewModel: PhotoCaptionViewModel = viewModel(
        modelClass = PhotoCaptionViewModel::class.java,
        viewModelStoreOwner = navBackStackEntry,
        factory = PhotoCaptionViewModel.provideFactory(
            application = LocalContext.current.applicationContext as Application,
            owner = navBackStackEntry,
            initialEventId = initialEventId,
            initialDiaryPageName = initialDiaryPageName,
            entryIdToEdit = entryIdToEdit
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    // Prefill diaryPageName from viewModel when editing an existing entry
    var diaryPageName by remember { mutableStateOf(uiState.selectedDiaryPageName ?: initialDiaryPageName ?: "") }
    LaunchedEffect(uiState.selectedDiaryPageName, initialDiaryPageName) {
        diaryPageName = uiState.selectedDiaryPageName ?: initialDiaryPageName ?: diaryPageName
    }

    // Ensure availablePages are loaded when an event is preselected by the ViewModel
    LaunchedEffect(uiState.selectedEventId) {
        // call VM handler to (re)load pages for the selected event; safe if null
        viewModel.onEventSelected(uiState.selectedEventId)
    }
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
                // Show up to 9 images in a responsive grid similar to Moments.
                // Layout rules:
                // - 1 image: big full-width preview
                // - 4 images: 2 columns (2x2)
                // - otherwise: 3 columns
                val maxImages = minOf(uiState.selectedPhotoUris.size, 9)
                val displayImages = uiState.selectedPhotoUris.take(maxImages)

                if (displayImages.size == 1) {
                    AsyncImage(
                        model = displayImages.first(),
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val columns = if (displayImages.size == 4) 2 else 3
                    val rows = (displayImages.size + columns - 1) / columns
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        // Use Float multipliers to avoid ambiguous overloads with Int * Dp
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
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            items(displayImages) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected image for preview",
                                    modifier = Modifier
                                        .size(itemSize)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 用户自定义提示（可选），会传给后端影响生成结果
            OutlinedTextField(
                value = uiState.userPrompt,
                onValueChange = { viewModel.onUserPromptChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("自定义提示（可选）") },
                placeholder = { Text("例如：温柔、诗意、简洁短句...") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                // 在此页面不显示“日记页/事件”选择控件（由上层页面管理），仅显示保存按钮。
                // 如果没有可用的 diaryPageName（即不是从页内进入），提示用户从日记页进入添加。
                Spacer(modifier = Modifier.height(8.dp))
                if (diaryPageName.isBlank()) {
                    Text(text = "未指定日记页：请从日记页入口进入以添加条目。", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val context = LocalContext.current
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            viewModel.saveDiaryEntry(
                                diaryPageName = diaryPageName.ifBlank { null },
                                eventId = uiState.selectedEventId
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isLoading && diaryPageName.isNotBlank()
                    ) {
                        Text("保存到图片日记")
                    }

                    Button(
                        onClick = {
                            val uris = uiState.selectedPhotoUris
                            try {
                                ShareUtils.shareImagesWithCaption(context, uris, uiState.generatedCaption)
                                scope.launch {
                                    snackbarHostState.showSnackbar("文案已复制到剪贴板，若目标应用未显示请长按粘贴")
                                }
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("分享失败：${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isLoading && diaryPageName.isNotBlank() && uiState.selectedPhotoUris.isNotEmpty() && uiState.generatedCaption.isNotBlank()
                    ) {
                        Text("分享")
                    }
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
                val maxImages = minOf(images.size, 9)
                val displayImages = images.take(maxImages)
                if (displayImages.size == 1) {
                    AsyncImage(
                        model = displayImages.first(),
                        contentDescription = "Formatted output image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                when (displayImages.size) {
                                    in 1..3 -> 120.dp
                                    in 4..6 -> 240.dp
                                    else -> 360.dp
                                }
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(displayImages.size) { idx ->
                            val uri = displayImages[idx]
                            AsyncImage(
                                model = uri,
                                contentDescription = "Formatted output image",
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp)),
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
