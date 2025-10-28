package com.example.dairyApp.diary

import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val diaryDao: DiaryDao) {

    // --- DiaryEntry methods ---
    suspend fun insertEntry(entry: DiaryEntry) {
        diaryDao.insertEntry(entry)
    }

    suspend fun updateEntry(entry: DiaryEntry) {
        diaryDao.updateEntry(entry)
    }

    suspend fun deleteEntry(entry: DiaryEntry) {
        diaryDao.deleteEntry(entry)
    }

    suspend fun deletePage(eventId: String, pageName: String) {
        diaryDao.deleteEntriesByEventAndPage(eventId, pageName)
    }

    fun getEntryById(entryId: String): Flow<DiaryEntry?> {
        return diaryDao.getEntryById(entryId)
    }

    fun getEntriesForEvent(eventId: String): Flow<List<DiaryEntry>> {
        return diaryDao.getEntriesForEvent(eventId)
    }

    fun getEntriesForEventAndPage(eventId: String, pageName: String): Flow<List<DiaryEntry>> {
        return diaryDao.getEntriesForEventAndPage(eventId, pageName)
    }

    fun getUnassignedEntries(): Flow<List<DiaryEntry>> {
        return diaryDao.getUnassignedEntries()
    }

    fun getAllEntries(): Flow<List<DiaryEntry>> {
        return diaryDao.getAllEntries()
    }

    // --- DiaryEvent methods ---
    suspend fun insertEvent(event: DiaryEvent) {
        diaryDao.insertEvent(event)
    }

    suspend fun updateEvent(event: DiaryEvent) {
        diaryDao.updateEvent(event)
    }

    suspend fun deleteEvent(event: DiaryEvent) {
        // First, delete all entries associated with this event to maintain data integrity
        diaryDao.deleteEntriesByEventId(event.id)
        // Then, delete the event itself
        diaryDao.deleteEvent(event)
    }

    fun getEventById(eventId: String): Flow<DiaryEvent?> {
        return diaryDao.getEventById(eventId)
    }

    fun getAllEvents(): Flow<List<DiaryEvent>> {
        return diaryDao.getAllEvents()
    }
}
