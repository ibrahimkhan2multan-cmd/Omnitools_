package com.omnitools.app.tools.calc

/** Percentage and discount maths. Throws [IllegalArgumentException] with a readable message for impossible inputs. */
object PercentCalc {

    /** What is [percent]% of [value]? */
    fun percentOf(percent: Double, value: Double): Double = value * percent / 100.0

    /** [part] is what percent of [whole]? */
    fun whatPercent(part: Double, whole: Double): Double {
        require(whole != 0.0) { "The whole cannot be zero." }
        return part / whole * 100.0
    }

    /** Percentage change from [from] to [to] (positive = increase). */
    fun change(from: Double, to: Double): Double {
        require(from != 0.0) { "The starting value cannot be zero." }
        return (to - from) / Math.abs(from) * 100.0
    }

    data class Discount(val saved: Double, val afterDiscount: Double, val tax: Double, val total: Double)

    /** Applies a discount first, then adds tax on the discounted price. */
    fun discount(price: Double, discountPercent: Double, taxPercent: Double = 0.0): Discount {
        require(price >= 0) { "Price cannot be negative." }
        require(discountPercent in 0.0..100.0) { "Discount must be between 0 and 100%." }
        require(taxPercent >= 0) { "Tax cannot be negative." }
        val saved = price * discountPercent / 100.0
        val after = price - saved
        val tax = after * taxPercent / 100.0
        return Discount(saved, after, tax, after + tax)
    }
}
