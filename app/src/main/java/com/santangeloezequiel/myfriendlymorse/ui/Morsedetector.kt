package com.santangeloezequiel.myfriendlymorse.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.santangeloezequiel.morse_core.MorseDecoder
import com.santangeloezequiel.morse_core.TextInputMorseEncoder
import com.santangeloezequiel.myfriendlymorse.R
import com.santangeloezequiel.myfriendlymorse.audioinput.MorseAudioInput
import com.santangeloezequiel.myfriendlymorse.databinding.FragmentMorsedetectorBinding
import kotlin.text.iterator

private const val REQUEST_MIC_PERMISSION = 1001
class Morsedetector : Fragment() {

    private var _binding: FragmentMorsedetectorBinding? = null
    private val binding get() = _binding!!

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
        val updateHandler = Handler(Looper.getMainLooper())
        val updateRunnable = object : Runnable {
            override fun run() {
                val newText: String
                synchronized(morseSignal) {
                    newText = if (lastIndex < morseSignal.length) {
                        val snapshot = morseSignal.substring(lastIndex)
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
                                val decoded = MorseDecoder.morseToText(TextInputMorseEncoder.morseTextToMorse(letterBuffer.toString()))
                                binding.tvOutput1.append(decoded)
                                letterBuffer.setLength(0)
                            }
                            '\t' -> { // fin de palabra
                                val decoded = MorseDecoder.morseToText(TextInputMorseEncoder.morseTextToMorse(letterBuffer.toString()))
                                binding.tvOutput1.append(decoded)
                               // binding.tvOutput1.append(" ") // espacio entre palabras
                                letterBuffer.setLength(0)
                            }
                        }
                    }
                    lastIndex = morseSignal.length
                }

                // Repetir mientras el botón siga presionado
                updateHandler.postDelayed(this, 50)
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        binding.btnmicrophone.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {

                MotionEvent.ACTION_DOWN -> {
                    binding.btnmicrophone.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))

                    // Iniciar detector Morse
                    MorseAudioInput.start(requireContext(), morseSignal)
                    lastIndex = morseSignal.length

                    // Arrancar Runnable que actualiza UI cada 50ms
                    updateHandler.post(updateRunnable)

                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.btnmicrophone.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.beige))

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
                arrayOf(Manifest.permission.RECORD_AUDIO)
                REQUEST_MIC_PERMISSION
        }
    }

    private fun onMicrophoneReady() {
        // ACÁ después vas a iniciar el detector Morse
       // binding.textView.text = "Micrófono listo 🎤"
    }

    @Deprecated("Usar ActivityResultLauncher para permisos")
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