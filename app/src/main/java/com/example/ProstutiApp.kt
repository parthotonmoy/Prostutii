package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.DataStoreManager

class ProstutiApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var dataStoreManager: DataStoreManager
        private set

    lateinit var repository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        dataStoreManager = DataStoreManager(this)
        repository = AppRepository(database, dataStoreManager)
    }
}
