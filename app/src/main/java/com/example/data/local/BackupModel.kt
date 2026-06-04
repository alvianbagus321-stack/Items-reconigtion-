package com.example.data.local

import kotlinx.serialization.Serializable

@Serializable
data class InventoryBackup(
    val groups: List<GroupEntity>,
    val items: List<ItemEntity>
)
