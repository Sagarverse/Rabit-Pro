package com.example.rabit.domain.model

object HidKeyCodes {
    const val KEY_NONE: Byte = 0x00
    const val KEY_A: Byte = 0x04
    const val KEY_B: Byte = 0x05
    const val KEY_C: Byte = 0x06
    const val KEY_D: Byte = 0x07
    const val KEY_E: Byte = 0x08
    const val KEY_F: Byte = 0x09
    const val KEY_G: Byte = 0x0A
    const val KEY_H: Byte = 0x0B
    const val KEY_I: Byte = 0x0C
    const val KEY_J: Byte = 0x0D
    const val KEY_K: Byte = 0x0E
    const val KEY_L: Byte = 0x0F
    const val KEY_M: Byte = 0x10
    const val KEY_N: Byte = 0x11
    const val KEY_O: Byte = 0x12
    const val KEY_P: Byte = 0x13
    const val KEY_Q: Byte = 0x14
    const val KEY_R: Byte = 0x15
    const val KEY_S: Byte = 0x16
    const val KEY_T: Byte = 0x17
    const val KEY_U: Byte = 0x18
    const val KEY_V: Byte = 0x19
    const val KEY_W: Byte = 0x1A
    const val KEY_X: Byte = 0x1B
    const val KEY_Y: Byte = 0x1C
    const val KEY_Z: Byte = 0x1D
    
    const val KEY_1: Byte = 0x1E
    const val KEY_2: Byte = 0x1F
    const val KEY_3: Byte = 0x20
    const val KEY_4: Byte = 0x21
    const val KEY_5: Byte = 0x22
    const val KEY_6: Byte = 0x23
    const val KEY_7: Byte = 0x24
    const val KEY_8: Byte = 0x25
    const val KEY_9: Byte = 0x26
    const val KEY_0: Byte = 0x27
    
    const val KEY_ENTER: Byte = 0x28
    const val KEY_ESC: Byte = 0x29
    const val KEY_BACKSPACE: Byte = 0x2A
    const val KEY_TAB: Byte = 0x2B
    const val KEY_SPACE: Byte = 0x2C
    
    const val KEY_MINUS: Byte = 0x2D
    const val KEY_EQUAL: Byte = 0x2E
    const val KEY_LEFT_BRACKET: Byte = 0x2F
    const val KEY_RIGHT_BRACKET: Byte = 0x30
    const val KEY_BACKSLASH: Byte = 0x31
    const val KEY_SEMICOLON: Byte = 0x33
    const val KEY_APOSTROPHE: Byte = 0x34
    const val KEY_GRAVE: Byte = 0x35
    const val KEY_COMMA: Byte = 0x36
    const val KEY_DOT: Byte = 0x37
    const val KEY_SLASH: Byte = 0x38
    const val KEY_CAPS_LOCK: Byte = 0x39
    
    const val KEY_F1: Byte = 0x3A
    const val KEY_F2: Byte = 0x3B
    const val KEY_F3: Byte = 0x3C
    const val KEY_F4: Byte = 0x3D
    const val KEY_F5: Byte = 0x3E
    const val KEY_F6: Byte = 0x3F
    const val KEY_F7: Byte = 0x40
    const val KEY_F8: Byte = 0x41
    const val KEY_F9: Byte = 0x42
    const val KEY_F10: Byte = 0x43
    const val KEY_F11: Byte = 0x44
    const val KEY_F12: Byte = 0x45
    
    const val KEY_RIGHT: Byte = 0x4F
    const val KEY_LEFT: Byte = 0x50
    const val KEY_DOWN: Byte = 0x51
    const val KEY_UP: Byte = 0x52
    
    const val KEY_POWER: Byte = 0x66.toByte()
    const val KEY_EJECT: Byte = 0xB8.toShort().toByte() // Note: Often in consumer page, but some keyboards map as regular key
    
    const val MODIFIER_NONE: Byte = 0
    const val MODIFIER_LEFT_CTRL: Byte = 0x01
    const val MODIFIER_LEFT_SHIFT: Byte = 0x02
    const val MODIFIER_LEFT_ALT: Byte = 0x04
    const val MODIFIER_LEFT_GUI: Byte = 0x08 

    // Consumer Control Keys (Usage Page 0x0C)
    const val MEDIA_PLAY_PAUSE: Short = 0x00CD
    const val MEDIA_STOP: Short = 0x00B7
    const val MEDIA_NEXT: Short = 0x00B5
    const val MEDIA_PREVIOUS: Short = 0x00B6
    const val MEDIA_VOL_UP: Short = 0x00E9
    const val MEDIA_VOL_DOWN: Short = 0x00EA
    const val MEDIA_MUTE: Short = 0x00E2
    
    // Display Brightness (Consumer Page)
    const val BRIGHTNESS_UP: Short = 0x006F
    const val BRIGHTNESS_DOWN: Short = 0x0070
    
    // Telephony Keys (Usage Page 0x0C)
    const val CALL_ANSWER: Short = 0x01B1.toShort()
    const val CALL_REJECT: Short = 0x01B2.toShort()

    fun getHidCode(char: Char): KeyEventModel {
        return when (char) {
            'a' -> KeyEventModel(KEY_A)
            'b' -> KeyEventModel(KEY_B)
            'c' -> KeyEventModel(KEY_C)
            'd' -> KeyEventModel(KEY_D)
            'e' -> KeyEventModel(KEY_E)
            'f' -> KeyEventModel(KEY_F)
            'g' -> KeyEventModel(KEY_G)
            'h' -> KeyEventModel(KEY_H)
            'i' -> KeyEventModel(KEY_I)
            'j' -> KeyEventModel(KEY_J)
            'k' -> KeyEventModel(KEY_K)
            'l' -> KeyEventModel(KEY_L)
            'm' -> KeyEventModel(KEY_M)
            'n' -> KeyEventModel(KEY_N)
            'o' -> KeyEventModel(KEY_O)
            'p' -> KeyEventModel(KEY_P)
            'q' -> KeyEventModel(KEY_Q)
            'r' -> KeyEventModel(KEY_R)
            's' -> KeyEventModel(KEY_S)
            't' -> KeyEventModel(KEY_T)
            'u' -> KeyEventModel(KEY_U)
            'v' -> KeyEventModel(KEY_V)
            'w' -> KeyEventModel(KEY_W)
            'x' -> KeyEventModel(KEY_X)
            'y' -> KeyEventModel(KEY_Y)
            'z' -> KeyEventModel(KEY_Z)
            'A' -> KeyEventModel(KEY_A, MODIFIER_LEFT_SHIFT)
            'B' -> KeyEventModel(KEY_B, MODIFIER_LEFT_SHIFT)
            'C' -> KeyEventModel(KEY_C, MODIFIER_LEFT_SHIFT)
            'D' -> KeyEventModel(KEY_D, MODIFIER_LEFT_SHIFT)
            'E' -> KeyEventModel(KEY_E, MODIFIER_LEFT_SHIFT)
            'F' -> KeyEventModel(KEY_F, MODIFIER_LEFT_SHIFT)
            'G' -> KeyEventModel(KEY_G, MODIFIER_LEFT_SHIFT)
            'H' -> KeyEventModel(KEY_H, MODIFIER_LEFT_SHIFT)
            'I' -> KeyEventModel(KEY_I, MODIFIER_LEFT_SHIFT)
            'J' -> KeyEventModel(KEY_J, MODIFIER_LEFT_SHIFT)
            'K' -> KeyEventModel(KEY_K, MODIFIER_LEFT_SHIFT)
            'L' -> KeyEventModel(KEY_L, MODIFIER_LEFT_SHIFT)
            'M' -> KeyEventModel(KEY_M, MODIFIER_LEFT_SHIFT)
            'N' -> KeyEventModel(KEY_N, MODIFIER_LEFT_SHIFT)
            'O' -> KeyEventModel(KEY_O, MODIFIER_LEFT_SHIFT)
            'P' -> KeyEventModel(KEY_P, MODIFIER_LEFT_SHIFT)
            'Q' -> KeyEventModel(KEY_Q, MODIFIER_LEFT_SHIFT)
            'R' -> KeyEventModel(KEY_R, MODIFIER_LEFT_SHIFT)
            'S' -> KeyEventModel(KEY_S, MODIFIER_LEFT_SHIFT)
            'T' -> KeyEventModel(KEY_T, MODIFIER_LEFT_SHIFT)
            'U' -> KeyEventModel(KEY_U, MODIFIER_LEFT_SHIFT)
            'V' -> KeyEventModel(KEY_V, MODIFIER_LEFT_SHIFT)
            'W' -> KeyEventModel(KEY_W, MODIFIER_LEFT_SHIFT)
            'X' -> KeyEventModel(KEY_X, MODIFIER_LEFT_SHIFT)
            'Y' -> KeyEventModel(KEY_Y, MODIFIER_LEFT_SHIFT)
            'Z' -> KeyEventModel(KEY_Z, MODIFIER_LEFT_SHIFT)
            '1' -> KeyEventModel(KEY_1)
            '2' -> KeyEventModel(KEY_2)
            '3' -> KeyEventModel(KEY_3)
            '4' -> KeyEventModel(KEY_4)
            '5' -> KeyEventModel(KEY_5)
            '6' -> KeyEventModel(KEY_6)
            '7' -> KeyEventModel(KEY_7)
            '8' -> KeyEventModel(KEY_8)
            '9' -> KeyEventModel(KEY_9)
            '0' -> KeyEventModel(KEY_0)
            '!' -> KeyEventModel(KEY_1, MODIFIER_LEFT_SHIFT)
            '@' -> KeyEventModel(KEY_2, MODIFIER_LEFT_SHIFT)
            '#' -> KeyEventModel(KEY_3, MODIFIER_LEFT_SHIFT)
            '$' -> KeyEventModel(KEY_4, MODIFIER_LEFT_SHIFT)
            '%' -> KeyEventModel(KEY_5, MODIFIER_LEFT_SHIFT)
            '^' -> KeyEventModel(KEY_6, MODIFIER_LEFT_SHIFT)
            '&' -> KeyEventModel(KEY_7, MODIFIER_LEFT_SHIFT)
            '*' -> KeyEventModel(KEY_8, MODIFIER_LEFT_SHIFT)
            '(' -> KeyEventModel(KEY_9, MODIFIER_LEFT_SHIFT)
            ')' -> KeyEventModel(KEY_0, MODIFIER_LEFT_SHIFT)
            ' ' -> KeyEventModel(KEY_SPACE)
            '\n' -> KeyEventModel(KEY_ENTER)
            '\t' -> KeyEventModel(KEY_TAB)
            '-' -> KeyEventModel(KEY_MINUS)
            '_' -> KeyEventModel(KEY_MINUS, MODIFIER_LEFT_SHIFT)
            '=' -> KeyEventModel(KEY_EQUAL)
            '+' -> KeyEventModel(KEY_EQUAL, MODIFIER_LEFT_SHIFT)
            '[' -> KeyEventModel(KEY_LEFT_BRACKET)
            '{' -> KeyEventModel(KEY_LEFT_BRACKET, MODIFIER_LEFT_SHIFT)
            ']' -> KeyEventModel(KEY_RIGHT_BRACKET)
            '}' -> KeyEventModel(KEY_RIGHT_BRACKET, MODIFIER_LEFT_SHIFT)
            '\\' -> KeyEventModel(KEY_BACKSLASH)
            '|' -> KeyEventModel(KEY_BACKSLASH, MODIFIER_LEFT_SHIFT)
            ';' -> KeyEventModel(KEY_SEMICOLON)
            ':' -> KeyEventModel(KEY_SEMICOLON, MODIFIER_LEFT_SHIFT)
            '\'' -> KeyEventModel(KEY_APOSTROPHE)
            '"' -> KeyEventModel(KEY_APOSTROPHE, MODIFIER_LEFT_SHIFT)
            '`' -> KeyEventModel(KEY_GRAVE)
            '~' -> KeyEventModel(KEY_GRAVE, MODIFIER_LEFT_SHIFT)
            ',' -> KeyEventModel(KEY_COMMA)
            '<' -> KeyEventModel(KEY_COMMA, MODIFIER_LEFT_SHIFT)
            '.' -> KeyEventModel(KEY_DOT)
            '>' -> KeyEventModel(KEY_DOT, MODIFIER_LEFT_SHIFT)
            '/' -> KeyEventModel(KEY_SLASH)
            '?' -> KeyEventModel(KEY_SLASH, MODIFIER_LEFT_SHIFT)
            else -> KeyEventModel(KEY_NONE)
        }
    }
}
