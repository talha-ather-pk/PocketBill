package com.example.pocketbill.data.repository

import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.domain.invoice.FromSnapshot
import com.example.pocketbill.domain.invoice.SnapshotJson
import com.example.pocketbill.domain.invoice.ToSnapshot

/** Freezes the business details for an issued invoice (DATA_MODEL.md section 5.2). */
internal fun BusinessProfileEntity.toSnapshotJson(): String = SnapshotJson.encode(
    FromSnapshot(
        name = name,
        contactLines = contactLines,
        address = address,
        taxId = taxId,
        paymentInstructions = paymentInstructions,
        logoFile = logoFile,
    ),
)

/** Freezes the client details for an issued invoice. */
internal fun ClientEntity.toSnapshotJson(): String = SnapshotJson.encode(
    ToSnapshot(name = name, email = email, address = address, country = country, taxId = taxId),
)
