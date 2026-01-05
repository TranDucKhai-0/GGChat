package com.example.ggchat

import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream

object TcpJson {
    fun write(out: DataOutputStream, json: JSONObject) {
        val bytes = json.toString().toByteArray(Charsets.UTF_8)
        out.writeInt(bytes.size)
        out.write(bytes)
        out.flush()
    }

    fun read(input: DataInputStream): JSONObject? {
        return try {
            val len = input.readInt()
            val buf = ByteArray(len)
            input.readFully(buf)
            JSONObject(String(buf, Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }
}
