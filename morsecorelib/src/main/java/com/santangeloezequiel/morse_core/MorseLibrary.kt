package com.santangelo.morse

enum class MorseSignal {
    DOT,
    DASH,
    LETTER_GAP,
    WORD_GAP
}

object MorseLibrary {

    val CHAR_TO_MORSE: Map<Char, List<MorseSignal>> = mapOf(

        // letters
        'a' to listOf(MorseSignal.DOT, MorseSignal.DASH),
        'b' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT),
        'c' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT),
        'd' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT),
        'e' to listOf(MorseSignal.DOT),
        'f' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT),
        'g' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT),
        'h' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT),
        'i' to listOf(MorseSignal.DOT, MorseSignal.DOT),
        'j' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH),
        'k' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH),
        'l' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT),
        'm' to listOf(MorseSignal.DASH, MorseSignal.DASH),
        'n' to listOf(MorseSignal.DASH, MorseSignal.DOT),
        'o' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH),
        'p' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT),
        'q' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH),
        'r' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT),
        's' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT),
        't' to listOf(MorseSignal.DASH),
        'u' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH),
        'v' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH),
        'w' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH),
        'x' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH),
        'y' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH),
        'z' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT),

        //upper is the same

        'A' to listOf(MorseSignal.DOT, MorseSignal.DASH),
        'B' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT),
        'C' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT),
        'D' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT),
        'E' to listOf(MorseSignal.DOT),
        'F' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT),
        'G' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT),
        'H' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT),
        'I' to listOf(MorseSignal.DOT, MorseSignal.DOT),
        'J' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH),
        'K' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH),
        'L' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT),
        'M' to listOf(MorseSignal.DASH, MorseSignal.DASH),
        'N' to listOf(MorseSignal.DASH, MorseSignal.DOT),
        'O' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH),
        'P' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT),
        'Q' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH),
        'R' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT),
        'S' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT),
        'T' to listOf(MorseSignal.DASH),
        'U' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH),
        'V' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH),
        'W' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH),
        'X' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH),
        'Y' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH),
        'Z' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT),


        // numbers
        '0' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH),
        '1' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH),
        '2' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH),
        '3' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH),
        '4' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH),
        '5' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT),
        '6' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT),
        '7' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT),
        '8' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT),
        '9' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT),

        // punctuation
        '.' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH),
        ',' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH),
        ':' to listOf(MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT),
        '?' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT),
        '!' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH),
        '"' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT),
        '\'' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT),
        '=' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH),
        '+' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT),
        '-' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH),
        '/' to listOf(MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT),
        '&' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DOT),
        '@' to listOf(MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DOT),
        '¿' to listOf(MorseSignal.DOT, MorseSignal.DOT, MorseSignal.DASH, MorseSignal.DASH, MorseSignal.DOT) // no estándar
    )

    val MORSE_TO_CHAR: Map<List<MorseSignal>, Char> = CHAR_TO_MORSE.entries.associate { it.value to it.key }

    val TEXTMORSE_TO_MORSE : Map<Char , MorseSignal> = mapOf(
        '•' to MorseSignal.DOT,
        '-' to MorseSignal.DASH,
        ' ' to MorseSignal.LETTER_GAP,
        '\t' to MorseSignal.WORD_GAP,
        )//this is the morse readed as text

    val MORSE_TO_TEXTMORSE: Map<MorseSignal, Char> = TEXTMORSE_TO_MORSE.entries.associate { it.value to it.key }

}