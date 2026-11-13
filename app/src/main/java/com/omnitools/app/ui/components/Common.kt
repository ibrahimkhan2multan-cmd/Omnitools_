package com.omnitools.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitools.app.data.Tool
import com.omnitools.app.data.Category
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border

/** Standard screen frame: top bar with back arrow and optional actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge.copy(lineHeight = 26.sp), maxLines = 2) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Hairline border used on every card so surfaces read as one family. */
@Composable
fun cardBorder() = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

/** The app's standard card: surface color, hairline border, no shadow. */
@Composable
fun OmniCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder(),
        shape = RoundedCornerShape(18.dp),
        content = content,
    )
}

/** A clearly tappable row: icon, title, optional subtitle and a trailing icon (arrow by default). */
@Composable
fun LinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: ImageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).pressable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(icon, size = 40)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(trailing, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Rounded badge that holds a tool/category icon: one calm accent color everywhere, so lists stay easy to scan. */
@Composable
fun IconBadge(icon: ImageVector, size: Int = 44, accent: Color = Color.Unspecified) {
    val base = if (accent == Color.Unspecified) MaterialTheme.colorScheme.primary else accent
    // On light screens the bright category colours are darkened a little so the icon keeps good contrast.
    val onLight = MaterialTheme.colorScheme.onBackground.luminance() < 0.5f
    val c = if (onLight && accent != Color.Unspecified) androidx.compose.ui.graphics.lerp(base, Color.Black, 0.30f) else base
    val shape = RoundedCornerShape((size * 0.30f).dp)
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(c.copy(alpha = 0.22f), c.copy(alpha = 0.10f))), shape)
            .border(1.dp, c.copy(alpha = 0.28f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c, modifier = Modifier.size((size * 0.52f).dp))
    }
}

/** Each category gets its own colour, so the dashboard is easy to scan and looks alive. */
fun Category.accent(): Color = when (this) {
    Category.FILES -> Color(0xFFF2A93B)
    Category.DEV -> Color(0xFF58A6FF)
    Category.PDF -> Color(0xFFFF6B6B)
    Category.IMAGES -> Color(0xFFB07CFF)
    Category.QR -> Color(0xFF3DD6B0)
    Category.DEVICE -> Color(0xFF7CC4FF)
    Category.NOTES -> Color(0xFFFFD166)
    Category.CALC -> Color(0xFF5BE0A0)
    Category.PRODUCTIVITY -> Color(0xFFFF8FB1)
    Category.SECURITY -> Color(0xFF6FA8FF)
    Category.NETWORK -> Color(0xFF4FD1E8)
    Category.ADVANCED -> Color(0xFFFF9F5A)
    Category.GAMING -> Color(0xFFE56BFF)
}

/** A full-width row for one tool, with a favorite star. */
@Composable
fun ToolRow(
    tool: Tool,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pressable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(tool.icon, accent = tool.category.accent())
            Column(modifier = Modifier.weight(1f)) {
                Text(tool.name, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FavoriteStar(isFavorite, onToggleFavorite)
        }
    }
}

/** Star that pops with a springy bounce when it is switched on, and fades its color when switched off. */
@Composable
fun FavoriteStar(isFavorite: Boolean, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(isFavorite) {
        if (first) {
            first = false
        } else if (isFavorite) {
            scale.snapTo(0.5f)
            scale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
    }
    val tint by animateColorAsState(
        if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "star tint",
    )
    IconButton(onClick = onClick) {
        Icon(
            if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            tint = tint,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        )
    }
}

/** Small centered message used for empty states. */
@Composable
fun FeatureEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color.Unspecified,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(icon, size = 72, accent = accent)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        androidx.compose.material3.Button(onClick = onAction) { Text(actionLabel) }
    }
}

/** A short message in the middle of an empty list. */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
