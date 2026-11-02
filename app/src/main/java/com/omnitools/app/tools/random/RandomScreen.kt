package com.omnitools.app.tools.random

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omnitools.app.ui.components.ErrorText
import com.omnitools.app.ui.components.NumberField
import com.omnitools.app.ui.components.ResultCard
import com.omnitools.app.ui.components.ScreenScaffold
import com.omnitools.app.ui.components.TextInput
import com.omnitools.app.util.Actions

private val tabs = listOf("Dice", "Coin", "Number", "Pick one")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RandomScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var output by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    var diceCount by remember { mutableStateOf("2") }
    var sides by remember { mutableStateOf("6") }
    var min by remember { mutableStateOf("1") }
    var max by remember { mutableStateOf("100") }
    var count by remember { mutableStateOf("1") }
    var options by remember { mutableStateOf("") }

    fun run(block: () -> List<Pair<String, String>>) {
        try {
            output = block()
            error = null
        } catch (e: IllegalArgumentException) {
            output = emptyList()
            error = e.message
        }
    }

    ScreenScaffold(title = "Dice, Coin & Random", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { i, label ->
                    FilterChip(selected = tab == i, onClick = { tab = i; output = emptyList(); error = null }, label = { Text(label) })
                }
            }
            when (tab) {
                0 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Number of dice", diceCount, { diceCount = it.filter(Char::isDigit) }, Modifier.weight(1f))
                        NumberField("Sides", sides, { sides = it.filter(Char::isDigit) }, Modifier.weight(1f))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 6, 8, 10, 12, 20, 100).forEach { s ->
                            FilterChip(selected = sides == s.toString(), onClick = { sides = s.toString() }, label = { Text("d$s") })
                        }
                    }
                    Button(onClick = {
                        run {
                            val roll = RandomTools.rollDice(
                                diceCount.toIntOrNull() ?: throw IllegalArgumentException("Enter the number of dice."),
                                sides.toIntOrNull() ?: throw IllegalArgumentException("Enter the number of sides."),
                            )
                            listOf("Rolls" to roll.rolls.joinToString(", "), "Total" to roll.total.toString())
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Roll") }
                }
                1 -> Button(onClick = {
                    run { listOf("Result" to if (RandomTools.flipCoin()) "Heads" else "Tails") }
                }, modifier = Modifier.fillMaxWidth()) { Text("Flip the coin") }
                2 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Minimum", min, { min = it }, Modifier.weight(1f))
                        NumberField("Maximum", max, { max = it }, Modifier.weight(1f))
                    }
                    NumberField("How many numbers", count, { count = it.filter(Char::isDigit) })
                    Button(onClick = {
                        run {
                            val nums = RandomTools.numbers(
                                min.toLongOrNull() ?: throw IllegalArgumentException("Minimum must be a whole number."),
                                max.toLongOrNull() ?: throw IllegalArgumentException("Maximum must be a whole number."),
                                count.toIntOrNull() ?: throw IllegalArgumentException("Enter how many numbers you want."),
                            )
                            listOf("Random number${if (nums.size == 1) "" else "s"}" to nums.joinToString(", "))
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Generate") }
                }
                else -> {
                    TextInput("Options, one per line", options, { options = it }, singleLine = false, minLines = 4)
                    Button(onClick = {
                        run { listOf("Picked" to RandomTools.pick(options.lines().map { it.trim() }.filter { it.isNotEmpty() })) }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Pick one") }
                }
            }
            ErrorText(error)
            output.forEach { (caption, value) -> ResultCard(caption, value) }
            if (output.isNotEmpty()) {
                OutlinedButton(
                    onClick = { Actions.copy(context, "Random result", output.joinToString("\n") { "${it.first}: ${it.second}" }) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Copy result") }
            }
        }
    }
}
