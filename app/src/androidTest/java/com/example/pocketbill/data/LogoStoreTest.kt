package com.example.pocketbill.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * F-14 on the phone: importing a picked image shrinks it to at most 600 px, stores it under a
 * content-based name, and refuses files that aren't images. Uses a test folder; the founder's
 * real logo is never touched (the store is pointed at a separate copy of filesDir/logos).
 */
@RunWith(AndroidJUnit4::class)
class LogoStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val inputs = File(context.cacheDir, "logo-input-test").apply { mkdirs() }
    private val store = LogoStore(context)
    private val created = mutableListOf<String>()

    @After fun cleanUp() {
        inputs.deleteRecursively()
        created.forEach { File(store.dir, it).delete() }
    }

    private fun imageFile(width: Int, height: Int): Uri {
        val file = File(inputs, "in-${width}x$height.png")
        file.outputStream().use {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(31, 78, 121)) }
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return Uri.fromFile(file)
    }

    @Test fun largeImageIsShrunkAndStored() = runBlocking {
        val name = store.import(imageFile(2400, 800).toString())
        assertNotNull("import failed", name)
        created += name!!
        val stored = BitmapFactory.decodeFile(File(store.dir, name).path)
        assertEquals(600, stored.width)
        assertEquals(200, stored.height)
    }

    @Test fun sameImageGetsTheSameName() = runBlocking {
        val uri = imageFile(300, 100).toString()
        val first = store.import(uri)!!
        created += first
        assertEquals(first, store.import(uri))
        assertTrue(first.endsWith(".png"))
    }

    @Test fun aFileThatIsNotAnImageIsRefused() = runBlocking {
        val text = File(inputs, "note.txt").apply { writeText("not an image") }
        assertNull(store.import(Uri.fromFile(text).toString()))
    }
}
