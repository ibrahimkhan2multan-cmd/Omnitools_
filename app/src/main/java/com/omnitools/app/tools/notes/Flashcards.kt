package com.omnitools.app.tools.notes

import com.omnitools.app.tools.advanced.Json

/** Question-and-answer cards for studying. Kept as one small JSON file in the app's private storage. */
object Flashcards {
    data class Card(val id: Long, val front: String, val back: String, val known: Boolean = false)

    const val MAX_CARDS = 2000
    const val MAX_TEXT = 1000

    fun encode(cards: List<Card>): String =
        cards.joinToString(",", "[", "]") { """{"id":${it.id},"f":${Json.quote(it.front)},"b":${Json.quote(it.back)},"k":${it.known}}""" }

    /** Reads cards back. Anything unreadable is dropped instead of crashing. */
    fun decode(text: String): List<Card> {
        if (text.isBlank()) return emptyList()
        return try {
            val root = Json.parse(text) as? Json.Node.Arr ?: return emptyList()
            root.items.mapNotNull { n ->
                val o = n as? Json.Node.Obj ?: return@mapNotNull null
                fun field(k: String) = o.entries.firstOrNull { it.first == k }?.second
                val f = (field("f") as? Json.Node.Str)?.value ?: return@mapNotNull null
                val b = (field("b") as? Json.Node.Str)?.value ?: return@mapNotNull null
                val id = (field("id") as? Json.Node.Num)?.text?.toLongOrNull() ?: return@mapNotNull null
                val k = (field("k") as? Json.Node.Bool)?.value ?: false
                if (f.isBlank() || b.isBlank()) null else Card(id, f.take(MAX_TEXT), b.take(MAX_TEXT), k)
            }.take(MAX_CARDS)
        } catch (e: Json.JsonException) {
            emptyList()
        }
    }

    fun add(cards: List<Card>, front: String, back: String, id: Long): List<Card> {
        val f = front.trim(); val b = back.trim()
        require(f.isNotEmpty() && b.isNotEmpty()) { "Both sides of the card need some text." }
        require(cards.size < MAX_CARDS) { "You can keep up to $MAX_CARDS cards." }
        return cards + Card(id, f.take(MAX_TEXT), b.take(MAX_TEXT))
    }

    fun mark(cards: List<Card>, id: Long, known: Boolean) = cards.map { if (it.id == id) it.copy(known = known) else it }

    /** Cards still to learn, in a random order. */
    fun studyQueue(cards: List<Card>, random: java.util.Random = java.util.Random()): List<Card> =
        cards.filter { !it.known }.shuffled(random)
}
