package com.santangeloezequiel.myfriendlymorse.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.santangelo.morse.MorseDecoder
import com.santangelo.morse.TextInputMorseEncoder
import com.santangeloezequiel.myfriendlymorse.databinding.FragmentManualtranslatorBinding
import com.santangeloezequiel.myfriendlymorse.morseplayer.MorseLightPlayer
import com.santangeloezequiel.myfriendlymorse.morseplayer.MorseSoundPlayer
import kotlinx.coroutines.launch

class ManualTranslator : Fragment() {

    private var _binding: FragmentManualtranslatorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualtranslatorBinding.inflate(inflater, container, false)

        //inicializo la camara y el sonido
        MorseSoundPlayer.init(requireContext())
        MorseLightPlayer.init(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        var isActive = false //this is for when the user press the change boton
        var soundActive = false
        var lightActive = false
        var playActive = false

        /* ===============================
                OPCIONES DE CHANGE
           =============================== */

        binding.btnChange.setOnClickListener {
            isActive = !isActive
            if(!isActive) { //si no se apreto funciona normal

                //here I show the enter text and hide de morse options

                binding.buttonDASH.visibility = View.GONE
                binding.buttonDOT.visibility = View.GONE
                binding.buttonSPACE.visibility = View.GONE
                binding.etoutput.visibility = View.GONE
                binding.etInput.visibility = View.VISIBLE
                binding.btnClear.visibility = View.VISIBLE
                binding.btnDelete.visibility = View.GONE
                binding.etInput.hint=""

                //Lo siguiente intercambia lo que estaba arriba por lo que estaba abajo
                binding.etoutput.text=""
                binding.tvOutput.text=""
            }
            if(isActive){ //If you press it change to MorseToText

                //Here I hide the enterText and show the morse options
                binding.buttonDASH.visibility= View.VISIBLE
                binding.buttonDOT.visibility = View.VISIBLE
                binding.buttonSPACE.visibility = View.VISIBLE
                binding.etoutput.visibility = View.VISIBLE
                binding.etInput.visibility = View.GONE
                binding.btnClear.visibility = View.GONE
                binding.btnDelete.visibility = View.VISIBLE
                binding.etInput.hint=""


                //Lo siguiente intercambia lo que estaba arriba por lo que estaba abajo
                binding.etInput.setText("")
                binding.tvOutput.text=""
            }}

        /* ===============================
                TEXTO A MORSE
           =============================== */
        var previousText = ""

        binding.etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Antes de que cambie el texto no hago nada
                }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Se ejecuta mientras el usuario escribe

                    val newText = s.toString()  // esto es lo que escribió hasta ahora

                    val addedText = if (newText.length > previousText.length)
                        newText.substring(previousText.length)
                    else
                        "" // si borraron ignora

                    previousText = newText

                    val morseText = TextInputMorseEncoder.textToMorseText(newText)
                    binding.tvOutput.text = morseText
                }

            override fun afterTextChanged(s: Editable?) {
                // despues de que cambie el texto no hago nada, lo hago durante
            }
        })

        //CLEAR

        binding.btnClear.setOnClickListener{ //borra la pantalla / clear
            binding.etInput.setText("")
            binding.etoutput.text = ""
            binding.etInput.clearFocus()
            binding.tvOutput.text = ""
        }


        /* ===============================
                MORSE A TEXTO
           =============================== */


        // TEXT LISTENER PARA CUANDO ESCRIBE MORSE
        binding.etoutput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No necesitamos esto
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // No necesitamos esto
            }

            override fun afterTextChanged(s: Editable?) {
                val newText = s.toString()  // esto es lo que escribió hasta ahora

                val addedText = if (newText.length > previousText.length)
                    newText.substring(previousText.length)
                else
                    "" // si borraron ignora

                previousText = newText

                val morseText = MorseDecoder.morseToText(TextInputMorseEncoder.morseTextToMorse(newText))
                binding.tvOutput.text = morseText
            }
        })

        var lastimputSpace = false //Cada dos espacios de letra seguidos se hace uno de palabra

        //Listener de los botones de morse
        binding.buttonDOT.setOnClickListener {
            binding.etoutput.append("•")
            lastimputSpace=false

            if(soundActive) lifecycleScope.launch {
                MorseSoundPlayer.dotBip() }
            if(lightActive)lifecycleScope.launch{
                MorseLightPlayer.dotBip() } }
        binding.buttonDASH.setOnClickListener {
            binding.etoutput.append("-")
            lastimputSpace=false
            if(soundActive)lifecycleScope.launch{
                MorseSoundPlayer.dashBip() }
            if(lightActive)lifecycleScope.launch{
                MorseLightPlayer.dotBip() } }
        binding.buttonSPACE.setOnClickListener {  //Cada dos espacios de letra, lo tomo como un espacio de palabra
            if (lastimputSpace){
                lastimputSpace=false
                val text = binding.etoutput.text as Editable
                if (text.isNotEmpty() && text[text.length - 1] == ' ') {
                    text.delete(text.length - 1, text.length) // Borra el último espacio
                    text.append("\t") // Agrega tab
                }
            } else {
                lastimputSpace=true
                binding.etoutput.append(" ") }}
        binding.btnDelete.setOnClickListener {
                val text = binding.etoutput.text as Editable
                if (text.isNotEmpty()) {
                    text.delete(text.length - 1, text.length) // Borra el último espacio
                }
        }



        /* ===============================
                SOUND, LIGTH, PLAY
           =============================== */

        binding.btnsound.setOnClickListener {
            soundActive=!soundActive
            if(soundActive){
                MorseSoundPlayer.init(requireContext())
                binding.btnsound.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFFFF"))
            }
            else
            {binding.btnsound.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#33D7CCC8"))
                MorseSoundPlayer.release() } }
        binding.btnligth.setOnClickListener {
            lightActive=!lightActive

            if(lightActive){
                binding.btnligth.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFFFF"))
            }
            else
            {binding.btnligth.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#33D7CCC8"))} }
        binding.btnplay.setOnClickListener {
            playActive = !playActive
            if (playActive) {
                binding.btnplay.text = ("■")
                if (soundActive) MorseSoundPlayer.playMorse(binding.tvOutput.text.toString())
                if (lightActive) MorseLightPlayer.playMorse(binding.tvOutput.text.toString())
            } else {
                binding.btnplay.text = ("▶")
                MorseLightPlayer.stopMorse()
                MorseSoundPlayer.stopMorse()
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}