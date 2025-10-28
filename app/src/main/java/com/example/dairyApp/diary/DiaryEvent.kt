package com.example.dairyApp.diary

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.dairyApp.data.StringListConverter

@Entity(tableName = "diary_trips") // Table name is still diary_trips as changing it requires a migration
@TypeConverters(StringListConverter::class)
data class DiaryEvent(
    @PrimaryKey val id: String,
    val name: String,
    val entryIds: List<String> = emptyList(),
    val coverImageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
