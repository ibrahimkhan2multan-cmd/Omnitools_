package com.omnitools.app.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnitools.app.data.Category
import com.omnitools.app.data.ToolRegistry
import com.omnitools.app.ui.components.EmptyState
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.ToolRow

@Composable
fun CategoryScreen(
    category: Category,
    favorites: Set<String>,
    onBack: () -> Unit,
    onOpenTool: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val tools = ToolRegistry.inCategory(category)
    ScreenScaffold(title = category.title, onBack = onBack) { padding ->
        if (tools.isEmpty()) {
            EmptyState("No tools in this category yet.", Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(tools, key = { it.id }) { tool ->
                    ToolRow(tool, tool.id in favorites, { onOpenTool(tool.id) }, { onToggleFavorite(tool.id) })
                }
            }
        }
    }
}
