package com.example.pocketbill.domain.invoice

/** An invoice number and the sequence value it was made from. */
data class AssignedNumber(val number: String, val sequence: Long)

/** Invoice numbering rules N-1 to N-10 (DATA_MODEL.md section 5.1). */
object InvoiceNumbering {
    /** N-2: prefix plus the sequence padded to 4 digits: "INV-0001", then "INV-10000" past 9999. */
    fun format(prefix: String, sequence: Long): String {
        require(sequence >= 1) { "Sequence must be at least 1: $sequence" }
        return prefix + sequence.toString().padStart(4, '0')
    }

    /**
     * N-7: the first number at or after [nextSequence] that [isTaken] says is free. Used when the
     * prefix was changed back or the starting number was lowered, so numbers stay unique.
     */
    inline fun firstFree(prefix: String, nextSequence: Long, isTaken: (String) -> Boolean): AssignedNumber {
        var sequence = maxOf(1L, nextSequence)
        while (true) {
            val number = format(prefix, sequence)
            if (!isTaken(number)) return AssignedNumber(number, sequence)
            sequence++
        }
    }
}
