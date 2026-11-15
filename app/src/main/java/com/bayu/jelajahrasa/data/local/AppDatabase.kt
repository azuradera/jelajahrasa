package com.bayu.jelajahrasa.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bayu.jelajahrasa.data.local.dao.CollectionDao
import com.bayu.jelajahrasa.data.local.dao.MovieDao
import com.bayu.jelajahrasa.data.local.entity.CollectionEntity
import com.bayu.jelajahrasa.data.local.entity.MovieEntity

@Database(
    entities = [
        MovieEntity::class,
        CollectionEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao

    abstract fun collectionDao(): CollectionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jelajah_rasa_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
