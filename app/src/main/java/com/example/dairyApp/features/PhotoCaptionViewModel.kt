package com.example.dairyApp.features

import android.app.Application
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.example.dairyApp.data.AppDatabase
import com.example.dairyApp.data.CaptionRepository
import com.example.dairyApp.diary.DiaryEntry
import com.example.dairyApp.diary.DiaryEvent // Updated import
import com.example.dairyApp.diary.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class PhotoCaptionUiState(
    val selectedPhotoUris: List<Uri> = emptyList(),
    val generatedCaption: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveMessage: String? = null,
    val availableEvents: List<DiaryEvent> = emptyList(), // Changed to DiaryEvent
    val selectedEventId: String? = null
)

class PhotoCaptionViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val initialEventId: String?
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PhotoCaptionUiState())
    val uiState: StateFlow<PhotoCaptionUiState> = _uiState.asStateFlow()

    private val captionRepository = CaptionRepository(application.applicationContext)
    private val diaryRepository: DiaryRepository

    init {
        val diaryDao = AppDatabase.getDatabase(application).diaryDao()
        diaryRepository = DiaryRepository(diaryDao)

        viewModelScope.launch {
            diaryRepository.getAllEvents() // Changed to getAllEvents
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000L),
                    initialValue = emptyList()
                ).collect { events: List<DiaryEvent> -> // Explicitly typed as List<DiaryEvent>
                    _uiState.update {
                        it.copy(
                            availableEvents = events,
                            selectedEventId = initialEventId?.takeIf { id -> events.any { event -> event.id == id } } ?: it.selectedEventId
                        )
                    }
                }
        }
        if (initialEventId != null && _uiState.value.selectedEventId == null) {
            _uiState.update { it.copy(selectedEventId = initialEventId) }
        }
    }

    fun onPhotosSelected(uris: List<Uri>) {
        _uiState.update { it.copy(selectedPhotoUris = uris, saveMessage = null) }
    }

    fun generateCaption() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, saveMessage = null) }
            try {
                val caption = captionRepository.generateCaptionForImages(_uiState.value.selectedPhotoUris)
                _uiState.update { it.copy(generatedCaption = caption, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun onCaptionChanged(newCaption: String) {
        _uiState.update { it.copy(generatedCaption = newCaption, saveMessage = null) }
    }

    fun onEventSelected(eventId: String?) {
        _uiState.update { it.copy(selectedEventId = eventId) }
    }

    fun saveDiaryEntry(diaryPageName: String?, eventId: String?) {
        val currentState = _uiState.value
        if (currentState.selectedPhotoUris.isEmpty() || currentState.generatedCaption.isBlank()) {
            _uiState.update { it.copy(saveMessage = "没有图片或文案来保存。") }
            return
        }

        viewModelScope.launch {
            try {
                val imageUriStrings = currentState.selectedPhotoUris.map { it.toString() }
                val newEntryId = UUID.randomUUID().toString()
                val newEntry = DiaryEntry(
                    id = newEntryId,
                    imageUris = imageUriStrings,
                    caption = currentState.generatedCaption,
                    timestamp = System.currentTimeMillis(),
                    diaryPageName = diaryPageName?.takeIf { it.isNotBlank() },
                    eventId = eventId
                )
                diaryRepository.insertEntry(newEntry)

                if (eventId != null) {
                    val eventToUpdate = diaryRepository.getEventById(eventId).firstOrNull() // Changed to getEventById
                    eventToUpdate?.let { event: DiaryEvent -> // Explicitly typed as DiaryEvent
                        val updatedEntryIds = event.entryIds.toMutableList()
                        if (!updatedEntryIds.contains(newEntryId)) {
                            updatedEntryIds.add(newEntryId)
                            diaryRepository.updateEvent(event.copy(entryIds = updatedEntryIds)) // Changed to updateEvent
                        }
                    }
                }
                _uiState.update { it.copy(saveMessage = "已保存到日记！") }
            } catch (e: Exception) {
                _uiState.update { it.copy(saveMessage = "保存失败: ${e.message}") }
            }
        }
    }

    fun clearSaveMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }

    companion object {
        fun provideFactory(
            application: Application,
            owner: SavedStateRegistryOwner,
            initialEventId: String?
        ): ViewModelProvider.Factory {
            val defaultArgs = Bundle()
            return object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
                    if (modelClass.isAssignableFrom(PhotoCaptionViewModel::class.java)) {
                        return PhotoCaptionViewModel(
                            application = application,
                            savedStateHandle = handle,
                            initialEventId = initialEventId
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
