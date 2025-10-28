package com.example.dairyApp.features

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.example.dairyApp.data.AppDatabase
import com.example.dairyApp.diary.DiaryEntry
import com.example.dairyApp.diary.DiaryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryEntryListViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val diaryRepository: DiaryRepository
    private val eventId: String = savedStateHandle.get<String>("eventId")!!
    private val diaryPageName: String = savedStateHandle.get<String>("diaryPageName")!!

    init {
        val diaryDao = AppDatabase.getDatabase(application).diaryDao()
        diaryRepository = DiaryRepository(diaryDao)
    }

    val entries: StateFlow<List<DiaryEntry>> = diaryRepository.getEntriesForEventAndPage(eventId, diaryPageName)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun delete(entry: DiaryEntry) {
        viewModelScope.launch {
            diaryRepository.deleteEntry(entry)
        }
    }

    companion object {
        fun provideFactory(
            application: Application,
            owner: SavedStateRegistryOwner,
            eventId: String,
            diaryPageName: String,
            defaultArgs: Bundle? = null
        ): AbstractSavedStateViewModelFactory = object : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
                handle["eventId"] = eventId
                handle["diaryPageName"] = diaryPageName
                return DiaryEntryListViewModel(application, handle) as T
            }
        }
    }
}
