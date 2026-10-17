package com.example.pocketbill.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's database (DATA_MODEL.md section 1). Every schema change bumps [version] and ships with
 * a migration and its test (DATA_MODEL 7, DECISIONS D-016). Exported schemas in app/schemas are
 * never edited after they are committed.
 *
 * - v1: first schema (P4-T03).
 * - v2: client.preferredCurrency and client.preferredTermsDays, both nullable (D-015).
 */
@Database(
    entities = [
        BusinessProfileEntity::class,
        ClientEntity::class,
        InvoiceEntity::class,
        LineItemEntity::class,
        ItemPresetEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // adds two nullable client columns; existing rows keep NULL
    ],
)
abstract class PocketBillDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun presetDao(): PresetDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val FILE_NAME = "pocketbill.db"

        /**
         * Opens the database file. Deliberately never calls fallbackToDestructiveMigration:
         * a missing migration must fail in testing, never wipe a user's invoices (DATA_MODEL G-1).
         */
        fun open(context: Context): PocketBillDatabase =
            Room.databaseBuilder(context.applicationContext, PocketBillDatabase::class.java, FILE_NAME).build()
    }
}
