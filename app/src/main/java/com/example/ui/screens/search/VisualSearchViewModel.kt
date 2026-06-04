package com.example.ui.screens.search

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.analyzeImageWithGemini
import com.example.data.local.ItemEntity
import com.example.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class VisualSearchState(
    val imageUri: String = "",
    val isAnalyzing: Boolean = true,
    val extractedKeywords: String = "",
    val matchedItems: List<ItemEntity> = emptyList(),
    val error: String? = null
)

class VisualSearchViewModel : ViewModel() {
    private val repository = AppContainer.inventoryRepository!!
    private val _uiState = MutableStateFlow(VisualSearchState())
    val uiState: StateFlow<VisualSearchState> = _uiState

    fun analyzeImage(context: Context, uriString: String) {
        _uiState.value = _uiState.value.copy(imageUri = uriString, isAnalyzing = true, error = null)
        viewModelScope.launch {
            try {
                val uri = Uri.parse(Uri.decode(uriString))
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val prompt = "Analyze this image and identify the primary main object. Reply strictly with 2 or 3 short search keywords separated by space. Include any visible brand or textual label."
                    val keywordsText = analyzeImageWithGemini(context, bitmap, prompt)
                        .replace("\n", " ").trim()
                    
                    val keywords = keywordsText.split(Regex("\\s+")).filter { it.isNotBlank() }
                    
                    val allItems = repository.allItems.first()
                    
                    // Simple search logic based on keywords
                    val matched = allItems.filter { item ->
                        keywords.any { keyword ->
                            item.name.contains(keyword, ignoreCase = true) || item.description.contains(keyword, ignoreCase = true)
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        extractedKeywords = keywordsText,
                        matchedItems = matched
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isAnalyzing = false, error = "Failed to load image")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isAnalyzing = false, error = e.localizedMessage)
            }
        }
    }
}
