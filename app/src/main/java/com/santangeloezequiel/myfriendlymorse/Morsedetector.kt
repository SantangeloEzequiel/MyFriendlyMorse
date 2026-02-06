package com.santangeloezequiel.myfriendlymorse

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.santangeloezequiel.myfriendlymorse.databinding.FragmentMorsedetectorBinding
import android.view.MotionEvent
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color

//Librerias de MORSE
import com.santangelo.morse.MorseDecoder
import com.santangelo.morse.TextInputMorseEncoder

class Morsedetector : Fragment() {

    private var _binding: FragmentMorsedetectorBinding? = null
    private val binding get() = _binding!!

    private val REQUEST_MIC_PERMISSION = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMorsedetectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var lastIndex = 0
        val morseSignal = StringBuffer()
        val letterBuffer = StringBuffer() // para acumular puntos/guiones de cada letra

        checkMicrophonePermission()

        // Handler y Runnable para actualizar UI periódicamente
        val updateHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val updateRunnable = object : Runnable {
            override fun run() {
                val newText: String
                synchronized(morseSignal) {
                    newText = if (lastIndex < morseSignal.length) {
                        val snapshot = morseSignal.substring(lastIndex)
                        lastIndex = morseSignal.length
                        snapshot
                    } else ""
                }

                if (newText.isNotEmpty()) {
                    // Mostrar la señal cruda
                    binding.etoutput1.append(newText)

                    // Procesar cada símbolo de la señal
                    for (c in newText) {
                        when (c) {
                            '•', '-' -> letterBuffer.append(c) // acumula puntos/guiones
                            ' ' -> { // fin de letra
                                val decoded = try {
                                    MorseDecoder.morseToText(TextInputMorseEncoder.morseTextToMorse(letterBuffer.toString()))
                                } catch (e: Exception) {
                                    "?" // si no se reconoce, poner un ?
                                }
                                binding.tvOutput1.append(decoded)
                                letterBuffer.setLength(0)
                            }
                            '\t' -> { // fin de palabra
                                val decoded = try {
                                    MorseDecoder.morseToText(TextInputMorseEncoder.morseTextToMorse(letterBuffer.toString()))
                                } catch (e: Exception) {
                                    "?"
                                }
                                binding.tvOutput1.append(decoded)
                               // binding.tvOutput1.append(" ") // espacio entre palabras
                                letterBuffer.setLength(0)
                            }
                        }
                    }
                }

                // Repetir mientras el botón siga presionado
                updateHandler.postDelayed(this, 50)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        binding.btnmicrophone.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {

                MotionEvent.ACTION_DOWN -> {
                    binding.btnmicrophone.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#FFFFFFFF"))

                    // Iniciar detector Morse
                    MorseAudioInput.start(requireContext(), morseSignal)
                    lastIndex = morseSignal.length

                    // Arrancar Runnable que actualiza UI cada 50ms
                    updateHandler.post(updateRunnable)

                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.btnmicrophone.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#33D7CCC8"))

                    // Detener detector Morse
                    MorseAudioInput.stop()

                    // Detener Runnable
                    updateHandler.removeCallbacks(updateRunnable)

                    true
                }

                else -> false
            }
        }
    }















    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onMicrophoneReady()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_MIC_PERMISSION
            )
        }
    }

    private fun onMicrophoneReady() {
        // ACÁ después vas a iniciar el detector Morse
       // binding.textView.text = "Micrófono listo 🎤"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_MIC_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            onMicrophoneReady()
        } else {
            //binding.textView.text = "Permiso de micrófono denegado"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
