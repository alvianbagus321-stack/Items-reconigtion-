package com.example.domain.repository

import com.example.data.local.GroupEntity
import com.example.data.local.InventoryDao
import com.example.data.local.ItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class InventoryRepository(private val dao: InventoryDao) {

    val allGroups: Flow<List<GroupEntity>> = dao.getAllGroups()
    val allItems: Flow<List<ItemEntity>> = dao.getAllItems()

    fun searchItems(query: String): Flow<List<ItemEntity>> {
        return dao.searchItems(query)
    }

    fun getItemsByGroup(groupId: String): Flow<List<ItemEntity>> {
        return dao.getItemsByGroup(groupId)
    }

    fun getItemById(id: String): Flow<ItemEntity?> {
        return dao.getItemById(id)
    }

    fun getGroupById(id: String): Flow<GroupEntity?> {
        return dao.getGroupById(id)
    }

    suspend fun insertItem(item: ItemEntity) = withContext(Dispatchers.IO) {
        dao.insertItem(item)
    }

    suspend fun insertGroup(group: GroupEntity) = withContext(Dispatchers.IO) {
        dao.insertGroup(group)
    }

    suspend fun deleteItem(id: String) = withContext(Dispatchers.IO) {
        dao.deleteItem(id)
    }

    suspend fun deleteGroup(id: String) = withContext(Dispatchers.IO) {
        dao.deleteGroup(id)
    }
}
