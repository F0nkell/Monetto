package com.example.monetto

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val context = getApplication<Application>().applicationContext

    // --- 0. УПРАВЛЕНИЕ ВАЛЮТОЙ (НОВОЕ) ---

    // По умолчанию Евро
    private val _currency = MutableStateFlow(AppCurrency.EUR)
    val currencyFlow: StateFlow<AppCurrency> = _currency.asStateFlow()

    init {
        // Загружаем сохраненную валюту при старте
        viewModelScope.launch {
            context.transactionsDataStore.data.map { prefs ->
                val code = prefs[CURRENCY_KEY] ?: AppCurrency.EUR.code
                AppCurrency.values().find { it.code == code } ?: AppCurrency.EUR
            }.collect { savedCurrency ->
                _currency.value = savedCurrency
            }
        }

        // ... остальные подписки (init блок из прошлого кода) ...
        viewModelScope.launch {
            getTransactionsFlow(context).collect { _transactions.value = it }
        }
        viewModelScope.launch {
            getReportsFlow(context).collect { _reports.value = it }
        }
        viewModelScope.launch {
            getGoalsFlow(context).collect { _goals.value = it }
        }
    }

    fun updateCurrency(newCurrency: AppCurrency) {
        viewModelScope.launch(Dispatchers.IO) {
            context.transactionsDataStore.edit { prefs ->
                prefs[CURRENCY_KEY] = newCurrency.code
            }
        }
    }

    // --- 1. УПРАВЛЕНИЕ ОБЩИМ БАЛАНСОМ ---

    private val BALANCE_KEY = longPreferencesKey("main_balance_cents")

    val balanceCentsFlow = context.transactionsDataStore.data
        .map { prefs -> prefs[BALANCE_KEY] ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private fun addToBalance(amountCents: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            context.transactionsDataStore.edit { prefs ->
                val current = prefs[BALANCE_KEY] ?: 0L
                prefs[BALANCE_KEY] = current + amountCents
            }
        }
    }

    // --- 2. УПРАВЛЕНИЕ ТРАНЗАКЦИЯМИ ---

    private val _transactions = MutableStateFlow<List<TransactionItem>>(emptyList())
    val transactionsFlow: StateFlow<List<TransactionItem>> = _transactions.asStateFlow()

    // --- 3. РАСЧЕТ АГРЕГАТОВ ---

    val totalIncomeAllTime: Double
        get() = transactionsFlow.value
            .filter { it.isIncome }
            .sumOf { it.amount.absoluteValue }

    val totalExpensesAllTime: Double
        get() = transactionsFlow.value
            .filter { !it.isIncome }
            .sumOf { it.amount.absoluteValue }

    val balanceAllTime: Double
        get() = balanceCentsFlow.value / 100.0

    // --- 4. УПРАВЛЕНИЕ ОТЧЕТАМИ ---
    private val _reports = MutableStateFlow<List<ReportItem>>(emptyList())
    val reportsFlow: StateFlow<List<ReportItem>> = _reports.asStateFlow()

    fun setReportLimit(category: String, limit: Double, period: String) {
        // ВАЖНО: Лимит приходит в выбранной валюте, конвертируем в Евро для хранения
        val rate = _currency.value.rateToEuro
        val limitInEuro = limit / rate

        viewModelScope.launch(Dispatchers.IO) {
            val reportItem = ReportItem(category, limitInEuro, period)
            saveReportItem(context, reportItem)
        }
    }

    // --- 5. УПРАВЛЕНИЕ ЦЕЛЯМИ ---
    private val _goals = MutableStateFlow<List<GoalItem>>(emptyList())
    val goalsFlow: StateFlow<List<GoalItem>> = _goals.asStateFlow()

    fun addGoal(goal: GoalItem) {
        // Конвертируем цель в Евро перед сохранением
        val rate = _currency.value.rateToEuro
        val goalInEuro = goal.copy(
            targetAmount = goal.targetAmount / rate,
            savedAmount = goal.savedAmount / rate
        )

        viewModelScope.launch(Dispatchers.IO) {
            saveGoal(context, goalInEuro)
        }
    }

    fun addTransaction(title: String, category: String, amount: Double, isIncome: Boolean, iconRes: Int? = null) {
        // Конвертируем сумму транзакции в Евро перед сохранением
        val rate = _currency.value.rateToEuro
        val amountInEuro = amount / rate

        viewModelScope.launch(Dispatchers.IO) {
            val newTransaction = TransactionItem(
                name = title,
                category = category,
                amount = amountInEuro,
                isIncome = isIncome,
                iconRes = iconRes
            )
            saveTransaction(context, newTransaction)
            val amountCents = (newTransaction.amount * 100).toLong() * (if (newTransaction.isIncome) 1 else -1)
            addToBalance(amountCents)
        }
    }

    fun updateGoalAmount(goalId: Long, amount: Double) {
        // amount приходит в выбранной валюте, конвертируем в Евро
        val rate = _currency.value.rateToEuro
        val amountInEuro = amount / rate

        viewModelScope.launch(Dispatchers.IO) {
            val currentGoal = _goals.value.find { it.id == goalId }
            if (currentGoal != null) {
                val newSavedAmount = (currentGoal.savedAmount + amountInEuro).coerceAtLeast(0.0)
                val updatedGoal = currentGoal.copy(savedAmount = newSavedAmount)
                updateGoal(context, updatedGoal)

                val isIncome = amountInEuro > 0
                val transactionName = if (isIncome) "Пополнение счета: ${currentGoal.name}" else "Снятие со счета: ${currentGoal.name}"
                val newTransaction = TransactionItem(
                    name = transactionName,
                    category = "Сбережения/Цели",
                    amount = amountInEuro.absoluteValue,
                    isIncome = !isIncome,
                    iconRes = currentGoal.iconRes
                )
                saveTransaction(context, newTransaction)
                val amountCents = (amountInEuro.absoluteValue * 100).toLong() * (if (isIncome) -1 else 1)
                addToBalance(amountCents)
            }
        }
    }
}