package com.example.monetto

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun TransactionCard(transaction: TransactionItem, modifier: Modifier = Modifier) {
    // NOTE: Этот компонент похож по функциональности на TransactionRow.kt, но использует
    // Material3 Card и белый фон, что контрастирует с общей темной темой приложения.
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp), // Добавлен отступ для лучшего отображения
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = transaction.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black // Текст должен быть черным на белом фоне
                )
            }
            // Цвет зависит от типа транзакции: зеленый (доход) или красный (расход)
            // Добавлено явное форматирование суммы.
            val amountColor = if (transaction.isIncome) Color(0xFF4CAF50) else Color.Red
            val formattedAmount = (if (transaction.isIncome) "+" else "-") + "${"%,.2f".format(transaction.amount)} €"

            Text(
                text = formattedAmount,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = amountColor
            )
        }
    }
}