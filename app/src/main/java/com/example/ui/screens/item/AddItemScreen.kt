package com.example.ui.screens.item

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.utils.FileUtils
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    navController: NavController,
    initialImageUri: String?,
    viewModel: AddItemViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val context = LocalContext.current
    
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageUri != null) {
            viewModel.addImage(tempImageUri.toString())
        }
    }

    LaunchedEffect(initialImageUri) {
        if (!initialImageUri.isNullOrBlank()) {
            val decodedUri = URLDecoder.decode(initialImageUri, "utf-8")
            viewModel.setImages(listOf(decodedUri))
            viewModel.analyzeImage(context, decodedUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Item") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photos Requirements
            Text("Photos (1 for Fast Mode, 2+ recommended)", style = MaterialTheme.typography.titleMedium)
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.images) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Item image",
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                item {
                    val currentSize = uiState.images.size
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                tempImageUri = FileUtils.getTempImageUri(context)
                                cameraLauncher.launch(tempImageUri!!)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {}
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = "Add Photo")
                            Text(if(currentSize < 2) "${currentSize}/2" else "$currentSize Added", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (uiState.isAnalyzing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Analyzing image with Gemini AI...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else if (uiState.description.startsWith("Error")) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            uiState.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text("Item Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.name.isBlank()
            )

            OutlinedTextField(
                value = uiState.price,
                onValueChange = viewModel::updatePrice,
                label = { Text("Price") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                maxLines = 4
            )

            // Simplistic Group Selector
            if (groups.isNotEmpty()) {
                Text("Select Group (Optional)", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(groups) { group ->
                        FilterChip(
                            selected = uiState.selectedGroupId == group.id,
                            onClick = { 
                                val newId = if (uiState.selectedGroupId == group.id) null else group.id
                                viewModel.selectGroup(newId) 
                            },
                            label = { Text(group.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.saveItem(onComplete = { navController.popBackStack("home", false) }) },
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("save_button"),
                enabled = uiState.name.isNotBlank() && uiState.images.isNotEmpty()
            ) {
                Text("Save Item")
            }
            if (uiState.images.isEmpty()) {
                Text(
                    text = "Please add at least 1 photo.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (uiState.images.size == 1) {
                Text(
                    text = "Fast Mode: AI recognition might make mistakes. Add more photos for better accuracy.",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
