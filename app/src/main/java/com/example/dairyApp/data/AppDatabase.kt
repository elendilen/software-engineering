package com.example.dairyApp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.dairyApp.diary.DiaryDao
import com.example.dairyApp.diary.DiaryEntry
import com.example.dairyApp.diary.DiaryEvent // <--- 修改了这里

@Database(entities = [DiaryEntry::class, DiaryEvent::class], version = 1, exportSchema = false) // <--- 修改了这里
@TypeConverters(StringListConverter::class) // Ensure converters are registered for the DB
abstract class AppDatabase : RoomDatabase() {

    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "diary_database" // Name of the database file
                )
                // Optional: Add migrations if you change the schema in the future
                // .addMigrations(MIGRATION_1_2 /*, MIGRATION_2_3 ...*/)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
