package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.domain.repository.InventoryRepository

object AppContainer {
    private var database: AppDatabase? = null
    var inventoryRepository: InventoryRepository? = null

    fun init(context: Context) {
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "smart_inventory_db"
            ).build()
            inventoryRepository = InventoryRepository(database!!.inventoryDao())
        }
    }
}
