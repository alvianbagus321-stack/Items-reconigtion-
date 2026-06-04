package com.example.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SettingsManager {
    private const val PREFS_NAME = "customization_settings"
    private lateinit var prefs: SharedPreferences

    private val _searchBarColor = MutableStateFlow(Color.Unspecified)
    val searchBarColor: StateFlow<Color> = _searchBarColor.asStateFlow()

    private val _searchBarOpacity = MutableStateFlow(1f)
    val searchBarOpacity: StateFlow<Float> = _searchBarOpacity.asStateFlow()
    
    private val _searchBarBlur = MutableStateFlow(0f)
    val searchBarBlur: StateFlow<Float> = _searchBarBlur.asStateFlow()
    
    private val _backgroundImageUri = MutableStateFlow<String?>(null)
    val backgroundImageUri: StateFlow<String?> = _backgroundImageUri.asStateFlow()
    
    private val _backgroundColor = MutableStateFlow(Color.Unspecified)
    val backgroundColor: StateFlow<Color> = _backgroundColor.asStateFlow()

    private val _currency = MutableStateFlow("Rp")
    val currency: StateFlow<String> = _currency.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorInt = prefs.getInt("search_bar_color", 0)
        if (colorInt != 0) _searchBarColor.value = Color(colorInt)
        
        _searchBarOpacity.value = prefs.getFloat("search_bar_opacity", 1f)
        _searchBarBlur.value = prefs.getFloat("search_bar_blur", 0f)
        _backgroundImageUri.value = prefs.getString("background_image_uri", null)
        
        val bgColorInt = prefs.getInt("background_color", 0)
        if (bgColorInt != 0) _backgroundColor.value = Color(bgColorInt)
        
        _currency.value = prefs.getString("currency", "Rp") ?: "Rp"
    }

    fun setCurrency(currency: String) {
        _currency.value = currency
        prefs.edit().putString("currency", currency).apply()
    }

    fun setSearchBarColor(color: Color) {
        _searchBarColor.value = color
        prefs.edit().putInt("search_bar_color", if (color == Color.Unspecified) 0 else color.toArgb()).apply()
    }

    fun setSearchBarOpacity(opacity: Float) {
        _searchBarOpacity.value = opacity
        prefs.edit().putFloat("search_bar_opacity", opacity).apply()
    }
    
    fun setSearchBarBlur(blur: Float) {
        _searchBarBlur.value = blur
        prefs.edit().putFloat("search_bar_blur", blur).apply()
    }

    fun setBackgroundImageUri(uri: String?) {
        _backgroundImageUri.value = uri
        prefs.edit().putString("background_image_uri", uri).apply()
    }
    
    fun setBackgroundColor(color: Color) {
        _backgroundColor.value = color
        prefs.edit().putInt("background_color", if (color == Color.Unspecified) 0 else color.toArgb()).apply()
    }
}
