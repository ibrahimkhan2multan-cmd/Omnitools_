package com.omnitools.app.tools.calc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.SectionTitle

private const val BASES = "bases"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnitConverterScreen(onBack: () -> Unit) {
    var kindKey by remember { mutableStateOf(UnitConverter.Kind.LENGTH.name) }

    ScreenScaffold(title = "Unit Converter", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                UnitConverter.Kind.values().forEach { k ->
                    FilterChip(selected = kindKey == k.name, onClick = { kindKey = k.name }, label = { Text(k.title) })
                }
                FilterChip(selected = kindKey == BASES, onClick = { kindKey = BASES }, label = { Text("Number bases") })
            }
            if (kindKey == BASES) BaseConverter() else UnitPanel(UnitConverter.Kind.valueOf(kindKey))
        }
    }
}

@Composable
private fun UnitPanel(kind: UnitConverter.Kind) {
    val units = UnitConverter.unitsFor(kind)
    var fromIndex by remember(kind) { mutableStateOf(0) }
    var toIndex by remember(kind) { mutableStateOf(1) }
    var input by remember(kind) { mutableStateOf("1") }

    val value = input.toDoubleOrNull()
    val outcome: Pair<String?, String?> = when {
        input.isBlank() -> null to null
        value == null -> null to "Enter a valid number."
        else -> try {
            ExpressionEvaluator.format(UnitConverter.convert(kind, value, units[fromIndex], units[toIndex])) to null
        } catch (e: IllegalArgumentException) {
            null to (if (e.message == "Below absolute zero") "That is below absolute zero." else e.message)
        }
    }

    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        label = { Text("Value") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
    UnitDropdown("From", units, fromIndex) { fromIndex = it }
    UnitDropdown("To", units, toIndex) { toIndex = it }

    outcome.second?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    outcome.first?.let {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = com.omnitools.app.ui.components.cardBorder(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Result", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$it ${units[toIndex].symbol}", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
    if (kind == UnitConverter.Kind.DATA) {
        Text(
            "KB/MB/GB use 1000-based steps; KiB/MiB/GiB use 1024-based steps.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(label: String, units: List<UnitConverter.Unit>, selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "${units[selected].name} (${units[selected].symbol})",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEachIndexed { i, u ->
                DropdownMenuItem(text = { Text("${u.name} (${u.symbol})") }, onClick = { onSelect(i); expanded = false })
            }
        }
    }
}

@Composable
private fun BaseConverter() {
    var input by remember { mutableStateOf("255") }
    var fromBase by remember { mutableStateOf(10) }
    val targets = listOf(2, 8, 10, 16)

    SectionTitle("Input base")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        targets.forEach { b ->
            FilterChip(selected = fromBase == b, onClick = { fromBase = b }, label = { Text(baseName(b)) })
        }
    }
    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        label = { Text("Number") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    val results = targets.map { b ->
        b to try {
            UnitConverter.convertBase(input, fromBase, b) to null
        } catch (e: IllegalArgumentException) {
            null to e.message
        }
    }
    results.firstOrNull { it.second.second != null }?.second?.second?.let {
        Text(it, color = MaterialTheme.colorScheme.error)
    }
    if (results.all { it.second.first != null }) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = com.omnitools.app.ui.components.cardBorder(), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                results.forEach { (b, r) ->
                    Text("${baseName(b)}: ${r.first}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    Text(
        "Whole numbers only; very large values are supported.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun baseName(b: Int) = when (b) {
    2 -> "Binary"
    8 -> "Octal"
    10 -> "Decimal"
    16 -> "Hex"
    else -> "Base $b"
}
