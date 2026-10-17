package com.example.pocketbill.domain.invoice

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Business details frozen onto an invoice when it is issued (DATA_MODEL.md sections 5.2–5.3),
 * so the invoice keeps looking the way it was sent after the profile changes.
 */
@Serializable
data class FromSnapshot(
    val v: Int = 1,
    val name: String,
    val contactLines: String = "",
    val address: String = "",
    val taxId: String? = null,
    val paymentInstructions: String = "",
    val logoFile: String? = null,
)

/** Client details frozen onto an invoice when it is issued. */
@Serializable
data class ToSnapshot(
    val v: Int = 1,
    val name: String,
    val email: String? = null,
    val address: String? = null,
    val country: String? = null,
    val taxId: String? = null,
)

/** Reads and writes snapshot JSON. Unknown fields are ignored so later versions can add fields (5.3). */
object SnapshotJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(snapshot: FromSnapshot): String = json.encodeToString(FromSnapshot.serializer(), snapshot)

    fun encode(snapshot: ToSnapshot): String = json.encodeToString(ToSnapshot.serializer(), snapshot)

    /** Null if the text can't be read; the caller shows a warning and never rewrites it (5.3). */
    fun decodeFrom(text: String): FromSnapshot? = try {
        json.decodeFromString(FromSnapshot.serializer(), text)
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    fun decodeTo(text: String): ToSnapshot? = try {
        json.decodeFromString(ToSnapshot.serializer(), text)
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}
