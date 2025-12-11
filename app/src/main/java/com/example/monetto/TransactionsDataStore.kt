package com.example.monetto

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// DataStore Preferences: Используется для простой локальной персистентности данных.
val Context.transactionsDataStore by preferencesDataStore("transactions_store")
private val TRANSACTIONS_KEY = stringPreferencesKey("transactions_list")

// 1. Класс данных транзакции. `@kotlinx.serialization.Serializable` позволяет сохранять его в JSON.
@kotlinx.serialization.Serializable
data class TransactionItem(
    val id: Long = System.currentTimeMillis(), // Уникальный ID, основанный на времени создания
    val name: String,
    val category: String,
    val amount: Double,
    val isIncome: Boolean,
    val date: Long = System.currentTimeMillis(), // Дата транзакции (в миллисекундах)
    val iconRes: Int? = null // Ресурс иконки (пока не используется, но оставлен для возможного расширения)
)

/**
 * Сохраняет новую транзакцию в DataStore.
 * Транзакция добавляется в начало списка (индекс 0).
 */
suspend fun saveTransaction(context: Context, transaction: TransactionItem) {
    context.transactionsDataStore.edit { prefs ->
        // Читаем старый список или инициализируем пустой
        val oldListJson = prefs[TRANSACTIONS_KEY] ?: "[]"
        // Десериализуем список
        val list = Json.decodeFromString<List<TransactionItem>>(oldListJson).toMutableList()
        // Добавляем новую транзакцию в начало
        list.add(0, transaction)
        // Сериализуем и сохраняем обратно
        prefs[TRANSACTIONS_KEY] = Json.encodeToString(list)
    }
}

/**
 * Возвращает Flow со списком транзакций, который будет автоматически обновляться при изменении DataStore.
 */
fun getTransactionsFlow(context: Context) = context.transactionsDataStore.data.map { prefs ->
    val json = prefs[TRANSACTIONS_KEY] ?: "[]"
    Json.decodeFromString<List<TransactionItem>>(json)
}

@kotlinx.serialization.Serializable
data class ReportItem(
    val category: String,         // Категория, на которую установлен лимит
    val limitAmount: Double,      // Установленный лимит
    val period: String = "Month"  // Период: Week, Month, Year (по умолчанию)
)

// 2. DataStore ключ для хранения списка Отчетов/Лимитов
val REPORTS_KEY = stringPreferencesKey("reports_list")

/**
 * Сохраняет (или обновляет) ReportItem в DataStore.
 * Обновляет существующий лимит, если категория совпадает.
 */
suspend fun saveReportItem(context: Context, report: ReportItem) {
    context.transactionsDataStore.edit { prefs ->
        val oldListJson = prefs[REPORTS_KEY] ?: "[]"
        val list = Json.decodeFromString<List<ReportItem>>(oldListJson).toMutableList()

        // Удаляем старый, если существует, и добавляем новый
        list.removeAll { it.category == report.category }
        list.add(0, report)

        prefs[REPORTS_KEY] = Json.encodeToString(list)
    }
}

/**
 * Возвращает Flow со списком ReportItem.
 */
fun getReportsFlow(context: Context) = context.transactionsDataStore.data
    .map { prefs ->
        val json = prefs[REPORTS_KEY] ?: "[]"
        try {
            Json.decodeFromString<List<ReportItem>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
@kotlinx.serialization.Serializable
data class GoalItem(
    val id: Long = System.currentTimeMillis(),
    val name: String,             // Название цели (например, "Новый Macbook")
    val targetAmount: Double,     // Целевая сумма
    val savedAmount: Double = 0.0, // Текущая накопленная сумма
    val deadline: Long,           // Дата окончания срока (в миллисекундах)
    val colorHex: String,         // Цвет в формате HEX (например, "#FF0000")
    val iconRes: Int,             // Ресурс иконки
    val periodUnit: String        // Единица срока: Year, Month, Week, Day
)

// Ключ для DataStore
private val GOALS_KEY = stringPreferencesKey("goals_list")

/**
 * Возвращает Flow со списком целей, который будет автоматически обновляться.
 */
fun getGoalsFlow(context: Context): Flow<List<GoalItem>> {
    return context.transactionsDataStore.data
        .map { prefs ->
            val json = prefs[GOALS_KEY] ?: "[]"
            try {
                Json.decodeFromString<List<GoalItem>>(json)
            } catch (e: Exception) {
                // В случае ошибки десериализации (например, при первом запуске), возвращаем пустой список
                e.printStackTrace()
                emptyList()
            }
        }
}

/**
 * Сохраняет новую цель в DataStore.
 */
suspend fun saveGoal(context: Context, goal: GoalItem) {
    context.transactionsDataStore.edit { prefs ->
        val oldListJson = prefs[GOALS_KEY] ?: "[]"
        val list = Json.decodeFromString<List<GoalItem>>(oldListJson).toMutableList()
        list.add(0, goal)
        prefs[GOALS_KEY] = Json.encodeToString(list)
    }
}
suspend fun updateGoal(context: Context, updatedGoal: GoalItem) {
    context.transactionsDataStore.edit { prefs ->
        val oldListJson = prefs[GOALS_KEY] ?: "[]"
        val list = Json.decodeFromString<List<GoalItem>>(oldListJson).toMutableList()

        // Находим индекс цели, которую нужно обновить
        val index = list.indexOfFirst { it.id == updatedGoal.id }

        if (index != -1) {
            // Заменяем старую цель обновленной
            list[index] = updatedGoal
        } else {
            // Если цель не найдена (чего не должно быть), добавляем ее как новую
            list.add(updatedGoal)
        }

        prefs[GOALS_KEY] = Json.encodeToString(list)
    }
}
enum class AppCurrency(val code: String, val symbol: String, val rateToEuro: Double) {
    EUR("EUR", "€", 1.0),
    USD("USD", "$", 1.05), // 1 Евро = 1.05 Доллара
    RUB("RUB", "₽", 105.0), // 1 Евро = 105 Рублей
    GBP("GBP", "£", 0.85)
}

// Ключ для сохранения выбранной валюты
val CURRENCY_KEY = androidx.datastore.preferences.core.stringPreferencesKey("selected_currency")
