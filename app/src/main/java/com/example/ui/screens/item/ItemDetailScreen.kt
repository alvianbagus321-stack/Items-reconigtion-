package com.example.ui.screens.item

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ItemDetailViewModel : ViewModel() {
    private val repository = AppContainer.inventoryRepository!!
    private val _itemState = MutableStateFlow<com.example.data.local.ItemEntity?>(null)
    val itemState: StateFlow<com.example.data.local.ItemEntity?> = _itemState

    fun loadItem(id: String) {
        viewModelScope.launch {
            repository.getItemById(id).collect { item ->
                _itemState.value = item
            }
        }
    }

    fun deleteItem(id: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteItem(id)
            onComplete()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    navController: NavController,
    itemId: String,
    viewModel: ItemDetailViewModel = viewModel()
) {
    val item by viewModel.itemState.collectAsState()

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.deleteItem(itemId) { navController.popBackStack() } 
                    }, modifier = Modifier.testTag("delete_item_button")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        item?.let { nonNullItem ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                val images = try {
                    Json.decodeFromString<List<String>>(nonNullItem.imagesJson)
                } catch (e: Exception) {
                    listOf(nonNullItem.mainImageUri)
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(images) { uri ->
                        AsyncImage(
                            model = uri.takeIf { it.isNotBlank() },
                            contentDescription = nonNullItem.name,
                            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Price", style = MaterialTheme.typography.labelMedium)
                Text("$${String.format("%.2f", nonNullItem.price)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Spacer(modifier = Modifier.height(16.dp))

                Text("Description", style = MaterialTheme.typography.labelMedium)
                Text(nonNullItem.description, style = MaterialTheme.typography.bodyLarge)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text("Added on: ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(nonNullItem.createdAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
