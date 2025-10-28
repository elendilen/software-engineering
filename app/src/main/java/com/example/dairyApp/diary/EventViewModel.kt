package com.example.dairyApp.diary

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dairyApp.data.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class EventViewModel(application: Application) : ViewModel() {

    private val diaryRepository: DiaryRepository

    init {
        val diaryDao = AppDatabase.getDatabase(application).diaryDao()
        diaryRepository = DiaryRepository(diaryDao)
    }

    val events: StateFlow<List<DiaryEvent>> = diaryRepository.getAllEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createEvent(name: String) {
        viewModelScope.launch {
            val newEvent = DiaryEvent(
                id = UUID.randomUUID().toString(),
                name = name,
                entryIds = emptyList(),
                coverImageUri = null,
                timestamp = System.currentTimeMillis()
            )
            diaryRepository.insertEvent(newEvent)
        }
    }

    fun delete(event: DiaryEvent) {
        viewModelScope.launch {
            diaryRepository.deleteEvent(event)
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
                        return EventViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}
