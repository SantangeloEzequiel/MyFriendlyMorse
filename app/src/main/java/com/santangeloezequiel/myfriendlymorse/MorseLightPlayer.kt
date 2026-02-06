package com.santangeloezequiel.myfriendlymorse


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.Job


object MorseLightPlayer {

    private val DOT_DURATION: Long=100 //en mS
    private var currentStreamId: Int? = null //Seguridad para que no se superpongan

    //CAMARA
    private var cameraId : String? = null
    private var morseJob: Job? = null //para cancelar el courtime de reproduccion
    private lateinit var cameraManager: CameraManager
    //Los metodos que voy a usar, short y long beep
    //USO como DOT 100 mS, DASH 300 mS, SIMBOL SPACE, 100 mS, letter GAP = 300 mS, word GAP = 700 mS

    //init
    fun init(context: Context) {
        cameraManager= context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (id in cameraManager.cameraIdList) {
                val hasFlash = cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)
                val facing = cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                if (hasFlash == true && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    break
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    //Inicializo las funciones genericas
    fun turnOnFlash() {
        cameraId?.let { id ->
            try {
                cameraManager.setTorchMode(id, true) // enciende
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    fun turnOffFlash() {
        cameraId?.let { id ->
            try {
                cameraManager.setTorchMode(id, false) // apaga
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }
    //END OF CAMARA


    //Funcion general para reproducir una luz con medidas de proteccion para que no se superpongan
    private suspend fun playLigth(duration: Long) {
        turnOnFlash()
        delay(duration)
        turnOffFlash()
    }

    //Tonos especificos
    suspend fun dotBip() = playLigth(DOT_DURATION)
    suspend fun dashBip() = playLigth(DOT_DURATION * 3)


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
        turnOffFlash()
        morseJob?.cancel()
        morseJob=null
    }


}
