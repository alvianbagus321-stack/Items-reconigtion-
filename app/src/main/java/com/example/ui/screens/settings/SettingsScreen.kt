package com.example.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var apiKey by remember { mutableStateOf(prefs.getString("gemini_api_key", "") ?: "") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    val searchBarColor by SettingsManager.searchBarColor.collectAsState()
    val searchBarOpacity by SettingsManager.searchBarOpacity.collectAsState()
    val searchBarBlur by SettingsManager.searchBarBlur.collectAsState()
    val backgroundColor by SettingsManager.backgroundColor.collectAsState()
    val backgroundImageUri by SettingsManager.backgroundImageUri.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { SettingsManager.setBackgroundImageUri(it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "API Configurations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                "To use the AI visual search and recognition feature, you need a Gemini API Key. You can get one for free at Google AI Studio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    prefs.edit().putString("gemini_api_key", apiKey).apply()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Settings saved successfully!")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Configuration")
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                "Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            val currency by SettingsManager.currency.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Primary Currency", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(currency)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Rp", "$", "€", "£", "¥").forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr) },
                                onClick = {
                                    SettingsManager.setCurrency(curr)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                "UI Customization",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text("Search Bar Blur Radius: ${searchBarBlur.toInt()}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = searchBarBlur,
                onValueChange = { SettingsManager.setSearchBarBlur(it) },
                valueRange = 0f..50f
            )

            Text("Search Bar Opacity: ${(searchBarOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = searchBarOpacity,
                onValueChange = { SettingsManager.setSearchBarOpacity(it) },
                valueRange = 0f..1f
            )
            
            Text("Search Bar Color", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Color.Unspecified, Color.Red, Color.Green, Color.Blue, Color.Cyan, Color.Magenta, Color.Yellow, Color.Black, Color.White).forEach { color ->
                    val isSelected = searchBarColor == color
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(if (color == Color.Unspecified) MaterialTheme.colorScheme.surfaceVariant else color, RoundedCornerShape(16.dp))
                            .clickable { SettingsManager.setSearchBarColor(color) }
                            .padding(2.dp)
                    ) {
                        if (isSelected) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha=0.5f), RoundedCornerShape(16.dp)))
                        }
                    }
                }
            }

            Text("Background Color", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Color.Unspecified, Color.Red.copy(alpha=0.2f), Color.Green.copy(alpha=0.2f), Color.Blue.copy(alpha=0.2f), Color.Cyan.copy(alpha=0.2f), Color.LightGray).forEach { color ->
                    val isSelected = backgroundColor == color
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(if (color == Color.Unspecified) MaterialTheme.colorScheme.background else color, RoundedCornerShape(16.dp))
                            .clickable { SettingsManager.setBackgroundColor(color) }
                            .padding(2.dp)
                    ) {
                        if (isSelected) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha=0.5f), RoundedCornerShape(16.dp)))
                        }
                    }
                }
            }

            Text("Background Image", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Select Image")
                }
                if (backgroundImageUri != null) {
                    Button(onClick = { SettingsManager.setBackgroundImageUri(null) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Clear")
                    }
                }
            }
            if (backgroundImageUri != null) {
                AsyncImage(
                    model = backgroundImageUri,
                    contentDescription = "Background Preview",
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
    }
}
