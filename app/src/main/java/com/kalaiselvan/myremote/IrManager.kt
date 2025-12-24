package com.kalaiselvan.myremote

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log

class IrManager(context: Context) {

    private val irManager: ConsumerIrManager? = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    private val TAG = "IrManager"

    fun hasIrEmitter(): Boolean {
        return irManager?.hasIrEmitter() == true
    }

    fun transmit(hexCode: String, frequency: Int = 38400, isLsb: Boolean = true, repeats: Int = 1) {
        if (irManager == null || !irManager.hasIrEmitter()) {
            Log.e(TAG, "No IR Emitter found")
            return
        }

        val pattern = convertHexToNecPattern(hexCode, isLsb, repeats)
        irManager.transmit(frequency, pattern)
    }

    private fun convertHexToNecPattern(hexCode: String, isLsb: Boolean, repeats: Int): IntArray {
        // Strip 0x prefix if present and parse to Long to handle unsigned 32-bit properly
        val cleanHex = hexCode.removePrefix("0x")
        val codeValue = cleanHex.toLong(16)

        val list = mutableListOf<Int>()

        // Leader code
        list.add(9000)
        list.add(4500)

        // NEC sends 32 bits as 4 bytes.
        // The standard usually implies sending the bytes in order: Address, ~Address, Command, ~Command.
        // Within each byte, bits are sent LSB first.
        
        for (byteShift in listOf(24, 16, 8, 0)) {
            val byteValue = (codeValue shr byteShift).toInt() and 0xFF
            
            // Send bits LSB first (0 to 7) or MSB first (7 downTo 0)
            if (isLsb) {
                for (i in 0..7) {
                    val bit = (byteValue shr i) and 1
                    addBit(list, bit)
                }
            } else {
                 for (i in 7 downTo 0) {
                    val bit = (byteValue shr i) and 1
                    addBit(list, bit)
                }
            }
        }

        // Stop bit (562us ON)
        list.add(562)
        
        // Repeat Codes
        for (i in 0 until repeats) {
            // Gap (40ms)
            list.add(40000)
            // Repeat Sequence: 9ms ON, 2.25ms OFF, 562us ON
            list.add(9000)
            list.add(2250)
            list.add(562)
        }

        // Final Trailing Silence
        list.add(40000)

        return list.toIntArray()
    }
    
    private fun addBit(list: MutableList<Int>, bit: Int) {
        if (bit == 1) {
            list.add(562)
            list.add(1687)
        } else {
            list.add(562)
            list.add(562)
        }
    }
}
