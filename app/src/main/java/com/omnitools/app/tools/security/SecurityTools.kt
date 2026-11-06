package com.omnitools.app.tools.security

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ln
import kotlin.math.pow

/** A rough, honest estimate of how hard a password is to guess. It is a guide, not a guarantee. */
object PasswordStrength {
    data class Result(val bits: Double, val rating: String, val crackTime: String, val tips: List<String>)

    private val common = setOf(
        "password", "123456", "12345678", "123456789", "qwerty", "abc123", "111111", "letmein", "welcome", "monkey", "dragon", "iloveyou",
        "admin", "login", "passw0rd", "master", "sunshine", "princess", "football", "baseball", "shadow", "superman", "qwerty123", "1q2w3e4r",
        "000000", "123123", "654321", "pakistan", "password1", "p@ssw0rd", "changeme", "trustno1", "whatever", "freedom", "hello123",
    )
    private val sequences = listOf("abcdefghijklmnopqrstuvwxyz", "0123456789", "qwertyuiop", "asdfghjkl", "zxcvbnm", "9876543210")

    fun poolSize(p: String): Int {
        var pool = 0
        if (p.any { it in 'a'..'z' }) pool += 26
        if (p.any { it in 'A'..'Z' }) pool += 26
        if (p.any { it in '0'..'9' }) pool += 10
        if (p.any { it in "!@#$%^&*()-_=+[]{};:'\",.<>/?\\|`~ " }) pool += 33
        if (p.any { it.code > 127 }) pool += 100
        return pool
    }

    fun evaluate(password: String): Result {
        require(password.isNotEmpty()) { "Enter a password to check." }
        val tips = ArrayList<String>()
        val lower = password.lowercase()
        if (lower in common || lower.trimEnd { it.isDigit() } in common) {
            return Result(8.0, "Very weak", "instantly", listOf("This is one of the most common passwords. Never use it."))
        }
        var bits = password.length * ln(poolSize(password).coerceAtLeast(2).toDouble()) / ln(2.0)
        // Penalties for patterns that make guessing easier.
        val repeats = Regex("(.)\\1{2,}").findAll(password).sumOf { it.value.length - 1 }
        if (repeats > 0) { bits -= repeats * 3.0; tips.add("Avoid repeating the same character.") }
        var seq = 0
        for (s in sequences) for (len in 4..s.length) for (st in 0..s.length - len) if (lower.contains(s.substring(st, st + len))) seq = maxOf(seq, len)
        if (seq >= 4) { bits -= seq * 3.0; tips.add("Avoid runs like 1234 or abcd.") }
        if (Regex("(19|20)\\d\\d$").containsMatchIn(password)) { bits -= 8.0; tips.add("A year at the end is easy to guess.") }
        if (common.any { it.length >= 5 && lower.contains(it) }) { bits -= 15.0; tips.add("It contains a very common word.") }
        if (password.length < 12) tips.add("Use at least 12 characters.")
        if (poolSize(password) < 62) tips.add("Mix upper and lower case letters, digits and symbols.")
        bits = bits.coerceAtLeast(1.0)
        val rating = when {
            bits < 28 -> "Very weak"
            bits < 40 -> "Weak"
            bits < 60 -> "Fair"
            bits < 80 -> "Strong"
            else -> "Very strong"
        }
        return Result(bits, rating, crackTime(bits), tips.distinct())
    }

    /** Time to try half of all possibilities at 10 billion guesses per second (a fast offline attack). */
    fun crackTime(bits: Double): String {
        val seconds = 2.0.pow(bits - 1) / 1e10
        return when {
            seconds < 1 -> "less than a second"
            seconds < 60 -> "${seconds.toLong()} seconds"
            seconds < 3600 -> "${(seconds / 60).toLong()} minutes"
            seconds < 86400 -> "${(seconds / 3600).toLong()} hours"
            seconds < 86400 * 365 -> "${(seconds / 86400).toLong()} days"
            seconds < 86400.0 * 365 * 1000 -> "${(seconds / 86400 / 365).toLong()} years"
            seconds < 86400.0 * 365 * 1e9 -> "thousands to millions of years"
            else -> "longer than the age of the universe"
        }
    }
}

/** Random passphrases from a list of about 500 plain four-letter words: each word adds roughly 9 bits. */
object Passphrase {
    private val raw = """
        able acid aged also area army away baby back ball band bank base bath bear beat been bell belt best bike bill bird blue boat body bold bone book boot born boss both bowl bulk burn bush busy cafe cake call calm came camp card care cart case cash cast cave cell chat chef chin city clay clip club coal coat code coin cold come cook cool cope copy cord core cost crew crop dark data date dawn dear deep deer desk dial dice diet dish dive door dose down draw drew drop drum dual duck dust duty each earn east easy edge else even ever exit face fact fair fall farm fast fate feed feel fell file fill film find fine fire firm fish five flag flat flew flow foam fold folk food foot form fort four free from fuel full fund gain game gate gave gear gift girl give glad glow goal goat gold golf gone good grab gray grew grid grow gulf hair half hall hand hang hard harm hate have head heal hear heat held help herb here hero hide high hill hint hold hole holy home hope horn host hour huge hunt hurt idea inch iron isle item jack jade jazz join joke jump june jury just keen keep kept kick kind king kite knee knew know lack lake lamp land lane last late lead leaf lean left lend lens less life lift like lime line link lion list live load loan lock logo long look loop lord lost loud love luck made mail main make male mall many mark mask mass meal mean meat meet melt menu mild mile milk mind mine mint miss mode moon more most move much must name navy near neck need nest news nice nine node none noon nose note odds once only open oral oven pace pack page paid pain pair palm park part pass past path peak pearl pick pile pine pink pipe plan play plot plus poem pole pond pool port pose post pour pray pull pure push race rail rain rank rare rate read real rent rest rice rich ride ring rise risk road rock role roll roof room root rope rose rule rush safe sail salt same sand save seat seed seek self sell send ship shop show shut sick side sign silk sing sink site size skin slim slip slow snow soft soil sold sole some song soon sort soul soup spot star stay step stop such suit sure swim tail take tale talk tall tank tape task team tell tent term test text than that them then thin this tide tile time tiny tire told tone tool tour town toys tree trim trip true tube tune turn twin type unit upon urge used user vary vast very view vote wait wake walk wall want warm wash wave weak wear week well went were west what when whom wide wife wild will wind wine wing wire wise wish with wolf wood wool word wore work yard yarn year zero zone
    """.trim().split(Regex("\\s+"))

    val words: List<String> = raw.distinct()

    fun bitsPerWord(): Double = ln(words.size.toDouble()) / ln(2.0)

    const val MIN_WORDS = 3
    const val MAX_WORDS = 12

    data class Result(val text: String, val bits: Int)

    fun generate(count: Int, separator: String = "-", capitalize: Boolean = false, addNumber: Boolean = false, random: java.util.Random = SecureRandom()): Result {
        require(count in MIN_WORDS..MAX_WORDS) { "Use between $MIN_WORDS and $MAX_WORDS words." }
        val picked = List(count) { words[random.nextInt(words.size)].let { w -> if (capitalize) w.replaceFirstChar { c -> c.uppercase() } else w } }.toMutableList()
        var bits = (count * bitsPerWord()).toInt()
        if (addNumber) { picked[picked.lastIndex] = picked.last() + random.nextInt(100); bits += 6 }
        return Result(picked.joinToString(separator), bits)
    }
}

/** Password-based text encryption: AES-256-GCM with a key made by PBKDF2-HMAC-SHA256. Output starts with "OT1.". */
object TextCrypto {
    private const val PREFIX = "OT1."
    private const val ITERATIONS = 210_000
    private const val SALT = 16
    private const val IV = 12
    const val MIN_PASSWORD = 6
    private val random = SecureRandom()

    private fun key(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    fun encrypt(text: String, password: String, iterations: Int = ITERATIONS): String {
        require(text.isNotEmpty()) { "Enter some text to encrypt." }
        require(password.length >= MIN_PASSWORD) { "Use a password of at least $MIN_PASSWORD characters." }
        val salt = ByteArray(SALT).also { random.nextBytes(it) }
        val iv = ByteArray(IV).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password.toCharArray(), salt, iterations), GCMParameterSpec(128, iv))
        val body = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
        val all = salt + iv + body
        return PREFIX + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(all)
    }

    fun decrypt(token: String, password: String, iterations: Int = ITERATIONS): String {
        val t = token.trim().filter { !it.isWhitespace() }
        require(t.startsWith(PREFIX)) { "This is not text made by OmniTools (it should start with $PREFIX)." }
        require(password.isNotEmpty()) { "Enter the password." }
        val all = try { java.util.Base64.getUrlDecoder().decode(t.removePrefix(PREFIX)) } catch (e: IllegalArgumentException) { throw IllegalArgumentException("The encrypted text is damaged.") }
        require(all.size > SALT + IV + 16) { "The encrypted text is damaged." }
        val salt = all.copyOfRange(0, SALT)
        val iv = all.copyOfRange(SALT, SALT + IV)
        val body = all.copyOfRange(SALT + IV, all.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password.toCharArray(), salt, iterations), GCMParameterSpec(128, iv))
        return try {
            String(cipher.doFinal(body), Charsets.UTF_8)
        } catch (e: AEADBadTagException) {
            throw IllegalArgumentException("Wrong password, or the text was changed.")
        } catch (e: javax.crypto.BadPaddingException) {
            throw IllegalArgumentException("Wrong password, or the text was changed.")
        }
    }
}
