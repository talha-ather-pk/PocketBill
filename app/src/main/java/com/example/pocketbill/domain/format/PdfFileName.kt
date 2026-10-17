package com.example.pocketbill.domain.format

import java.text.Normalizer

/**
 * PDF file names (MVP_SPEC.md section 6): `INV-0014_Muller-Design-GmbH.pdf`. ASCII-only so every
 * email and messaging app accepts them; the PDF itself keeps the accents.
 */
object PdfFileName {
    /** Letters with no accent to strip, spelled out instead (Ø→O, ß→ss, Æ→AE, Ł→L). */
    private val spelledOut = mapOf(
        'Ø' to "O", 'ø' to "o", 'ß' to "ss", 'Æ' to "AE", 'æ' to "ae", 'Œ' to "OE", 'œ' to "oe",
        'Ł' to "L", 'ł' to "l", 'Đ' to "D", 'đ' to "d", 'Ð' to "D", 'ð' to "d", 'Þ' to "Th", 'þ' to "th",
        'ı' to "i", 'Ħ' to "H", 'ħ' to "h",
    )

    /** Keeps names readable in file lists; longer client names are cut. */
    private const val MAX_PART_LENGTH = 60

    /** [number] is null for drafts, which have no number yet. */
    fun forInvoice(number: String?, clientName: String?): String {
        val first = number?.let(::asciiPart)?.ifEmpty { null } ?: "Draft"
        val second = clientName?.let(::asciiPart).orEmpty()
        return if (second.isEmpty()) "$first.pdf" else "${first}_$second.pdf"
    }

    /** "Müller Design GmbH" → "Muller-Design-GmbH". */
    fun asciiPart(text: String): String {
        val spelled = buildString { text.forEach { append(spelledOut[it] ?: it) } }
        val stripped = Normalizer.normalize(spelled, Normalizer.Form.NFD).replace(COMBINING_MARKS, "")
        return stripped
            .replace(NOT_SAFE, "-")
            .trim('-')
            .take(MAX_PART_LENGTH)
            .trimEnd('-')
    }

    private val COMBINING_MARKS = Regex("\\p{M}+")
    private val NOT_SAFE = Regex("[^A-Za-z0-9]+")
}
