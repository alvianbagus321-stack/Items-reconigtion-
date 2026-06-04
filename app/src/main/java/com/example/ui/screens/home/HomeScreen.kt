package com.example.ui.screens.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.local.ItemEntity
import com.example.ui.screens.settings.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val filteredItems by viewModel.filteredItems.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    
    val searchBarColor by SettingsManager.searchBarColor.collectAsState()
    val searchBarOpacity by SettingsManager.searchBarOpacity.collectAsState()
    val searchBarBlur by SettingsManager.searchBarBlur.collectAsState()
    val backgroundColor by SettingsManager.backgroundColor.collectAsState()
    val backgroundImageUri by SettingsManager.backgroundImageUri.collectAsState()

    var expandedGroups by remember { mutableStateOf(setOf<String>()) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            viewModel.exportData(context, uri) { success ->
                Toast.makeText(context, if (success) "Config saved successfully" else "Failed to save config", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.importData(context, uri) { success ->
                Toast.makeText(context, if (success) "Config imported successfully" else "Failed to import config", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Custom Background Image
        if (backgroundImageUri != null) {
            AsyncImage(
                model = backgroundImageUri,
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Scaffold(
            containerColor = if (backgroundImageUri != null) Color.Transparent else (if (backgroundColor == Color.Unspecified) MaterialTheme.colorScheme.background else backgroundColor),
            topBar = {
                TopAppBar(
                    title = { Text("Inventory", fontWeight = FontWeight.ExtraBold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (backgroundImageUri != null) Color.Transparent else (if (backgroundColor == Color.Unspecified) MaterialTheme.colorScheme.background else backgroundColor),
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                    actions = {
                        IconButton(onClick = { 
                            viewModel.updateSearchQuery(searchQuery)
                            Toast.makeText(context, "Refreshed", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                        FilledTonalIconButton(onClick = { showCreateGroupDialog = true }) {
                            Icon(Icons.Filled.CreateNewFolder, contentDescription = "Create Group")
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save Config") },
                                onClick = {
                                    showMenu = false
                                    exportLauncher.launch("smart_inventory_backup.json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import Config") },
                                onClick = {
                                    showMenu = false
                                    importLauncher.launch(arrayOf("application/json", "*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("settings")
                                }
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("camera") },
                    modifier = Modifier.testTag("scan_button"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = { Icon(Icons.Filled.PhotoCamera, contentDescription = "Scan New Item") },
                    text = { Text("Scan Item") }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val tfColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = (if (searchBarColor == Color.Unspecified) MaterialTheme.colorScheme.surface else searchBarColor).copy(alpha = searchBarOpacity),
                        unfocusedContainerColor = (if (searchBarColor == Color.Unspecified) MaterialTheme.colorScheme.surface else searchBarColor).copy(alpha = searchBarOpacity)
                    )
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .blur(radius = searchBarBlur.dp),
                        placeholder = { Text("Search inventory...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = { 
                            IconButton(onClick = { navController.navigate("camera?mode=search") }) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = "Visual Search", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = tfColors
                    )
                }

                if (filteredItems.isEmpty() && (groups.isEmpty() || searchQuery.isNotBlank())) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No items found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("Tap 'Scan Item' to add your first inventory item.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val grouped = filteredItems.groupBy { it.groupId }
                        
                        // Show ungroupped items first or last. Let's do it first if any.
                        val ungroupped = grouped[null] ?: emptyList()
                        if (ungroupped.isNotEmpty()) {
                            items(ungroupped, key = { it.id }) { item ->
                                ItemCard(item = item) {
                                    navController.navigate("item_detail/${item.id}")
                                }
                            }
                        }

                        // Show groups
                        groups.forEach { group ->
                            val groupItems = grouped[group.id] ?: emptyList()
                            val isExpanded = expandedGroups.contains(group.id)
                            if (groupItems.isNotEmpty() || searchQuery.isBlank()) {
                                item(key = "group_${group.id}", span = { GridItemSpan(maxLineSpan) }) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 4.dp)
                                            .clickable {
                                                expandedGroups = if (isExpanded) {
                                                    expandedGroups - group.id
                                                } else {
                                                    expandedGroups + group.id
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isExpanded) Icons.Filled.KeyboardArrowDown 
                                            else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Toggle",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Filled.Folder, contentDescription = "Folder", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${group.name} (${groupItems.size})", 
                                            style = MaterialTheme.typography.bodyLarge, 
                                            fontWeight = FontWeight.SemiBold, 
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { viewModel.deleteGroup(group.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete Group", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                if (isExpanded || searchQuery.isNotBlank()) {
                                    items(groupItems, key = { it.id }) { item ->
                                        ItemCard(item = item) {
                                            navController.navigate("item_detail/${item.id}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        var newGroupName by remember { mutableStateOf("") }
        var newGroupDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create New Data Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Group Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newGroupDesc,
                        onValueChange = { newGroupDesc = it },
                        label = { Text("Group Description") },
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            viewModel.createGroup(newGroupName, newGroupDesc)
                            showCreateGroupDialog = false
                        }
                    },
                    enabled = newGroupName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ItemCard(item: ItemEntity, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val currency by com.example.ui.screens.settings.SettingsManager.currency.collectAsState()
    OutlinedCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).testTag("task_item_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = item.mainImageUri.takeIf { it.isNotBlank() },
                contentDescription = item.name,
                modifier = Modifier.aspectRatio(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                error = coil.compose.rememberAsyncImagePainter(android.R.drawable.ic_menu_gallery)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "$currency ${if(item.price % 1.0 == 0.0) item.price.toLong().toString() else String.format("%.2f", item.price)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        }
    }
}
