package cn.irina.thepitaddon.utils

import java.security.SecureRandom
import java.util.*

class RandomUtil {
    private val random: Random = Random()
    private val secureRandom: SecureRandom = SecureRandom()

    private fun secureRandomDouble(): Double {
        return (secureRandom.nextDouble() + System.nanoTime().toDouble() % 1e9 / 1e9 + random.nextDouble()) % 1.0
    }

    fun hasSuccessfullyByChance(chance: Double): Boolean {
        require(chance in 0.0..1.0)
        return when {
            chance <= 0.0 -> false
            chance >= 1.0 -> true
            else -> secureRandomDouble() < chance
        }
    }
}