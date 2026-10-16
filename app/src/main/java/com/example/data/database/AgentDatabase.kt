package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.DefaultAgentProfiles
import com.example.data.converter.AgentConverters
import com.example.data.dao.AgentProfileDao
import com.example.data.entity.AgentProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main Room Database for the AgentCore Multi-Agent AI Platform.
 */
@Database(
    entities = [AgentProfileEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(AgentConverters::class)
abstract class AgentDatabase : RoomDatabase() {

    abstract fun agentProfileDao(): AgentProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AgentDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): AgentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AgentDatabase::class.java,
                    "agentcore_platform.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate default built-in agent profiles
                            scope.launch {
                                getInstance(context, scope).agentProfileDao()
                                    .insertProfiles(DefaultAgentProfiles.createDefaults())
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
