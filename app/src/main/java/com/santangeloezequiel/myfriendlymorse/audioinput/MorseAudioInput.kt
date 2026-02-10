package com.santangeloezequiel.myfriendlymorse.audioinput
//Using dsp (digital signal procesor) and android microphone (configuration) from the package


import android.content.Context
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import android.os.SystemClock


private const val BUFFER_SIZE = 512   // cuantos bytes tendra cada callback

private const val SAMPLE_RATE = 32000 //en Hz


private const val UMBRAL  = 0.00038 // Ajustado ligeramente > 0 para evitar ruido puro

private const val MIN_TIME = 30L // ms



//private const val MIN_FRAME_SILENCE= 0


object MorseAudioInput {

    @Suppress("StaticFieldLeak")
    private var mic: MicrophoneInput? = null
    private var finder: FrequencyFinder = FrequencyFinder(SAMPLE_RATE.toFloat(), BUFFER_SIZE)
    private var goertzel: Goertzel? = null

    //Propiedades de Executor
    private var executor = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)


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
    private var umbralSilence : Long = 0
    private var dotprom : Long = 0
    private var changeToSound = true
    private var changeToSilence = false
    private var pulseRealTime:Long = 0
    private var pulseStartTime:Long = 0
    private var silenceRealTime:Long = 0
    private var silenceStartTime:Long = 0

    private var lastFrameTime : Long = 0L

    private var happendTwice : Boolean = false






    fun start(context: Context, morseSignal: StringBuffer) {

        stop()
        running.set(true)

        mic = MicrophoneInput(context.applicationContext, SAMPLE_RATE, BUFFER_SIZE) { buffer ->
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
                        //      INICIO LOGICA MORSE
                        // ===============================


                        val now = SystemClock.elapsedRealtime()

                        if (now - lastFrameTime < MIN_TIME) return@execute
                        lastFrameTime = now
                        //in this if now it would be previous now

                        if (isLearning) {


                            if (power > UMBRAL) {

                                if (changeToSound) {
                                    changeToSound = false
                                    changeToSilence = true

                                    pulseStartTimes.add(now)

                                    if (counterLearned > -1)
                                        silenceRealTimes.add(now - silenceStartTimes.last())

                                    counterLearned++
                                    if (counterLearned > 0) Log.d("MorseAudioInput", "silenceRealTime = ${silenceRealTimes.last()}  counterLearned= $counterLearned")
                                }

                            } else if (power < UMBRAL) {

                                if (changeToSilence) {
                                    changeToSound = true
                                    changeToSilence = false

                                    if (counterLearned > -1) {

                                        silenceStartTimes.add(now)
                                        pulseRealTimes.add(now - pulseStartTimes.last())

                                        Log.d(
                                            "MorseAudioInput",
                                            "pulseRealTime = ${pulseRealTimes.last()}  counterLearned= $counterLearned"
                                        )
                                        if (counterLearned > 0) {

                                            val prev = pulseRealTimes[pulseRealTimes.size - 2]
                                            val curr = pulseRealTimes.last()


                                            if (1.5 * minOf(prev, curr) < maxOf(prev, curr)) {
                                                if(happendTwice) {
                                                    isLearning = false

                                                    val dotPulses = if (prev < curr) {
                                                        pulseRealTimes.filter { it * 1.5< curr }
                                                    } else {
                                                        pulseRealTimes.filter { it >= curr * 1.5 }
                                                    }

                                                    if (dotPulses.isEmpty()) {
                                                        Log.w(
                                                            "MorseAudioInput",
                                                            "dotPulses vacío, no se puede calcular umbrales"
                                                        )
                                                        return@execute
                                                    }

                                                    val maxDot = dotPulses.max()

                                                    val dashPulses =
                                                        pulseRealTimes.filter { it > maxDot }
                                                    if (dashPulses.isEmpty()) {
                                                        Log.w(
                                                            "MorseAudioInput",
                                                            "dashPulses vacío, no se puede calcular umbral Morse"
                                                        )
                                                        return@execute
                                                    }

                                                    val minDash = dashPulses.min()

                                                    umbralMorse =
                                                        ((maxDot + minDash) / 2.4).toLong()
                                                    umbralSilence = (maxDot * 1.05).toLong()

                                                    Log.d(
                                                        "MorseAudioInput",
                                                        "umbral Morse = $umbralMorse dotprom = $dotprom umbralSilence = $umbralSilence"
                                                    )

                                                    val maxIndex = minOf(
                                                        counterLearned,
                                                        pulseRealTimes.size,
                                                        silenceRealTimes.size
                                                    )

                                                    for (i in 0 until maxIndex + 1) {

                                                        if (i > 0) decideMorseSilence(
                                                            silenceRealTimes[i - 1],
                                                            umbralSilence,
                                                            morseSignal
                                                        )

                                                        decideMorseSound(
                                                            pulseRealTimes[i],
                                                            umbralMorse,
                                                            morseSignal
                                                        )

                                                    }
                                                }
                                                silenceStartTime=now
                                            }
                                            happendTwice=true
                                        }
                                    }
                                }
                            }
                        } else {
                            // !isLearning
                            if (power > UMBRAL) {

                                if (changeToSound) {
                                    changeToSound = false
                                    changeToSilence = true

                                    pulseStartTime = now
                                    silenceRealTime = now - silenceStartTime

                                    decideMorseSilence(
                                        silenceRealTime,
                                        umbralSilence,
                                        morseSignal
                                    )
                                }

                            } else if (power < UMBRAL) {

                                if (changeToSilence) {
                                    changeToSound = true
                                    changeToSilence = false

                                    silenceStartTime = now
                                    pulseRealTime = now - pulseStartTime

                                    decideMorseSound(
                                        pulseRealTime,
                                        umbralMorse,
                                        morseSignal
                                    )
                                }
                            }
                        }
                        // ===============================
                        //     FIN LOGICA MORSE
                        // ===============================

                    }
                    if (morseSignal.length > 5000) morseSignal.setLength(0)

                }
                catch (e: Throwable) {
            Log.e("MorseAudioInput", "CRASH INTERNO EN EXECUTOR", e)
                 }
            }
        }
        mic?.start()
    }


    fun stop() {
        mic?.stop()
        mic = null
        goertzel = null
        finder.softReset()
        resetState()
    }


    fun resetState() {
        isLearning = true
        counterLearned = -1

        pulseStartTimes.clear()
        pulseRealTimes.clear()
        silenceStartTimes.clear()
        silenceRealTimes.clear()

        umbralMorse = 0

        changeToSound = true
        changeToSilence = false
        happendTwice = false

        pulseRealTime = 0
        pulseStartTime = 0
        silenceRealTime = 0
        silenceStartTime = 0
        lastFrameTime = 0L
    }

    private fun appendMorseSignal(morseSignal: StringBuffer, ch: Char) {
        synchronized(morseSignal) { morseSignal.append(ch) }
    }

    private fun decideMorseSound(pulseRealTime : Long, umbralMorse : Long, morseSignal : StringBuffer){
        Log.d("MorseAudioInput", "sonido $pulseRealTime   umbral = $umbralMorse")
        if(pulseRealTime <= umbralMorse)
            appendMorseSignal(morseSignal,'•')
        else
            appendMorseSignal(morseSignal,'-')
    }

    private fun decideMorseSilence(silenceRealTime : Long, umbralSilence : Long, morseSignal : StringBuffer){
        Log.d("MorseAudioInput", "silencio $silenceRealTime   umbral = $umbralSilence")
        if(silenceRealTime >= umbralSilence * 2)
            appendMorseSignal(morseSignal, '\t')
        else if(silenceRealTime >= umbralSilence) {
            appendMorseSignal(morseSignal, ' ')
            Log.d("MorseAudioInput", "------------")
        }
    }

}

