package com.example.pocketbill.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Turns a generated PDF into page images for the preview screen, using Android's PdfRenderer. */
object PdfPageImages {
    suspend fun render(file: File, widthPx: Int): List<Bitmap> = withContext(Dispatchers.Default) {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                (0 until renderer.pageCount).map { index ->
                    renderer.openPage(index).use { page ->
                        val height = widthPx * page.height / page.width
                        Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888).also {
                            // Pages have no background of their own; paper is white.
                            it.eraseColor(Color.WHITE)
                            page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
            }
        }
    }
}
