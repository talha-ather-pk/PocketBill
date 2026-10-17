package com.example.pocketbill.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * P4-T14 / MVP_SPEC 8: the app asks for no permissions (above all no INTERNET), Android cloud
 * backup stays off (D-009), and only the launcher activity is open to other apps. Fails the build
 * if a change breaks any of these; a new permission needs a documented reason first.
 */
class ManifestTest {
    private val android = "http://schemas.android.com/apk/res/android"

    // Unit tests run with the app module as the working directory.
    private val manifest = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))

    private fun elements(tag: String) = manifest.getElementsByTagName(tag).let { list -> (0 until list.length).map { list.item(it) as org.w3c.dom.Element } }

    @Test fun `no permissions are requested`() {
        assertEquals(emptyList<String>(), elements("uses-permission").map { it.getAttributeNS(android, "name") })
        assertTrue(elements("uses-permission-sdk-23").isEmpty())
    }

    @Test fun `android backup is off and extraction rules are set`() {
        val app = elements("application").single()
        assertEquals("false", app.getAttributeNS(android, "allowBackup"))
        assertEquals("@xml/data_extraction_rules", app.getAttributeNS(android, "dataExtractionRules"))
        val rules = File("src/main/res/xml/data_extraction_rules.xml").readText()
        val cloud = rules.substringAfter("<cloud-backup>").substringBefore("</cloud-backup>")
        assertFalse("cloud backup must include nothing", cloud.contains("<include"))
    }

    @Test fun `only the launcher activity is exported`() {
        val exported = listOf("activity", "provider", "receiver", "service").flatMap(::elements)
            .filter { it.getAttributeNS(android, "exported") == "true" }
            .map { it.getAttributeNS(android, "name") }
        assertEquals(listOf(".MainActivity"), exported)
    }

    @Test fun `the PDF provider shares the pdf cache folder only`() {
        val paths = File("src/main/res/xml/file_paths.xml").readText()
        assertTrue(paths.contains("<cache-path name=\"pdf\" path=\"pdf/\" />"))
        assertEquals(1, Regex("-path ").findAll(paths).count())
    }
}
