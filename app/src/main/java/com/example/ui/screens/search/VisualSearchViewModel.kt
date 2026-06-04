package com.example.ui.screens.search

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.analyzeImageWithGemini
import com.example.data.local.ItemEntity
import com.example.di.AppContainer
import com.example.ui.screens.settings.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OnlineProductStore(
    val storeName: String,
    val price: String,
    val productUrl: String
)

@Serializable
data class OnlineProductResult(
    val productName: String,
    val description: String,
    val imageUrl: String,
    val priceRange: String,
    val stores: List<OnlineProductStore>
)

data class VisualSearchState(
    val imageUri: String = "",
    val isAnalyzing: Boolean = true,
    val extractedKeywords: String = "",
    val matchedItems: List<ItemEntity> = emptyList(),
    val error: String? = null,
    // Mode State
    val searchMode: String = "data", // "data" or "online"
    val isOnlineAnalyzing: Boolean = false,
    val onlineResult: OnlineProductResult? = null,
    val onlineError: String? = null
)

class VisualSearchViewModel : ViewModel() {
    private val repository = AppContainer.inventoryRepository!!
    private val _uiState = MutableStateFlow(VisualSearchState())
    val uiState: StateFlow<VisualSearchState> = _uiState

    fun setMode(context: Context, mode: String) {
        _uiState.value = _uiState.value.copy(searchMode = mode)
        if (mode == "online" && _uiState.value.onlineResult == null && !_uiState.value.isOnlineAnalyzing) {
            analyzeImageOnline(context, _uiState.value.imageUri)
        }
    }

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

    fun analyzeImageOnline(context: Context, uriString: String) {
        _uiState.value = _uiState.value.copy(isOnlineAnalyzing = true, onlineError = null)
        viewModelScope.launch {
            try {
                val uri = Uri.parse(Uri.decode(uriString))
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val selectedCurrency = SettingsManager.currency.value
                    val prompt = """
                        Analyze this image of a product and act as an online shopping search engine. Identifikasi produk utama dalam gambar ini dan carikan harga aslinya di e-commerce / internet.
                        Sediakan hasil pencarian terperinci dalam Bahasa Indonesia.
                        Format rentang harga dan harga online dengan mata uang: $selectedCurrency (convert or format correctly).
                        
                        You MUST respond ONLY with a valid, clean, raw JSON block. DO NOT wrap it in ```json ... ``` markdown codeblocks - just the raw JSON text. It must match this schema strictly:
                        {
                          "productName": "Sleek and clear name of the product identified in the image",
                          "description": "Comprehensive specifications, useful descriptions, and key features of the product.",
                          "imageUrl": "Direct URL of a representative high-quality image of this product category or item online (e.g. from public image site like Unsplash or clear product CDN, must start with https://images.unsplash.com or another valid domain)",
                          "priceRange": "$selectedCurrency 150.000 - $selectedCurrency 250.000",
                          "stores": [
                            {
                              "storeName": "Name of Marketplace (e.g., Tokopedia, Shopee, Lazada, Blibli, Amazon)",
                              "price": "$selectedCurrency 165.000",
                              "productUrl": "https://www.tokopedia.com/search?q=product_name_query"
                            },
                            {
                              "storeName": "Name of Marketplace (e.g., Tokopedia, Shopee, Lazada, Blibli, Amazon)",
                              "price": "$selectedCurrency 172.000",
                              "productUrl": "https://shopee.co.id/search?keyword=product_name_query"
                            }
                          ]
                        }
                    """.trimIndent()

                    val resultText = analyzeImageWithGemini(context, bitmap, prompt)
                    val cleanJson = cleanJsonString(resultText)
                    
                    try {
                        val json = Json { ignoreUnknownKeys = true; isLenient = true }
                        val result = json.decodeFromString<OnlineProductResult>(cleanJson)
                        _uiState.value = _uiState.value.copy(
                            isOnlineAnalyzing = false,
                            onlineResult = result
                        )
                    } catch (e: Exception) {
                        Log.e("VisualSearchViewModel", "JSON Parsing failure, prompt response was: $resultText", e)
                        
                        // Fallback: manually construct a result if JSON parse failed but we got some text
                        val fallback = OnlineProductResult(
                            productName = "Product Identified",
                            description = resultText,
                            imageUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500",
                            priceRange = "$selectedCurrency (Lihat deskripsi)",
                            stores = listOf(
                                OnlineProductStore("Google Search", "Sesuai Pasar", "https://www.google.com/search?q=harga+barang")
                            )
                        )
                        _uiState.value = _uiState.value.copy(
                            isOnlineAnalyzing = false,
                            onlineResult = fallback
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isOnlineAnalyzing = false, onlineError = "Failed to load image")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isOnlineAnalyzing = false, onlineError = e.localizedMessage)
            }
        }
    }

    private fun cleanJsonString(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substringAfter("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.substringAfter("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.substringBeforeLast("```")
        }
        return clean.trim()
    }
}
