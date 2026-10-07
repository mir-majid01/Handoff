package com.handoff.app

import android.app.Application
import androidx.room.Room
import com.handoff.app.core.extraction.ExtractionEngine
import com.handoff.app.core.extraction.ExtractorsConfigLoader
import com.handoff.app.data.db.HandoffDatabase
import com.handoff.app.data.prefs.SettingsStore
import com.handoff.app.data.repo.HandoffRepository

/** Composition root: singletons live here, ViewModel layer pulls from [HandoffApp]. */
class HandoffApp : Application() {

    lateinit var database: HandoffDatabase
        private set
    lateinit var repository: HandoffRepository
        private set
    lateinit var settingsStore: SettingsStore
        private set
    lateinit var extractionEngine: ExtractionEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, HandoffDatabase::class.java, "handoff.db")
            .fallbackToDestructiveMigration()
            .build()
        repository = HandoffRepository(database.handoffDao())
        settingsStore = SettingsStore(this)
        extractionEngine = ExtractionEngine(this, ExtractorsConfigLoader.load(this))
    }
}
