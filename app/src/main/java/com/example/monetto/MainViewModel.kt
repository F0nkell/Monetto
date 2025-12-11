package com.example.monetto

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// Единственный ViewModel для управления финансами Monetto
class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

    // --- 1. УПРАВЛЕНИЕ ОБЩИМ БАЛАНСОМ ---

    // Ключ для сохранения баланса
    private val BALANCE_KEY = longPreferencesKey("main_balance_cents")

    // ВАЖНО: Мы используем transactionsDataStore, который объявлен в файле TransactionsDataStore.kt
    // Не создаем здесь новый dataStore!

    // Баланс хранится в центах (Long) для точности
    val balanceCentsFlow = context.transactionsDataStore.data
        .map { prefs -> prefs[BALANCE_KEY] ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    // Функции для обновления баланса в DataStore
    private fun addToBalance(amountCents: Long) {
        viewModelScope.launch {
            context.transactionsDataStore.edit { prefs ->
                val current = prefs[BALANCE_KEY] ?: 0L
                prefs[BALANCE_KEY] = current + amountCents
            }
        }
    }

    fun setBalance(newBalanceCents: Long) {
        viewModelScope.launch {
            context.transactionsDataStore.edit { prefs ->
                prefs[BALANCE_KEY] = newBalanceCents
            }
        }
    }

    // --- 2. УПРАВЛЕНИЕ ТРАНЗАКЦИЯМИ ---

    private val _transactions = MutableStateFlow<List<TransactionItem>>(emptyList())
    val transactionsFlow: StateFlow<List<TransactionItem>> = _transactions.asStateFlow()

    // --- 4. УПРАВЛЕНИЕ ОТЧЕТАМИ (ЛИМИТАМИ) ---
    private val _reports = MutableStateFlow<List<ReportItem>>(emptyList())
    val reportsFlow: StateFlow<List<ReportItem>> = _reports.asStateFlow()

    // --- 5. УПРАВЛЕНИЕ ЦЕЛЯМИ (СЧЕТАМИ) ---
    private val _goals = MutableStateFlow<List<GoalItem>>(emptyList())
    val goalsFlow: StateFlow<List<GoalItem>> = _goals.asStateFlow()

    init {
        // Подписываемся на изменения данных при создании ViewModel
        viewModelScope.launch {
            getTransactionsFlow(context).collect { transactions ->
                _transactions.value = transactions
            }
        }
        viewModelScope.launch {
            getReportsFlow(context).collect { reports ->
                _reports.value = reports
            }
        }
        viewModelScope.launch {
            getGoalsFlow(context).collect { goals ->
                _goals.value = goals
            }
        }
    }

    // Добавление новой транзакции
    fun addTransaction(
        title: String,
        category: String,
        amount: Double,
        isIncome: Boolean,
        iconRes: Int? = null
    ) {
        viewModelScope.launch {
            val newTransaction = TransactionItem(
                name = title,
                category = category,
                amount = amount,
                isIncome = isIncome,
                iconRes = iconRes
            )

            // 1. Сохраняем транзакцию в список
            saveTransaction(context, newTransaction)

            // 2. Обновляем глобальный баланс
            // Конвертируем сумму в центы и определяем знак (+/-)
            val amountCents = (newTransaction.amount * 100).toLong() * (if (newTransaction.isIncome) 1 else -1)
            addToBalance(amountCents)
        }
    }

    // --- 3. РАСЧЕТ АГРЕГАТОВ ДЛЯ КАРТОЧЕК ---

    val totalIncomeAllTime: Double
        get() = transactionsFlow.value
            .filter { it.isIncome }
            .sumOf { it.amount.absoluteValue }

    val totalExpensesAllTime: Double
        get() = transactionsFlow.value
            .filter { !it.isIncome }
            .sumOf { it.amount.absoluteValue }

    // Используется для отображения общего баланса на главном экране.
    // Берем из сохраненного значения (balanceCentsFlow), чтобы учитывать ручные корректировки.
    val balanceAllTime: Double
        get() = balanceCentsFlow.value / 100.0

    /**
     * Сохраняет (или обновляет) лимит отчета.
     */
    fun setReportLimit(category: String, limit: Double, period: String) {
        viewModelScope.launch {
            val reportItem = ReportItem(category, limit, period)
            saveReportItem(context, reportItem)
        }
    }

    // Добавление новой цели
    fun addGoal(goal: GoalItem) {
        viewModelScope.launch {
            saveGoal(context, goal)
        }
    }

    /**
     * Обновляет накопленную сумму цели и регистрирует транзакцию.
     * @param goalId ID цели.
     * @param amount Сумма пополнения (положительное число) или снятия (отрицательное число).
     */
    fun updateGoalAmount(goalId: Long, amount: Double) {
        viewModelScope.launch {
            val currentGoal = _goals.value.find { it.id == goalId }

            if (currentGoal != null) {
                // 1. Обновление цели
                val newSavedAmount = (currentGoal.savedAmount + amount).coerceAtLeast(0.0)
                val updatedGoal = currentGoal.copy(savedAmount = newSavedAmount)

                updateGoal(context, updatedGoal) // Сохраняем обновленную цель

                // 2. Создание транзакции для записи в историю
                val isIncome = amount > 0
                val transactionName = if (isIncome) "Пополнение счета: ${currentGoal.name}" else "Снятие со счета: ${currentGoal.name}"

                val newTransaction = TransactionItem(
                    name = transactionName,
                    category = "Сбережения/Цели",
                    amount = amount.absoluteValue,
                    isIncome = !isIncome, // Внимание: Пополнение цели = Расход из кошелька (!isIncome)
                    iconRes = currentGoal.iconRes
                )

                // 3. Сохраняем транзакцию
                saveTransaction(context, newTransaction)

                // 4. Обновляем общий баланс
                // Если пополняем цель (amount > 0), деньги уходят из кошелька -> баланс уменьшается (-1)
                val amountCents = (amount.absoluteValue * 100).toLong() * (if (isIncome) -1 else 1)
                addToBalance(amountCents)
            }
        }
    }
}