package com.santangelo.morse

import org.jetbrains.annotations.Nullable
import kotlin.collections.mutableListOf
import kotlin.text.iterator


/*
abstract class MorseInput {

    abstract fun readSignal(): List<MorseSignal>
}*/

object TextInputMorseEncoder {

    fun morseTextToMorse(text : String): List<MorseSignal> {
        //take a string tiped as MORSE, go element by element and covert it an morse object.
        val list = ArrayList<MorseSignal>()
        for (i in text) {
        list.add(MorseLibrary.TEXTMORSE_TO_MORSE.getValue(i))
        }
        return list
    }

    fun textToMorseText(text: String): String {
        val result = StringBuilder()
        for (letter in text) {
            if (letter == ' ' || letter == '\t' || letter == '\n') {
                result.append('\t')
                continue
            }
            val morselist = MorseLibrary.CHAR_TO_MORSE[letter] ?: continue// obtiene la lista de señales
            for (element in morselist) {
                result.append(MorseLibrary.MORSE_TO_TEXTMORSE.getValue(element)) // convierte a texto morse
            }
            result.append(' ') // separa letras con espacio en el texto morse
        }
        return result.toString()
    }

    fun textToMorse (text: String): List<MorseSignal> {
        return TextInputMorseEncoder.morseTextToMorse(TextInputMorseEncoder.textToMorseText(text))
    }
}
//class AudioMorseInput : MorseInput
//class LightMorseInput : MorseInput