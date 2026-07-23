package com.hematoscope.app.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads an MJPEG-over-HTTP stream and emits decoded frames.
 *
 * This is the fallback for microscopy cameras whose firmware is not UVC-compliant
 * and therefore can only be driven by the vendor's PC software: run a small MJPEG
 * re-broadcaster on the PC (many capture apps, or ffmpeg/mjpg-streamer, can expose
 * `http://<pc-ip>:<port>/stream`) and point HematoScope at that URL. The phone and
 * PC must share a network.
 *
 * The parser scans the multipart stream for JPEG SOI (0xFFD8) / EOI (0xFFD9)
 * markers rather than trusting the boundary header, which is more robust across
 * broadcaster implementations.
 */
class NetworkStreamController {

    fun frames(streamUrl: String, connectTimeoutMs: Int = 8000): Flow<Bitmap> = flow {
        val connection = (URL(streamUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = connectTimeoutMs
            readTimeout = 0 // keep-alive stream
            doInput = true
            connect()
        }
        try {
            BufferedInputStream(connection.inputStream).use { input ->
                val buffer = ByteArrayOutputStream()
                var prev = -1
                var inFrame = false
                while (true) {
                    val b = input.read()
                    if (b == -1) break
                    if (!inFrame) {
                        // Look for JPEG start-of-image (FF D8).
                        if (prev == 0xFF && b == 0xD8) {
                            inFrame = true
                            buffer.reset()
                            buffer.write(0xFF)
                            buffer.write(0xD8)
                        }
                    } else {
                        buffer.write(b)
                        // JPEG end-of-image (FF D9).
                        if (prev == 0xFF && b == 0xD9) {
                            val bytes = buffer.toByteArray()
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { emit(it) }
                            inFrame = false
                        }
                    }
                    prev = b
                }
            }
        } finally {
            runCatching { connection.disconnect() }
        }
    }.flowOn(Dispatchers.IO)
}
