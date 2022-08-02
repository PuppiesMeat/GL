package com.hs.opengl.sharetexture

import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and

/**
 *
 * Date: 2022/7/22
 * Time: 15:04
 * @author shun.he
 */
object HexUtil {

    fun hex2Bytes(hex: String, dest: ByteArray?): ByteArray? {
        if (hex.isEmpty()) return null
        val destination = dest ?: ByteArray(hex.length / 2)
        var j = 0
        for (i in hex.indices step 2) {
            destination[j] = Integer.parseInt(hex.substring(i, i + 2), 16).toByte()
            j++
        }
        return destination
    }

    fun hexBytes2HexStr(bytes: ByteArray): String {
        return String(bytes)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val a = "b6".toByte(16)
        println("$a")
    }

    fun parseHexStr2Byte(hexStr: String): ByteArray? {
        return if (hexStr.length < 1) {
            null
        } else {
            val result = ByteArray(hexStr.length / 2)
            for (i in 0 until hexStr.length / 2) {
                val high = hexStr.substring(i * 2, i * 2 + 1).toInt(16)
                val low = hexStr.substring(i * 2 + 1, i * 2 + 2).toInt(16)
                result[i] = (high * 16 + low).toByte()
            }
            result
        }
    }

    fun byte2Hex(data: ByteArray): String? {

        val sb = StringBuffer()
        (data.indices).forEach {
            var hex = Integer.toHexString((data[it] and 0xFF.toByte()).toInt())
            if (hex.length < 2) {
                hex = "0$hex"
            }
            sb.append(hex.toUpperCase())
        }
        return sb.toString()
    }

    fun asUuid(bytes: ByteArray?): UUID? {
        val bb: ByteBuffer = ByteBuffer.wrap(bytes)
        val firstLong: Long = bb.long
        val secondLong: Long = bb.long
        return UUID(firstLong, secondLong)
    }

    fun asBytes(uuid: UUID): ByteArray? {
        val bb: ByteBuffer = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }

}