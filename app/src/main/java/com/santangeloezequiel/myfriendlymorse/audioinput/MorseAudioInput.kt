package com.santangeloezequiel.myfriendlymorse.audioinput
//Using dsp (digital signal procesor) and android microphone (configuration) from the package


import android.content.Context
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import android.os.SystemClock


private const val BUFFER_SIZE = 512   // cuantos bytes tendra cada callback
private const val UMBRAL = 0.00035      // Ajustado ligeramente > 0 para evitar ruido puro
private const val SAMPLE_RATE = 32000 //en Hz

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
                                    if(counterLearned>-1)silenceRealTimes.add(now - silenceStartTimes[counterLearned])
                                    counterLearned++
                                }
                            }
                            else {
                                Log.d("MorseAudioInput", "-----")
                                if(changeToSilence){
                                    changeToSound=true
                                    changeToSilence=false
                                    if(counterLearned>-1){
                                        silenceStartTimes.add((now))
                                        pulseRealTimes.add(now - pulseStartTimes[counterLearned])
                                        if(counterLearned>0){
                                            if(2 * minOf(pulseRealTimes[counterLearned-1],pulseRealTimes[counterLearned])< maxOf(pulseRealTimes[counterLearned-1],pulseRealTimes[counterLearned])){
                                                isLearning=false
                                                //Ultimo codigo de learning
                                                val prom = (pulseRealTimes.sum() - pulseRealTimes[counterLearned]) / (counterLearned-1)
                                                umbralMorse = (prom + pulseRealTimes[counterLearned])/2

                                                dotprom =  if (pulseRealTimes[counterLearned]>pulseRealTimes[counterLearned-1])
                                                   pulseRealTimes[counterLearned]
                                                else
                                                    prom

                                                for (i in 0 until counterLearned){
                                                    if(pulseRealTimes[i]<=umbralMorse)
                                                        synchronized(morseSignal) { morseSignal.append('•') }
                                                    else
                                                        synchronized(morseSignal) { morseSignal.append('-') }
                                                    if(i<counterLearned){
                                                        if(silenceRealTimes[i]>=7*dotprom)
                                                            synchronized(morseSignal) { morseSignal.append('\t') }
                                                        else if(silenceRealTimes[i]>= dotprom)
                                                            synchronized(morseSignal) { morseSignal.append(' ') }
                                                        }
                                                }
                                                silenceStartTime= now //Lo hago porque termina en un simbolo, asi que empieza a contar el silencio
                                                //fin de ultimo codigo de learning
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
                            }
                            else {
                                Log.d("MorseAudioInput", "----- $changeToSound $changeToSilence $pulseRealTime $silenceRealTime")
                                if(changeToSilence){
                                    changeToSound=true
                                    changeToSilence=false

                                    silenceStartTime= now
                                    pulseRealTime = now - pulseStartTime

                                    if(pulseRealTime<=umbralMorse) synchronized(morseSignal) { morseSignal.append('•') }
                                    else synchronized(morseSignal) { morseSignal.append('-') }

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
        dotprom = 0

        changeToSound = true
        changeToSilence = false

        pulseRealTime = 0
        pulseStartTime = 0
        silenceRealTime = 0
        silenceStartTime = 0
    }
}

