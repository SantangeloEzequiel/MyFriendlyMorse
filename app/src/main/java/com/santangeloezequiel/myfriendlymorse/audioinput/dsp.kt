package com.santangeloezequiel.myfriendlymorse.audioinput

import java.util.ArrayDeque
import kotlin.math.cos
import kotlin.math.sin


// --- Goertzel y FrequencyFinder ajustados para buffers variables ---

class Goertzel(
    private val sampleRate: Float,
    private val bufferSize: Int,
    val targetFrequency: Float
) {
    private val omega = (2.0f * Math.PI.toFloat() * targetFrequency) / sampleRate
    private val cosine = cos(omega)
    private val sine = sin(omega)
    private val coeff = 2.0f * cosine

    private val accumulationFrames: Int = 5
    private var framesProcessed = 0

    private val window = FloatArray(bufferSize) { i ->
        (0.5f * (1f - cos(2f * Math.PI.toFloat() * i / (bufferSize - 1))))
    }

    private var q0 = 0f
    private var q1 = 0f
    private var q2 = 0f

    fun process(buffer: FloatArray) {
        val size = minOf(buffer.size, bufferSize)
        framesProcessed++
        if (framesProcessed >= accumulationFrames) {
            reset()
            framesProcessed = 0
        }

        for (i in 0 until size) {
            val sampledAndWindowed = buffer[i] * window[i]
            q0 = coeff * q1 - q2 + sampledAndWindowed
            q2 = q1
            q1 = q0
        }
    }

    fun getPower(): Double {
        val real = q1 - q2 * cosine
        val imag = q2 * sine
        return (real * real + imag * imag).toDouble() / (bufferSize * bufferSize)
    }

    fun reset() {
        q0 = 0f; q1 = 0f; q2 = 0f
    }
}

class FrequencyFinder(
    private val sampleRate: Float,
    private val bufferSize: Int,
    minFreq: Float = 80f,
    maxFreq: Float = 1200f,
    step: Float = 40f
) {
    private val detectors: List<Goertzel>
    private val powerAccumulator: MutableMap<Float, Double>
    private val frequencyHistory = ArrayDeque<Float>()
    private val maxHistorySize = 3
    var frameCount = 0

    init {
        val freqs = ArrayList<Float>()
        var f = minFreq
        while (f <= maxFreq) {
            freqs.add(f)
            f += step
        }

        detectors = freqs.map { Goertzel(sampleRate, bufferSize, it) }
        powerAccumulator = detectors.associate { it.targetFrequency to 0.0 }.toMutableMap()
    }

    fun process(buffer: FloatArray) {
        if (buffer.isEmpty()) return

        // No hacer copyOf: pasar el buffer directo
        detectors.forEach { detector ->
            val size = minOf(buffer.size, bufferSize)
            detector.process(buffer) // <-- sin copiar
            val currentPower = powerAccumulator[detector.targetFrequency] ?: 0.0
            powerAccumulator[detector.targetFrequency] = currentPower + detector.getPower()
        }
        frameCount++
    }


    fun getDetectedFrequency(): Float {
        if (frameCount == 0) return -1f

        val best = powerAccumulator.maxByOrNull { it.value }
        if (best == null || best.value < 0.0001) {
            frequencyHistory.clear()
            return -1f
        }

        frequencyHistory.addLast(best.key)
        if (frequencyHistory.size > maxHistorySize) frequencyHistory.removeFirst()

        return if (frequencyHistory.isEmpty()) best.key else frequencyHistory.average().toFloat()
    }

    fun softReset() {
        powerAccumulator.keys.forEach { powerAccumulator[it] = 0.0 }
        frameCount = 0
        detectors.forEach { it.reset() }
    }
}
