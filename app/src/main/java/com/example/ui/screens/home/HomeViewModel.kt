package com.example.ui.screens.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ItemEntity
import com.example.data.local.InventoryBackup
import com.example.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HomeViewModel : ViewModel() {
    private val repository = AppContainer.inventoryRepository!!
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val items: StateFlow<List<ItemEntity>> = repository.allItems
        .combine(_searchQuery) { allItems, query ->
            if (query.isBlank()) {
                allItems
            } else {
                allItems.filter { 
                    it.name.contains(query, ignoreCase = true) || 
                    it.description.contains(query, ignoreCase = true) 
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun exportData(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val allGroups = repository.allGroups.first()
                val allItems = repository.allItems.first()
                val backup = InventoryBackup(allGroups, allItems)
                val jsonString = Json.encodeToString(backup)
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun importData(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                if (jsonString != null) {
                    val backup = Json.decodeFromString<InventoryBackup>(jsonString)
                    backup.groups.forEach { repository.insertGroup(it) }
                    backup.items.forEach { repository.insertItem(it) }
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }
}
