package com.example.pocketbill.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import kotlin.math.max

/** Logo files by name (F-14). Plain bytes, so backup code is tested without Android. */
interface LogoFiles {
    fun read(name: String): ByteArray?

    /** Stores a PNG and returns its content-based name. */
    fun write(png: ByteArray): String
}

/**
 * Business logos (F-14, DATA_MODEL 5.3): picked with the system photo picker (no permission),
 * shrunk to at most [MAX_SIDE_PX] and stored privately as PNG. Files are named by their content,
 * so a new logo never overwrites the one older invoices were issued with.
 */
class LogoStore(private val context: Context) : LogoFiles {
    val dir: File get() = File(context.filesDir, "logos")

    /** Null if the picked file isn't a readable image. */
    suspend fun import(uri: String): String? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val parsed = Uri.parse(uri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return@withContext null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2
        val decoded = resolver.openInputStream(parsed)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@withContext null
        val scale = MAX_SIDE_PX.toFloat() / max(decoded.width, decoded.height)
        val sized = if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt().coerceAtLeast(1), (decoded.height * scale).toInt().coerceAtLeast(1), true)
        } else {
            decoded
        }
        val png = ByteArrayOutputStream().use { out ->
            sized.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        write(png)
    }

    override fun read(name: String): ByteArray? = File(dir, name).takeIf { it.isFile }?.readBytes()

    override fun write(png: ByteArray): String {
        val name = MessageDigest.getInstance("SHA-256").digest(png).joinToString("") { "%02x".format(it) }.take(24) + ".png"
        dir.mkdirs()
        val file = File(dir, name)
        if (!file.exists()) file.writeBytes(png)
        return name
    }

    companion object {
        /** Plenty for a 160 × 48 pt PDF header at print resolution. */
        const val MAX_SIDE_PX = 600
    }
}
