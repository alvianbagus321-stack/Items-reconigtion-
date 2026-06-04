package com.example.ui.screens.item

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.analyzeImageWithGemini
import com.example.data.local.GroupEntity
import com.example.data.local.ItemEntity
import com.example.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class AddItemViewModel : ViewModel() {
    private val repository = AppContainer.inventoryRepository!!
    
    val groups: StateFlow<List<GroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState

    fun updateName(name: String) = _uiState.value.let { _uiState.value = it.copy(name = name) }
    fun updatePrice(price: String) = _uiState.value.let { _uiState.value = it.copy(price = price) }
    fun updateDescription(desc: String) = _uiState.value.let { _uiState.value = it.copy(description = desc) }
    fun selectGroup(groupId: String?) = _uiState.value.let { _uiState.value = it.copy(selectedGroupId = groupId) }
    
    fun setImages(uris: List<String>) {
        _uiState.value = _uiState.value.copy(images = uris)
    }

    fun addImage(uri: String) {
        val newImages = _uiState.value.images + uri
        _uiState.value = _uiState.value.copy(images = newImages)
    }

    fun analyzeImage(context: Context, uriString: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            try {
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val description = analyzeImageWithGemini(
                        context, 
                        bitmap, 
                        "Analyze this image in detail. Identify the primary item. Provide a clear, concise description and a highly likely name. CRITICAL: Carefully read and include any visible text, labels, serial numbers, brands, or barcodes on the item. If you see text or numbers, transcribe them exactly."
                    )
                    _uiState.value = _uiState.value.copy(
                        description = description,
                        isAnalyzing = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isAnalyzing = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isAnalyzing = false)
            }
        }
    }

    fun saveItem(onComplete: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank() || state.images.size < 2) return // Requirement: minimum 2 photos

        viewModelScope.launch {
            val item = ItemEntity(
                id = UUID.randomUUID().toString(),
                name = state.name,
                price = state.price.toDoubleOrNull() ?: 0.0,
                description = state.description,
                groupId = state.selectedGroupId,
                mainImageUri = state.images.firstOrNull() ?: "",
                imagesJson = Json.encodeToString(state.images)
            )
            repository.insertItem(item)
            onComplete()
        }
    }
}

data class AddItemUiState(
    val name: String = "",
    val price: String = "",
    val description: String = "",
    val selectedGroupId: String? = null,
    val images: List<String> = emptyList(),
    val isAnalyzing: Boolean = false
)
