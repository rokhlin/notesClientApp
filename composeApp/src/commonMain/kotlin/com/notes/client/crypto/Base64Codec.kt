package com.notes.client.crypto

/**
 * Pure Kotlin Multiplatform Base64 codec compatible with Android, iOS, Desktop, and WasmJs.
 */
object Base64Codec {
    private const val TABLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(bytes: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i++].toInt() and 0xFF
            val b1 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1
            val b2 = if (i < bytes.size) bytes[i++].toInt() and 0xFF else -1

            val c0 = b0 ushr 2
            val c1 = ((b0 and 0x03) shl 4) or if (b1 >= 0) (b1 ushr 4) else 0
            val c2 = if (b1 >= 0) ((b1 and 0x0F) shl 2) or if (b2 >= 0) (b2 ushr 6) else 0 else -1
            val c3 = if (b2 >= 0) b2 and 0x3F else -1

            sb.append(TABLE[c0])
            sb.append(TABLE[c1])
            sb.append(if (c2 >= 0) TABLE[c2] else '=')
            sb.append(if (c3 >= 0) TABLE[c3] else '=')
        }
        return sb.toString()
    }

    fun decode(str: String): ByteArray {
        val clean = str.filter { it in TABLE || it == '=' }
        val out = mutableListOf<Byte>()
        var i = 0
        while (i < clean.length) {
            val c0 = TABLE.indexOf(clean[i++])
            val c1 = if (i < clean.length) TABLE.indexOf(clean[i++]) else -1
            val c2 = if (i < clean.length) TABLE.indexOf(clean[i++]) else -1
            val c3 = if (i < clean.length) TABLE.indexOf(clean[i++]) else -1

            if (c0 >= 0 && c1 >= 0) {
                out.add(((c0 shl 2) or (c1 ushr 4)).toByte())
            }
            if (c1 >= 0 && c2 >= 0) {
                out.add((((c1 and 0x0F) shl 4) or (c2 ushr 2)).toByte())
            }
            if (c2 >= 0 && c3 >= 0) {
                out.add((((c2 and 0x03) shl 6) or c3).toByte())
            }
        }
        return out.toByteArray()
    }
}
