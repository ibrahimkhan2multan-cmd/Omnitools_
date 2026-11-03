package com.omnitools.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omnitools.app.R
import com.omnitools.app.data.Category
import com.omnitools.app.data.Tool
import com.omnitools.app.data.ToolRegistry
import com.omnitools.app.ui.components.EmptyState
import com.omnitools.app.ui.components.Entrance
import com.omnitools.app.ui.components.IconBadge
import com.omnitools.app.ui.components.OmniLogo
import com.omnitools.app.ui.components.pressable
import com.omnitools.app.ui.components.SectionTitle
import com.omnitools.app.ui.components.ToolRow

@Composable
fun HomeScreen(
    favorites: Set<String>,
    recents: List<String>,
    showRecents: Boolean,
    onOpenTool: (String) -> Unit,
    onOpenCategory: (Category) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { ToolRegistry.search(query) }
    val recentTools = remember(recents) { recents.mapNotNull { ToolRegistry.find(it) } }
    val favoriteTools = remember(favorites) { ToolRegistry.all.filter { it.id in favorites } }
    val suggested = remember(recents, favorites) {
        ToolRegistry.all.filter { it.id !in recents && it.id !in favorites }.take(4)
    }
    val categories = remember { ToolRegistry.activeCategories() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            item { Header(onOpenSettings) }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search tools") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (query.isNotBlank()) {
                if (results.isEmpty()) {
                    item { EmptyState("No tools match \"${query.trim()}\".") }
                } else {
                    item { SectionTitle("${results.size} result${if (results.size == 1) "" else "s"}") }
                    items(results, key = { "s_" + it.id }) { tool ->
                        Entrance("s_${tool.id}", index = 0, modifier = Modifier.animateItem()) {
                            ToolRow(tool, tool.id in favorites, { onOpenTool(tool.id) }, { onToggleFavorite(tool.id) })
                        }
                    }
                }
            } else {
                if (showRecents && recentTools.isNotEmpty()) {
                    item { SectionTitle("Recently used") }
                    items(recentTools.take(3), key = { "r_" + it.id }) { tool ->
                        Entrance("r_${tool.id}", index = 0, modifier = Modifier.animateItem()) {
                            ToolRow(tool, tool.id in favorites, { onOpenTool(tool.id) }, { onToggleFavorite(tool.id) })
                        }
                    }
                }
                if (favoriteTools.isNotEmpty()) {
                    item { SectionTitle("Favorites") }
                    items(favoriteTools, key = { "f_" + it.id }) { tool ->
                        Entrance("f_${tool.id}", index = 0, modifier = Modifier.animateItem()) {
                            ToolRow(tool, true, { onOpenTool(tool.id) }, { onToggleFavorite(tool.id) })
                        }
                    }
                }
                item { SectionTitle("Categories") }
                itemsIndexed(categories.chunked(2), key = { _, row -> "c_" + row.joinToString { it.name } }) { rowIndex, row ->
                    Entrance("cr_${row.joinToString { it.name }}", index = rowIndex + 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { category ->
                                CategoryCard(category, Modifier.weight(1f)) { onOpenCategory(category) }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (suggested.isNotEmpty()) {
                    item { SectionTitle("Suggested") }
                    items(suggested, key = { "g_" + it.id }) { tool ->
                        Entrance("g_${tool.id}", index = 0, modifier = Modifier.animateItem()) {
                            ToolRow(tool, tool.id in favorites, { onOpenTool(tool.id) }, { onToggleFavorite(tool.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OmniLogo(size = 46.dp, modifier = Modifier.padding(start = 2.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("OmniTools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "All-in-One Utility Suite",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
private fun CategoryCard(category: Category, modifier: Modifier, onClick: () -> Unit) {
    val count = ToolRegistry.inCategory(category).size
    Card(
        modifier = modifier.pressable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IconBadge(category.icon, size = 40)
            Text(category.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "$count tool${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
