package com.example.rabit.domain.model

data class KeyEventModel(
    val keyCode: Byte,
    val modifier: Byte = 0
)

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
    
    const val KEY_F1: Byte = 0x3A
    const val KEY_F2: Byte = 0x3B
    const val KEY_F3: Byte = 0x3C
    const val KEY_F8: Byte = 0x41 // Play/Pause on Mac
    const val KEY_F10: Byte = 0x43
    const val KEY_F11: Byte = 0x44 // Vol Down on Mac
    const val KEY_F12: Byte = 0x45 // Vol Up on Mac

    const val MODIFIER_NONE: Byte = 0
    const val MODIFIER_LEFT_CTRL: Byte = 0x01
    const val MODIFIER_LEFT_SHIFT: Byte = 0x02
    const val MODIFIER_LEFT_ALT: Byte = 0x04
    const val MODIFIER_LEFT_GUI: Byte = 0x08 // Command on Mac
    
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
            '0' -> KeyEventModel(KEY_0)
            '1' -> KeyEventModel(KEY_1)
            '2' -> KeyEventModel(KEY_2)
            '3' -> KeyEventModel(KEY_3)
            '4' -> KeyEventModel(KEY_4)
            '5' -> KeyEventModel(KEY_5)
            '6' -> KeyEventModel(KEY_6)
            '7' -> KeyEventModel(KEY_7)
            '8' -> KeyEventModel(KEY_8)
            '9' -> KeyEventModel(KEY_9)
            ' ' -> KeyEventModel(KEY_SPACE)
            '\n' -> KeyEventModel(KEY_ENTER)
            '6' -> KeyEventModel(KEY_6)
            '2' -> KeyEventModel(KEY_2)
            '0' -> KeyEventModel(KEY_0)
            else -> KeyEventModel(KEY_NONE)
        }
    }
}
