package com.example.pocketbill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pocketbill.ui.navigation.PocketBillApp
import com.example.pocketbill.ui.theme.PocketBillTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketBillTheme {
                PocketBillApp()
            }
        }
    }
}
