package com.example.pocketbill.domain.text

import java.text.Normalizer

/**
 * Case- and accent-insensitive form of a name, for duplicate-client checks and search
 * (UX_SPEC.md sections 5 and 6): "Müller Design GmbH" and "muller design gmbh" match.
 */
fun normalizeForMatching(text: String): String =
    Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("\\s+"), " ")
        .lowercase()
