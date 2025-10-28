package com.example.dairyApp.diary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dairyApp.data.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

// UI State for Diary Home Screen
data class DiaryHomeUiState(
    val events: List<DiaryEvent> = emptyList(), // Renamed from trips to events for clarity
    val isLoading: Boolean = false,
    val error: String? = null
)

class DiaryViewModel(application: Application) : AndroidViewModel(application) {

    private val diaryRepository: DiaryRepository

    init {
        val diaryDao = AppDatabase.getDatabase(application).diaryDao()
        diaryRepository = DiaryRepository(diaryDao)
    }

    val diaryHomeUiState: StateFlow<DiaryHomeUiState> = diaryRepository.getAllEvents() // Corrected method name
        .map { eventsList ->
            DiaryHomeUiState(events = eventsList, isLoading = false) // Updated property name
        }
        .catch { e ->
            emit(DiaryHomeUiState(isLoading = false, error = e.message ?: "An unexpected error occurred"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DiaryHomeUiState(isLoading = true)
        )

    // Renamed from addTrip to addEvent for consistency
    fun addEvent(name: String, coverImageUri: String? = null) {
        viewModelScope.launch {
            val newEvent = DiaryEvent(
                id = UUID.randomUUID().toString(),
                name = name,
                entryIds = emptyList(),
                coverImageUri = coverImageUri,
                timestamp = System.currentTimeMillis()
            )
            diaryRepository.insertEvent(newEvent) // Corrected method name
        }
    }
}
