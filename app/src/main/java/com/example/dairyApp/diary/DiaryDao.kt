package com.example.dairyApp.diary

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    // --- DiaryEntry specific methods ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntry)

    @Update
    suspend fun updateEntry(entry: DiaryEntry)

    @Delete
    suspend fun deleteEntry(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries WHERE tripId = :eventId AND groupName = :pageName")
    suspend fun deleteEntriesByEventAndPage(eventId: String, pageName: String)

    @Query("DELETE FROM diary_entries WHERE tripId = :eventId")
    suspend fun deleteEntriesByEventId(eventId: String)

    @Query("SELECT * FROM diary_entries WHERE id = :entryId")
    fun getEntryById(entryId: String): Flow<DiaryEntry?>

    @Query("SELECT * FROM diary_entries WHERE tripId = :eventId ORDER BY timestamp DESC")
    fun getEntriesForEvent(eventId: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE tripId = :eventId AND groupName = :pageName ORDER BY timestamp DESC")
    fun getEntriesForEventAndPage(eventId: String, pageName: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE tripId IS NULL ORDER BY timestamp DESC")
    fun getUnassignedEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<DiaryEntry>>

    // --- DiaryEvent specific methods ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DiaryEvent)

    @Update
    suspend fun updateEvent(event: DiaryEvent)

    @Delete
    suspend fun deleteEvent(event: DiaryEvent)

    @Query("SELECT * FROM diary_trips WHERE id = :eventId")
    fun getEventById(eventId: String): Flow<DiaryEvent?>

    @Query("SELECT * FROM diary_trips ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<DiaryEvent>>
}
