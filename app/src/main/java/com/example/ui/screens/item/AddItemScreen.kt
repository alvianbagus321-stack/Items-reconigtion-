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
            Text("Photos (Minimum 2 required for AI)", style = MaterialTheme.typography.titleMedium)
            
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
                            Text("${currentSize}/2", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (uiState.isAnalyzing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Analyzing image with AI...", style = MaterialTheme.typography.bodySmall)
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
                enabled = uiState.name.isNotBlank() && uiState.images.size >= 2
            ) {
                Text("Save Item")
            }
            if (uiState.images.size < 2) {
                Text(
                    text = "Please add at least 2 photos from different angles.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
