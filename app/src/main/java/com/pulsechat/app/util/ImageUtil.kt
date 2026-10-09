package com.pulsechat.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

object ImageUtil {

    fun readBytes(context: Context, uri: Uri): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: Exception) {
        null
    }

    /** downscale + recompress (WhatsApp-style "low quality") */
    fun compress(context: Context, uri: Uri, maxDim: Int = 1280, quality: Int = 70): ByteArray? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val bmp = BitmapFactory.decodeStream(input) ?: return null
            val scale = min(1f, maxDim.toFloat() / max(bmp.width, bmp.height))
            val w = (bmp.width * scale).toInt().coerceAtLeast(1)
            val h = (bmp.height * scale).toInt().coerceAtLeast(1)
            val scaled = if (scale < 1f) Bitmap.createScaledBitmap(bmp, w, h, true) else bmp
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
