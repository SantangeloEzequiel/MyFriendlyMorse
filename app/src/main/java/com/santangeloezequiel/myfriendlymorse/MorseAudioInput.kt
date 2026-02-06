package com.santangeloezequiel.myfriendlymorse

// Mic imports
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.ArrayDeque // para FrequencyFinder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import android.os.Handler
import android.os.Looper

//clock
import android.os.SystemClock



private const val BUFFER_SIZE = 512   // cuantos bytes tendra cada callback
private const val UMBRAL = 0.00035      // Ajustado ligeramente > 0 para evitar ruido puro
private const val SAMPLE_RATE = 16000 //en Hz
private const val MIN_FRAME_SILENCE= 0 //


object MorseAudioInput {

    private var mic: MicrophoneInput? = null
    private var finder: FrequencyFinder = FrequencyFinder(SAMPLE_RATE.toFloat(), BUFFER_SIZE)
    private var goertzel: Goertzel? = null

    //Propiedades de Executor
    private val executor = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)

    private val mainHandler = Handler(Looper.getMainLooper())

    // Variables internas de Morse

    //para learning
    private var isLearning = true
    private var counterLearned = -1
    private val pulseStartTimes = mutableListOf<Long>()
    private val pulseRealTimes = mutableListOf<Long>()

    private val silenceStartTimes = mutableListOf<Long>()
    private val silenceRealTimes = mutableListOf<Long>()

    //para en general
    private var umbralMorse : Long = 0

    private var changeToSound = true
    private var changeToSilence = false
    private var pulseRealTime:Long = 0
    private var pulseStartTime:Long = 0
    private var silenceRealTime:Long = 0
    private var silenceStartTime:Long = 0
    private var dotprom : Long = 0 //promedio duracion dot





    fun start(context: Context, morseSignal: StringBuffer) {

        stop()
        running.set(true)

        mic = MicrophoneInput(context, SAMPLE_RATE, BUFFER_SIZE) { buffer ->
            // Hilo de audio solo encola la tarea; el procesamiento pesado corre en executor
            if (!running.get()) return@MicrophoneInput

            // Copia el buffer solo una vez para no bloquear el audio thread
            val copy = buffer.copyOf()
            executor.execute {
                try {
                    if (!running.get()) return@execute


                    if (finder.frameCount >= 10) {
                        finder.softReset()
                        //  reset()
                    }
                    finder.process(copy)

                    val freq = finder.getDetectedFrequency()

                    if (freq > 0) {
                        if (goertzel == null || goertzel!!.targetFrequency != freq) {
                            goertzel = Goertzel(SAMPLE_RATE.toFloat(), BUFFER_SIZE, freq)
                        } else {
                            goertzel!!.reset()
                        }

                        goertzel!!.process(copy)
                        val power = goertzel!!.getPower()





                        // ===============================
                        //      LOGICA MORSE
                        // ===============================



                        if(isLearning){
                            val now :Long = SystemClock.elapsedRealtime()
                            if (power > UMBRAL) {
                                Log.d("MorseAudioInput", "OOOOOO")
                                if(changeToSound){
                                    changeToSound=false
                                    changeToSilence=true
                                    pulseStartTimes.add(now)
                                    if(counterLearned>-1)silenceRealTimes.add(now - silenceStartTimes.get(counterLearned))
                                    counterLearned++
                                }

                            } else {
                                Log.d("MorseAudioInput", "-----")
                                if(changeToSilence){
                                    changeToSound=true
                                    changeToSilence=false
                                    if(counterLearned>-1){
                                        silenceStartTimes.add((now))
                                        pulseRealTimes.add(now - pulseStartTimes.get(counterLearned))
                                        if(counterLearned>0){
                                            if(2 * minOf(pulseRealTimes.get(counterLearned-1),pulseRealTimes.get(counterLearned))< maxOf(pulseRealTimes.get(counterLearned-1),pulseRealTimes.get(counterLearned))){
                                                isLearning=false
                                                //Ultimo codigo de learning
                                                val prom = (pulseRealTimes.sum() - pulseRealTimes.get(counterLearned)) / (counterLearned-1)
                                                umbralMorse = (prom + pulseRealTimes.get(counterLearned))/2
                                                if(pulseRealTimes.get(counterLearned)>pulseRealTimes.get(counterLearned-1)) dotprom = pulseRealTimes.get(counterLearned)
                                                else dotprom = prom
                                                for (i in 0 until counterLearned){
                                                    if(pulseRealTimes.get(i)<=umbralMorse) synchronized(morseSignal) { morseSignal.append('•') }
                                                    else synchronized(morseSignal) { morseSignal.append('-') }
                                                    if(i<counterLearned){
                                                        if(silenceRealTimes.get(i)>=7*dotprom) synchronized(morseSignal) { morseSignal.append('\t') }
                                                        else if(silenceRealTimes.get(i)>= dotprom)synchronized(morseSignal) { morseSignal.append(' ') }
                                                        }
                                                }
                                                silenceStartTime= now //Lo hago porque termina en un simbolo, asi que empieza a contar el silencio
                                                //fin de ultimo codigo de learning
                                        }}
                                    } }}}
                        else{ //if (!isLearning)
                            val now:Long = SystemClock.elapsedRealtime()
                            if (power > UMBRAL) {
                                Log.d("MorseAudioInput", "OOOOOO $changeToSound $changeToSilence $pulseStartTime $silenceStartTime")
                                if(changeToSound) {
                                    changeToSound=false
                                    changeToSilence=true

                                    pulseStartTime = now
                                    silenceRealTime = now - silenceStartTime

                                    if(silenceRealTime>=7*dotprom) synchronized(morseSignal) { morseSignal.append('\t') }
                                    else if(silenceRealTime>= dotprom)synchronized(morseSignal) { morseSignal.append(' ') }
                                }
                            } else {
                                Log.d("MorseAudioInput", "----- $changeToSound $changeToSilence $pulseRealTime $silenceRealTime")
                                if(changeToSilence){
                                    changeToSound=true
                                    changeToSilence=false

                                    silenceStartTime= now
                                    pulseRealTime = now - pulseStartTime

                                    if(pulseRealTime<=umbralMorse) synchronized(morseSignal) { morseSignal.append('•') }
                                    else synchronized(morseSignal) { morseSignal.append('-') }

                            }}}


                        // ===============================
                        //     FIN LOGICA MORSE
                        // ===============================

                    }
                    if (morseSignal.length > 5000) morseSignal.setLength(0)

            }  catch (e: Throwable) {
            Log.e("MorseAudioInput", "CRASH INTERNO EN EXECUTOR", e)
        }}}

        mic?.start()
        }


    fun stop() {
        mic?.stop()
        mic = null
        goertzel = null
        finder.softReset()
    }
}



// ===============================
// CLASES DE PROCESAMIENTO E INPUT
// ===============================


class MicrophoneInput(
    private val context: Context,
    private val sampleRate: Int,
    private val bufferSize: Int,
    private val onBuffer: (FloatArray) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var audioThread: Thread? = null
    @Volatile private var running = false

    fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return

        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val internalBufferSize = maxOf(minBuffer, bufferSize * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            internalBufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) return

        running = true
        audioRecord!!.startRecording()

        audioThread = Thread {
            val buffer = ShortArray(bufferSize)
            while (running && audioRecord != null) {
                val read = audioRecord!!.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val floatBuffer = FloatArray(read) { buffer[it] / 32768f }
                    onBuffer(floatBuffer)
                }
            }
        }.apply { start() }
    }

    fun stop() {
        running = false

        try {
            audioThread?.join()
        } catch (_: InterruptedException) {}

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}

        audioRecord = null
        audioThread = null
    }
}


// --- Goertzel y FrequencyFinder ajustados para buffers variables ---

class Goertzel(
    private val sampleRate: Float,
    private val bufferSize: Int,
    val targetFrequency: Float
) {
    private val omega = (2.0f * Math.PI.toFloat() * targetFrequency) / sampleRate
    private val cosine = kotlin.math.cos(omega)
    private val sine = kotlin.math.sin(omega)
    private val coeff = 2.0f * cosine

    private val accumulationFrames: Int = 5
    private var framesProcessed = 0

    private val window = FloatArray(bufferSize) { i ->
        (0.5f * (1f - kotlin.math.cos(2f * Math.PI.toFloat() * i / (bufferSize - 1))))
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
