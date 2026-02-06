package com.santangeloezequiel.myfriendlymorse
import android.media.SoundPool
import android.media.AudioAttributes
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job



object MorseSoundPlayer {

    private val DOT_DURATION: Long=100 //en mS
    private lateinit var soundPool: SoundPool
    private var beepId: Int = 0
    private var morseJob: Job? = null //para cancelar el courtime de reproduccion
    private var currentStreamId: Int? = null

    //Inicializo la libreria de sound pool en mi clase MorseSoundPlayer
    fun init(context : Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        beepId = soundPool.load(context,R.raw.beep700hz, 1)
    }

    //Los metodos que voy a usar, short y long beep
    //USO como DOT 100 mS, DASH 300 mS, SIMBOL SPACE, 100 mS, letter GAP = 300 mS, word GAP = 700 mS
    fun release() {
        soundPool.release()
    }//Libero memoria, solo cuando lo termino de usar

//Funcion general para reproducir un tono con medidas de proteccion para que no se superpongan
    private suspend fun playTone(duration: Long) {
        currentStreamId?.let { soundPool.stop(it) }

        val streamId = soundPool.play(beepId, 1f, 1f, 1, -1, 1f)
        currentStreamId = streamId

        delay(duration)

        soundPool.stop(streamId)
        if (currentStreamId == streamId) {
            currentStreamId = null
        }
    }

    //Tonos especificos
    suspend fun dotBip() = playTone(DOT_DURATION)
    suspend fun dashBip() = playTone(DOT_DURATION * 3)


    //Silencios
    suspend fun  simbolGap(){
        delay(timeMillis = DOT_DURATION)
    }

    suspend fun letterGap(){
        delay(DOT_DURATION*2) //3-1, because one will always be next to a simbol
    }

    suspend fun wordGap(){
        delay(DOT_DURATION*6) //7-1, because one will always be next to a simbol
    }


    //Toco Morse en funcion del texto en pantalla (codificado en MORSE)
    fun playMorse (morseText:String){
        morseJob?.cancel()
        morseJob=CoroutineScope(Dispatchers.Default).launch {
            // morse
            var previous : Char

            for (item in morseText) {
                when (item) {
                    '•' -> {
                        dotBip()
                        simbolGap()
                    }

                    '-' -> {
                        dashBip()
                        simbolGap()
                    }

                    ' ' ->letterGap()
                    '\t'->wordGap() } } }
    }

    fun stopMorse(){
        soundPool.autoPause()
        morseJob?.cancel()
        morseJob=null
    }

}


