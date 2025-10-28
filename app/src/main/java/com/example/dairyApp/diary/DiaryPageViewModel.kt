package com.example.dairyApp.diary

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.example.dairyApp.data.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiaryPageListUiState(
    val event: DiaryEvent? = null,
    val diaryPageNames: List<String> = emptyList(),
    val entries: List<DiaryEntry> = emptyList(), // Added to hold the entries for the event
    val isLoading: Boolean = false,
    val error: String? = null
)

class DiaryPageViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val diaryRepository: DiaryRepository
    private val eventId: String = savedStateHandle.get<String>("eventId")!!

    init {
        val diaryDao = AppDatabase.getDatabase(application).diaryDao()
        diaryRepository = DiaryRepository(diaryDao)
    }

    val uiState: StateFlow<DiaryPageListUiState> = combine(
        diaryRepository.getEventById(eventId),
        diaryRepository.getEntriesForEvent(eventId)
    ) { event, entries ->
        val pageNames = entries.mapNotNull { it.diaryPageName }.distinct()
        // Populate all fields in the UI state
        DiaryPageListUiState(event = event, diaryPageNames = pageNames, entries = entries)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DiaryPageListUiState(isLoading = true)
    )

    fun deletePage(pageName: String) {
        viewModelScope.launch {
            diaryRepository.deletePage(eventId, pageName)
        }
    }

    companion object {
        fun provideFactory(
            application: Application,
            owner: SavedStateRegistryOwner,
            eventId: String,
            defaultArgs: Bundle? = null
        ): AbstractSavedStateViewModelFactory = object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
                handle["eventId"] = eventId
                return DiaryPageViewModel(application, handle) as T
            }
        }
    }
}
