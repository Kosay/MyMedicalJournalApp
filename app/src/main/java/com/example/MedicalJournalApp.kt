package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.HealthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MedicalJournalApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { HealthRepository(database.healthDao()) }
}
