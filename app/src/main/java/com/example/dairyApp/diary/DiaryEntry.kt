package com.example.dairyApp.diary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.dairyApp.data.StringListConverter

@Entity(tableName = "diary_entries")
@TypeConverters(StringListConverter::class)
data class DiaryEntry(
    @PrimaryKey val id: String,
    val imageUris: List<String>, 
    val caption: String,
    val timestamp: Long,
    @ColumnInfo(name = "groupName") val diaryPageName: String? = null, // Renamed, maps to old column
    @ColumnInfo(name = "tripId") val eventId: String? = null // Renamed, maps to old column
)
