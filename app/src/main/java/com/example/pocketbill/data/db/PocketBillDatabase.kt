package com.example.pocketbill.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's database (DATA_MODEL.md section 1). Version 1 stays open until the first release
 * (DECISIONS D-014); after that, every change needs a migration and its test (DATA_MODEL 7).
 */
@Database(
    entities = [
        BusinessProfileEntity::class,
        ClientEntity::class,
        InvoiceEntity::class,
        LineItemEntity::class,
        ItemPresetEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PocketBillDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun presetDao(): PresetDao

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
