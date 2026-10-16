package com.example.pocketbill.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ARCHITECTURE.md section 3: code in `domain/` is pure Kotlin. It must not import Android,
 * so the business rules stay testable on the computer without a phone.
 */
class DomainPurityTest {

    @Test fun `domain code has no Android imports`() {
        // Unit tests run with the app module as the working directory.
        val domainDir = File("src/main/java/com/example/pocketbill/domain")
        assertTrue("Domain folder not found at ${domainDir.absolutePath}", domainDir.isDirectory)

        val offenders = domainDir.walk()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines()
                    .filter { it.trimStart().startsWith("import android") }
                    .map { "${file.name}: ${it.trim()}" }
            }
            .toList()

        assertTrue("Android imports in domain code:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }
}
