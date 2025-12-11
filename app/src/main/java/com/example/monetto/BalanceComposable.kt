package com.example.monetto

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

@Composable
fun BalanceText(balance: Double) {
    // Форматируем число: 4500.89 → 4,500.89
    val formatted = remember(balance) {
        DecimalFormat("#,##0.00").format(balance)
    }

    Text(
        text = "€$formatted", // Можно заменить € на любую валюту позже
        fontSize = 45.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}
