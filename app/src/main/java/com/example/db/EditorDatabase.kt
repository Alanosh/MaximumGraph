package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProjectEntity::class,
        LayerEntity::class,
        KeyframeEntity::class,
        CustomCurveEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DBTypeConverters::class)
abstract class EditorDatabase : RoomDatabase() {
    abstract val editorDao: EditorDao

    companion object {
        @Volatile
        private var INSTANCE: EditorDatabase? = null

        fun getInstance(context: Context): EditorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EditorDatabase::class.java,
                    "draw_animate_editor.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
