package com.santangeloezequiel.myfriendlymorse.audioinput
//Using dsp (digital signal procesor) and android microphone (configuration) from the package


import android.content.Context
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import android.os.SystemClock


private const val BUFFER_SIZE = 256   // cuantos bytes tendra cada callback

private const val SAMPLE_RATE = 32000 //en Hz


private const val UMBRAL  = 0.00038 // Ajustado ligeramente > 0 para evitar ruido puro

private const val MIN_TIME = 45L // ms

private const val CONST_MULTIPLICATION_SOUND_FIRST = 1.5 //proporcion umbral entre una raya y un punto a priori

private const val MULTIPLICADOR_UMBRAL_MORSE_FINAL = 1 //proporcion umbral morse final
private const val UMBRAL_WORD_SILENCE = 2 //cuanto multiplica el umbralSilence, para que sea un umbral word

private const val FIRST_UMBRAL_SILENCE_MULTIPLIYER= 1.5 //cuando no tiene suficientes datos, que porcentaje del punto sera una raya



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

                        if (isLearning) {


                            if (power > UMBRAL) {

                                if (changeToSound && now - lastFrameTime > MIN_TIME) {
                                    changeToSound = false
                                    changeToSilence = true
                                    lastFrameTime=now

                                    pulseStartTimes.add(now)

                                    if (counterLearned > -1)
                                        silenceRealTimes.add(now - silenceStartTimes.last())

                                    counterLearned++
                                    if (counterLearned > 0) Log.d("MorseAudioInput", "silenceRealTime = ${silenceRealTimes.last()}  counterLearned= $counterLearned")
                                }

                            } else if (power < UMBRAL) {

                                if (changeToSilence &&now - lastFrameTime > MIN_TIME) {
                                    changeToSound = true
                                    changeToSilence = false
                                    lastFrameTime= now

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


                                            if (CONST_MULTIPLICATION_SOUND_FIRST * minOf(prev, curr) < maxOf(prev, curr)) {
                                                if(happendTwice) {
                                                    isLearning = false


                                                    //CALCULO SONIDOS
                                                    val dotPulses = mutableListOf<Long>()
                                                    val dashPulses = mutableListOf<Long>()

                                                    umbralMorse = (prev + curr)/2

                                                    for (i in 0 until pulseRealTimes.size){
                                                        if (pulseRealTimes[i]<=umbralMorse){
                                                            dotPulses.add(pulseRealTimes[i])
                                                        }
                                                        else
                                                            dashPulses.add(pulseRealTimes[i])
                                                    }

                                                    if (dotPulses.isEmpty()) {
                                                        Log.w(
                                                            "MorseAudioInput",
                                                            "dotPulses vacío, no se puede calcular umbrales"
                                                        )
                                                        return@execute
                                                    }
                                                    if (dashPulses.isEmpty()) {
                                                        Log.w(
                                                            "MorseAudioInput",
                                                            "dashPulses vacío, no se puede calcular umbral Morse"
                                                        )
                                                        return@execute
                                                    }

                                                    val maxDot = dotPulses.max()
                                                    val minDash = dashPulses.min()

                                                    umbralMorse = (((maxDot + minDash) / 2) * MULTIPLICADOR_UMBRAL_MORSE_FINAL)

                                                    //CALCULO SILENCIOS

                                                    umbralSilence= umbralMorse

                                                    val simbolSilences = mutableListOf<Long>()
                                                    val letterSilences =  mutableListOf<Long>()
                                                    val wordSilences =  mutableListOf<Long>()

                                                    for (i in 0 until silenceRealTimes.size){
                                                        if (silenceRealTimes[i]<=umbralSilence){
                                                            simbolSilences.add(silenceRealTimes[i])
                                                        }
                                                        else if(silenceRealTimes[i]<=umbralSilence * 1.7)
                                                            letterSilences.add(silenceRealTimes[i])
                                                        else
                                                            wordSilences.add(silenceRealTimes[i])
                                                    }

                                                    umbralSilence = when {
                                                        simbolSilences.isNotEmpty() && letterSilences.isNotEmpty() ->
                                                            (simbolSilences.max() + letterSilences.min()) / 2

                                                        simbolSilences.isNotEmpty() ->
                                                            (simbolSilences.max() * FIRST_UMBRAL_SILENCE_MULTIPLIYER).toLong()

                                                        letterSilences.isNotEmpty() ->
                                                            (letterSilences.min() / FIRST_UMBRAL_SILENCE_MULTIPLIYER).toLong()

                                                        wordSilences.isNotEmpty() ->
                                                            wordSilences.min() / (FIRST_UMBRAL_SILENCE_MULTIPLIYER*2).toLong()

                                                        else -> umbralSilence
                                                    }

                                                    Log.d(
                                                        "MorseAudioInput",
                                                        "umbral Morse = $umbralMorse  umbralSilence = $umbralSilence"
                                                    )


                                                    for (i in 0 until counterLearned+1) {

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

                                if (changeToSound && now - lastFrameTime > MIN_TIME) {
                                    changeToSound = false
                                    changeToSilence = true
                                    lastFrameTime=now

                                    pulseStartTime = now
                                    silenceRealTime = now - silenceStartTime

                                    decideMorseSilence(
                                        silenceRealTime,
                                        umbralSilence,
                                        morseSignal
                                    )
                                }

                            } else if (power < UMBRAL) {

                                if (changeToSilence && now - lastFrameTime > MIN_TIME) {
                                    changeToSound = true
                                    changeToSilence = false
                                    lastFrameTime=now

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
        running.set(false)
        executor.shutdownNow()
        executor = Executors.newSingleThreadExecutor()
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
        if(silenceRealTime >= umbralSilence * UMBRAL_WORD_SILENCE){
            Log.d("MorseAudioInput", "--------PALABRA--------")
            appendMorseSignal(morseSignal, '\t')
        }

        else if(silenceRealTime >= umbralSilence) {
            appendMorseSignal(morseSignal, ' ')
            Log.d("MorseAudioInput", "------------")
        }
    }

}

