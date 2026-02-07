package com.santangeloezequiel.morse_core

@Suppress("unused")
object MorseDecoder {

    fun morseToText(signals: List<MorseSignal>): String{
        val result = StringBuilder()
        val buffer = mutableListOf<MorseSignal>() //wait until a letter gap, then reset

        for (element in signals){
            if (element == MorseSignal.WORD_GAP) {
                if(buffer.isNotEmpty()) {
                    val letter = MorseLibrary.MORSE_TO_CHAR[buffer]
                    if (letter != null) { //verifico que el caracter sea valido
                        result.append(letter)
                        buffer.clear()
                    }

                }
                result.append(' ') //every word gap is a space
                continue //if the element is a space, it is not a letter, so go to the next element
            }

            if (element == MorseSignal.DOT || element == MorseSignal.DASH) {
                buffer.add(element)
            }

            if (element == MorseSignal.LETTER_GAP  && buffer.isNotEmpty()) {
                val letter = MorseLibrary.MORSE_TO_CHAR[buffer]
                if (letter != null) { //verifico que el caracter sea valido
                    result.append(letter)
                    buffer.clear()
                }
            }
        }

        val letter = MorseLibrary.MORSE_TO_CHAR[buffer]
        if (letter != null) { //verifico que el caracter sea valido
            result.append(letter)
            buffer.clear()
        }

        return result.toString()
    }

    fun morseToMorseText(signals: List<MorseSignal>): String{
        return TextInputMorseEncoder.textToMorseText(morseToText(signals))
    }
}