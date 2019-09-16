package com.joshtalks.joshskills.util

import java.security.SecureRandom
import java.util.*

class RandomString @JvmOverloads constructor(
    length: Int = 21,
    random: Random = SecureRandom(),
    symbols: String = alphanum
) {

    private val random: Random

    private val symbols: CharArray

    private val buf: CharArray

    /**
     * Generate a random string.
     */
    fun nextString(): String {
        for (idx in buf.indices)
            buf[idx] = symbols[random.nextInt(symbols.size)]
        return String(buf)
    }

    init {
        require(length >= 1)
        require(symbols.length >= 2)
        this.random = Objects.requireNonNull(random)
        this.symbols = symbols.toCharArray()
        this.buf = CharArray(length)
    }

    companion object {

        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        val lower = upper.toLowerCase(Locale.ROOT)

        val digits = "0123456789"

        val alphanum = upper + lower + digits
    }

}