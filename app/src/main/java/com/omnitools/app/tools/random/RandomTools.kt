package com.omnitools.app.tools.random

import java.security.SecureRandom
import java.util.Random

/** Dice, coin and random-number helpers. A [Random] can be injected so results are testable. */
object RandomTools {

    const val MAX_DICE = 20
    const val MAX_RANGE_COUNT = 100

    private val secure = SecureRandom()

    data class DiceRoll(val sides: Int, val rolls: List<Int>) {
        val total: Int get() = rolls.sum()
    }

    fun rollDice(count: Int, sides: Int, random: Random = secure): DiceRoll {
        require(count in 1..MAX_DICE) { "Roll between 1 and $MAX_DICE dice." }
        require(sides in 2..1000) { "A die needs between 2 and 1000 sides." }
        return DiceRoll(sides, List(count) { random.nextInt(sides) + 1 })
    }

    /** Returns true for heads. */
    fun flipCoin(random: Random = secure): Boolean = random.nextBoolean()

    /** [count] integers between [min] and [max], both inclusive. Duplicates are allowed. */
    fun numbers(min: Long, max: Long, count: Int, random: Random = secure): List<Long> {
        require(min <= max) { "The minimum must not be larger than the maximum." }
        require(count in 1..MAX_RANGE_COUNT) { "Ask for between 1 and $MAX_RANGE_COUNT numbers." }
        val span = max - min + 1
        require(span > 0) { "That range is too large." }
        return List(count) { min + Math.floorMod(random.nextLong(), span) }
    }

    /** Picks one item, or throws when the list is empty. */
    fun pick(items: List<String>, random: Random = secure): String {
        require(items.isNotEmpty()) { "Enter at least one option." }
        return items[random.nextInt(items.size)]
    }
}
