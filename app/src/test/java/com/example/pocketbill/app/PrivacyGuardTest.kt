package com.example.pocketbill.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * P5-T06: invoice data never reaches the system log or the network. Only [SafeLog] may log,
 * and the app has no networking code at all (no INTERNET permission either, see ManifestTest).
 */
class PrivacyGuardTest {
    private val sources = File("src/main/java").walk().filter { it.isFile && it.extension == "kt" }.toList()

    private fun offenders(pattern: Regex, allowed: Set<String> = emptySet()) = sources
        .filter { it.name !in allowed }
        .flatMap { file -> file.readLines().filter { pattern.containsMatchIn(it) }.map { "${file.name}: ${it.trim()}" } }

    @Test fun `only SafeLog writes to the log`() {
        val found = offenders(Regex("""import android\.util\.Log\b|println\(|printStackTrace\(""""), allowed = setOf("SafeLog.kt"))
        assertTrue("Use SafeLog instead:\n" + found.joinToString("\n"), found.isEmpty())
    }

    @Test fun `no networking code`() {
        val found = offenders(Regex("""import (java\.net\.(URL|HttpURLConnection|Socket)|okhttp3|retrofit2|io\.ktor)"""))
        assertTrue("Networking code found:\n" + found.joinToString("\n"), found.isEmpty())
    }
}
