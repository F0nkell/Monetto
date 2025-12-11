package com.example.monetto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.monetto.ui.theme.Coral
import com.example.monetto.ui.theme.colorOliveLight
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import android.graphics.Color as AndroidColor

// Константы цветов
private val BackgroundColor = Color(0xFF2E3033)
private val CardBackground = Color(0xFF3A3B3E)
private val DividerColor = Color(0xFF545659)
private val ExpenseColor = Color(0xFFF56C6F)

val GoalColors = mapOf(
    "Blue" to Color(AndroidColor.parseColor("#5F9AE1")),
    "Green" to Color(AndroidColor.parseColor("#6DD18B")),
    "Red" to Color(AndroidColor.parseColor("#FD7778")),
    "Purple" to Color(AndroidColor.parseColor("#9279D9"))
)
val GoalIcons = mapOf(
    "Car" to R.drawable.car,
    "House" to R.drawable.home,
    "Vacation" to R.drawable.plane,
    "Computer" to R.drawable.laptop,
    "Ring" to R.drawable.ring,
    "Savings" to R.drawable.coins
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            // Создаем ViewModel здесь, чтобы передавать её состояние валюты во все экраны
            val viewModel: TransactionViewModel = viewModel()

            Scaffold(
                containerColor = BackgroundColor,
                bottomBar = { Footer(navController = navController) }
            ) { innerPadding ->

                NavHost(
                    navController = navController,
                    startDestination = "HomePage",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable("HomePage") { AppHomePage(navController = navController, viewModel = viewModel) }
                    composable("TransactionsPage") { AppTransactionsPage(navController = navController, viewModel = viewModel) }
                    composable("ReportsPage") { AppReportsPage(navController = navController, viewModel = viewModel) }
                    composable("BudgetPage") { AppBudgetPage(navController = navController, viewModel = viewModel) }
                    composable(
                        route = "GoalDetailPage/{goalId}",
                        arguments = listOf(androidx.navigation.navArgument("goalId") { type = androidx.navigation.NavType.LongType })
                    ) { backStackEntry ->
                        val goalId = backStackEntry.arguments?.getLong("goalId") ?: 0L
                        GoalDetailPage(navController = navController, goalId = goalId, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// --- НОВЫЙ КОМПОНЕНТ: Выбор валюты ---
@Composable
fun CurrencySelector(viewModel: TransactionViewModel) {
    val currentCurrency by viewModel.currencyFlow.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = currentCurrency.code, color = colorOliveLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = colorOliveLight)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardBackground)
        ) {
            AppCurrency.values().forEach { currency ->
                DropdownMenuItem(
                    text = { Text("${currency.code} (${currency.symbol})", color = Color.White) },
                    onClick = {
                        viewModel.updateCurrency(currency)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AppHomePage(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel()
) {
    val transactions by viewModel.transactionsFlow.collectAsState()
    val currency by viewModel.currencyFlow.collectAsState()

    // ОПТИМИЗАЦИЯ: derivedStateOf предотвращает лишние пересчеты
    val totalIncomeNet by remember(transactions) {
        derivedStateOf { transactions.filter { it.isIncome }.sumOf { it.amount.absoluteValue } }
    }
    val totalExpensesNet by remember(transactions) {
        derivedStateOf { transactions.filter { !it.isIncome }.sumOf { it.amount.absoluteValue } }
    }

    val totalIncomeAllTime = viewModel.totalIncomeAllTime
    val totalExpensesAllTime = viewModel.totalExpensesAllTime
    val balanceAllTime = viewModel.balanceAllTime

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        HomePageHeader(viewModel) // Передаем ViewModel для валюты
        Spacer(modifier = Modifier.height(100.dp))

        // Конвертация баланса
        BalanceText(balance = balanceAllTime * currency.rateToEuro, currencySymbol = currency.symbol)

        NetBalanceCard(
            income = totalIncomeNet * currency.rateToEuro,
            expenses = totalExpensesNet * currency.rateToEuro,
            currencySymbol = currency.symbol
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IncomeCard(
                modifier = Modifier.weight(1f),
                totalIncome = totalIncomeAllTime * currency.rateToEuro,
                currencySymbol = currency.symbol
            )
            ExpensesCard(
                modifier = Modifier.weight(1f),
                totalExpenses = totalExpensesAllTime * currency.rateToEuro,
                currencySymbol = currency.symbol
            )
        }
        LastTransactionsList(transactions = transactions, count = 2, currency = currency)
    }
}

//==========================================HomePage==========================================

@Composable
fun HomePageHeader(viewModel: TransactionViewModel = viewModel()) {
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monetto",
                    color = Color.White,
                    fontSize = 40.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencySelector(viewModel) // Вставляем выбор валюты
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { /* действие */ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.profile),
                            contentDescription = "Профиль",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun BalanceText(balance: Double, currencySymbol: String = "€") {
    val formatted = remember(balance) {
        java.text.DecimalFormat("#,##0.00").format(balance)
    }

    Text(
        text = "$currencySymbol$formatted",
        fontSize = 45.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}
@Composable
fun IncomeCard(modifier: Modifier = Modifier, totalIncome: Double, currencySymbol: String) {
    Box(
        modifier = modifier
            .border(2.dp, colorOliveLight, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Income", color = colorOliveLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("+$currencySymbol${"%,.2f".format(totalIncome)}", color = colorOliveLight, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ExpensesCard(modifier: Modifier = Modifier, totalExpenses: Double, currencySymbol: String) {
    Box(
        modifier = modifier
            .border(2.dp, ExpenseColor, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Expenses", color = ExpenseColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("-$currencySymbol${"%,.2f".format(totalExpenses)}", color = ExpenseColor, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun NetBalanceCard(income: Double, expenses: Double, currencySymbol: String) {
    val net = income - expenses
    val isPositive = net >= 0
    val displayColor = if (isPositive) colorOliveLight else ExpenseColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text("Net balance: ", color = displayColor, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text("$currencySymbol${"%,.2f".format(net)}", color = displayColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isPositive) "▲" else "▼", color = displayColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LastTransactionsList(
    transactions: List<TransactionItem>,
    count: Int = 2,
    currency: AppCurrency
) {
    if (transactions.isEmpty()) {
        Text("No recent transactions", color = Color.Gray, modifier = Modifier.padding(top = 20.dp, start = 20.dp))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Recent Transactions", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        transactions.take(count).forEach { t ->
            TransactionRow(transaction = t, currency = currency)
        }
    }
}

@Composable
fun Footer(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .padding(start = 20.dp, end = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Divider(color = DividerColor, modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FooterItem(navController, "HomePage", R.drawable.homeicon, 60.dp)
                FooterItem(navController, "TransactionsPage", R.drawable.transactionsicon, 100.dp)
                FooterItem(navController, "ReportsPage", R.drawable.reportsicon, 62.dp)
                FooterItem(navController, "BudgetPage", R.drawable.budgeticon, 57.dp)
            }
        }
    }
}


//==========================================TransactionsPage==========================================
@Composable
fun AppTransactionsPage(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel()
) {
    val transactions by viewModel.transactionsFlow.collectAsState()
    val currency by viewModel.currencyFlow.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Month") }
    val displayDateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    val filteredTransactions = remember(transactions, selectedFilter) {
        val now = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val startDate = when (selectedFilter) {
            "Week" -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            "Month" -> now.with(TemporalAdjusters.firstDayOfMonth())
            "Year" -> now.with(TemporalAdjusters.firstDayOfYear())
            else -> now
        }
        val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = now.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        transactions.filter { t -> t.date in startMillis until endMillis }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(top = 20.dp, start = 20.dp, end = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TransactionsPageHeader(viewModel)
            Spacer(modifier = Modifier.height(20.dp))

            FilterDropdown(
                currentFilter = selectedFilter,
                onFilterSelected = { newFilter -> selectedFilter = newFilter }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет транзакций за выбранный период", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredTransactions, key = { it.id }) { t ->
                        Column {
                            TransactionRow(transaction = t, currency = currency)
                            Text(
                                text = displayDateFormat.format(Date(t.date)),
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colorOliveLight
            ) {
                Text("+", color = Color.White, fontSize = 24.sp)
            }
        }

        if (showAddDialog) {
            AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, category, amount, isIncome ->
                    viewModel.addTransaction(name, category, amount, isIncome)
                    showAddDialog = false
                },
                currencySymbol = currency.symbol
            )
        }
    }
}

@Composable
fun FilterDropdown(
    currentFilter: String,
    onFilterSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val filters = listOf("Week", "Month", "Year")

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("This $currentFilter", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text("▼", color = Color.White)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DividerColor)
        ) {
            filters.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter, color = if (filter == currentFilter) colorOliveLight else Color.White) },
                    onClick = { onFilterSelected(filter); expanded = false }
                )
            }
        }
    }
}

@Composable
fun TransactionRow(transaction: TransactionItem, currency: AppCurrency = AppCurrency.EUR) {
    val color = if (transaction.isIncome) colorOliveLight else ExpenseColor

    val iconRes = when (transaction.category) {
        "Supermarket" -> R.drawable.groserries
        "Medicine" -> R.drawable.medicine
        "Flowers" -> R.drawable.flowers
        "Fast food" -> R.drawable.fastfood
        "Clothes" -> R.drawable.clothes
        "Transport" -> R.drawable.transport
        "Taxi" -> R.drawable.taxi
        "Rent" -> R.drawable.rent
        "Finance" -> R.drawable.finance
        "Сбережения/Цели" -> transaction.iconRes ?: R.drawable.coins
        else -> null
    }

    // Конвертация
    val displayAmount = transaction.amount * currency.rateToEuro

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = transaction.category,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            // ОПТИМИЗАЦИЯ: weight(1f) для корректного переноса текста
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = transaction.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = transaction.category,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Text(
                text = (if (transaction.isIncome) "+" else "-") + "${currency.symbol}${"%,.2f".format(displayAmount)}",
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun TransactionsPageHeader(viewModel: TransactionViewModel = viewModel()) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions",
                color = Color.White,
                fontSize = 40.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                CurrencySelector(viewModel)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { /* действие по профилю */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.profile),
                        contentDescription = "Профиль",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionsPageMain() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp)
    ) {
    }
}

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Boolean) -> Unit,
    currencySymbol: String = "€"
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    val categories = listOf("Supermarket", "Medicine", "Flowers", "Fast food", "Clothes", "Transport", "Taxi", "Rent", "Finance")
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    val isCategorySelectionEnabled = !isIncome

    LaunchedEffect(isIncome) { if (isIncome) selectedCategory = "Finance" }

    val arrowSymbol = if (expanded) "▲" else "▼"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundColor,
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && amt > 0.0) {
                    onAdd(name, selectedCategory, amt, isIncome)
                    onDismiss()
                }
            }) { Text("Add", color = colorOliveLight) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) } },
        title = { Text("Add Transaction", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name", color = Color.Gray) },
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = selectedCategory, onValueChange = {}, label = { Text("Category", color = Color.Gray) }, readOnly = true,
                        trailingIcon = { Text(arrowSymbol, color = Color.White, modifier = Modifier.padding(end = 8.dp)) },
                        enabled = isCategorySelectionEnabled, textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth().clickable(enabled = isCategorySelectionEnabled) { expanded = !expanded },
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.White, disabledContainerColor = BackgroundColor, disabledBorderColor = Color.Gray)
                    )
                    if (isCategorySelectionEnabled) {
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(BackgroundColor)) {
                            categories.filter { it != "Finance" }.forEach { category ->
                                DropdownMenuItem(text = { Text(category, color = Color.White) }, onClick = { selectedCategory = category; expanded = false })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' || char == ',' } },
                    label = { Text("Amount ($currencySymbol)", color = Color.Gray) },
                    textStyle = TextStyle(color = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Checkbox(checked = isIncome, onCheckedChange = { isIncome = it }, colors = CheckboxDefaults.colors(checkedColor = colorOliveLight))
                    Text("Income", color = Color.White)
                }
            }
        }
    )
}

// ========================================== REPORTS PAGE ==========================================

@Composable
fun AppReportsPage(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val currency by viewModel.currencyFlow.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(BackgroundColor)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderReportsPage(viewModel)
            Box(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
                MainReportsPage(viewModel, currency)
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = colorOliveLight) {
                Text("+", color = Color.White, fontSize = 24.sp)
            }
        }
        if (showAddDialog) {
            AddReportLimitDialog(
                onDismiss = { showAddDialog = false },
                onSetLimit = { category, limit, period ->
                    viewModel.setReportLimit(category, limit, period)
                    showAddDialog = false
                },
                currencySymbol = currency.symbol
            )
        }
    }
}

@Composable
fun HeaderReportsPage(viewModel: TransactionViewModel = viewModel()) {
    Box {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reports",
                    color = Color.White,
                    fontSize = 40.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CurrencySelector(viewModel)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { /* действие */ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.profile),
                            contentDescription = "Профиль",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainReportsPage(
    viewModel: TransactionViewModel,
    currency: AppCurrency
) {
    val reports by viewModel.reportsFlow.collectAsState()
    val allTransactions by viewModel.transactionsFlow.collectAsState()
    val selectedFilter = "Month"

    // ОПТИМИЗАЦИЯ: Явно указываем типы, чтобы компилятор не путался
    val spentByCategoryMap: Map<String, Double> = remember(allTransactions, reports, selectedFilter) {
        // 1. Сначала фильтруем
        val filtered = filterTransactionsByPeriod(allTransactions, selectedFilter)

        // 2. Потом считаем
        filtered
            .filter { !it.isIncome } // Оставляем только расходы
            .groupBy { it.category } // Группируем по категориям
            .mapValues { entry ->    // Считаем сумму для каждой категории
                entry.value.sumOf { it.amount }
            }
    }

    val overspentItems = remember(reports, spentByCategoryMap, currency) {
        reports.filter { report ->
            // Сравниваем потраченное (в Евро) с лимитом (в Евро)
            val spent = spentByCategoryMap[report.category] ?: 0.0
            spent > report.limitAmount
        }.map { report ->
            val spent = spentByCategoryMap[report.category] ?: 0.0
            // Считаем разницу в Евро
            val diffEuro = spent - report.limitAmount
            // Конвертируем разницу в выбранную валюту для отображения
            val displayDiff = diffEuro * currency.rateToEuro

            Triple(report.category, displayDiff, getCategoryIcon(report.category))
        }
    }

    if (reports.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No limits set yet.", color = Color.Gray, fontSize = 16.sp)
            Text("Tap '+' to set a spending limit.", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reports) { report ->
                // Конвертируем суммы в Евро в выбранную валюту для отображения
                val spentInEuro = spentByCategoryMap[report.category] ?: 0.0

                val spentDisplay = spentInEuro * currency.rateToEuro
                val limitDisplay = report.limitAmount * currency.rateToEuro

                ReportLimitCard(
                    report = report,
                    spentAmount = spentDisplay,
                    limitDisplay = limitDisplay,
                    currencySymbol = currency.symbol
                )
            }

            if (overspentItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.Gray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Categories over Budget",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(overspentItems) { (category, amount, iconRes) ->
                    OverspentRow(
                        category = category,
                        amount = amount,
                        iconRes = iconRes,
                        currencySymbol = currency.symbol
                    )
                }
            }
        }
    }
}
@Composable
fun AddReportLimitDialog(
    onDismiss: () -> Unit,
    onSetLimit: (String, Double, String) -> Unit,
    currencySymbol: String
) {
    var amount by remember { mutableStateOf("") }
    val limitCategories = listOf("Supermarket", "Medicine", "Flowers", "Fast food", "Clothes", "Transport", "Taxi", "Rent")
    val periods = listOf("Month", "Week", "Year")

    var expandedCategory by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(limitCategories.first()) }

    var expandedPeriod by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf(periods.first()) }

    val arrowSymbolCategory = if (expandedCategory) "▲" else "▼"
    val arrowSymbolPeriod = if (expandedPeriod) "▲" else "▼"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                if (amt > 0.0) {
                    onSetLimit(selectedCategory, amt, selectedPeriod)
                    onDismiss()
                }
            }) { Text("Set Limit", color = colorOliveLight) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        },
        title = { Text("Set Spending Limit", color = Color.White) },
        text = {
            Column {
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = selectedCategory, onValueChange = {},
                        label = { Text("Category", color = Color.Gray) },
                        readOnly = true,
                        trailingIcon = {
                            Text(
                                text = arrowSymbolCategory,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { expandedCategory = !expandedCategory }
                                    .padding(end = 8.dp)
                            )
                        },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedCategory = !expandedCategory }
                    )

                    DropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false },
                        modifier = Modifier.background(BackgroundColor)
                    ) {
                        limitCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category, color = Color.White) },
                                onClick = {
                                    selectedCategory = category
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' || char == ',' } },
                    label = { Text("Limit Amount ($currencySymbol)", color = Color.Gray) },
                    textStyle = TextStyle(color = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Box(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = selectedPeriod, onValueChange = {},
                        label = { Text("Period", color = Color.Gray) },
                        readOnly = true,
                        trailingIcon = {
                            Text(
                                text = arrowSymbolPeriod,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { expandedPeriod = !expandedPeriod }
                                    .padding(end = 8.dp)
                            )
                        },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedPeriod = !expandedPeriod }
                    )

                    DropdownMenu(
                        expanded = expandedPeriod,
                        onDismissRequest = { expandedPeriod = false },
                        modifier = Modifier.background(BackgroundColor)
                    ) {
                        periods.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period, color = Color.White) },
                                onClick = {
                                    selectedPeriod = period
                                    expandedPeriod = false
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = BackgroundColor
    )
}

@Composable
fun ReportLimitCard(report: ReportItem, spentAmount: Double, limitDisplay: Double, currencySymbol: String) {
    val overspent = max(0.0, spentAmount - limitDisplay)
    val isOverspent = spentAmount > limitDisplay
    val progress = if (limitDisplay > 0) (spentAmount / limitDisplay).toFloat() else 0f
    val spentTextColor = if (isOverspent) Coral else if (spentAmount > 0) colorOliveLight else Color.White

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4D4F52)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(report.category, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Limit: $currencySymbol${"%.2f".format(limitDisplay)} / ${report.period}", fontSize = 14.sp, color = Color.LightGray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            ReportProgressBar(spent = spentAmount, limit = limitDisplay)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Spent: $currencySymbol${"%.2f".format(spentAmount)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = spentTextColor)
                if (isOverspent) {
                    Text("Overspent: $currencySymbol${"%.2f".format(overspent)}", fontSize = 14.sp, color = Coral)
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
        }
    }
}

@Composable
fun OverspentRow(category: String, amount: Double, iconRes: Int?, currencySymbol: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Icon(painterResource(id = iconRes), contentDescription = category, modifier = Modifier.size(32.dp), tint = Coral)
            } else {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Coral))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Overspent by: $currencySymbol${"%.2f".format(amount)}", color = Coral, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color(0xFF4A4B4D), thickness = 1.dp)
    }
}

// ========================================== BUDGET PAGE ==========================================

@Composable
fun AppBudgetPage(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val goals by viewModel.goalsFlow.collectAsState(initial = emptyList())
    val currency by viewModel.currencyFlow.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(BackgroundColor).padding(horizontal = 20.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderBudgetPage(viewModel)
            if (goals.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("No goals set yet.", color = Color.Gray, fontSize = 16.sp)
                    Text("Tap '+' to create a savings goal.", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(goals, key = { it.id }) { goal ->
                        GoalCard(goal = goal, currency = currency, onClick = { navController.navigate("GoalDetailPage/${goal.id}") })
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color(0xFF67D38B),
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 20.dp)
        ) {
            Text("+", color = Color.White, fontSize = 24.sp)
        }
        if (showAddDialog) {
            AddGoalDialog(
                onDismiss = { showAddDialog = false },
                onAddGoal = { goal -> viewModel.addGoal(goal); showAddDialog = false },
                currencySymbol = currency.symbol
            )
        }
    }
}

@Composable
fun HeaderBudgetPage(viewModel: TransactionViewModel = viewModel()) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 44.dp, start = 0.dp, end = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Savings & Goals",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                CurrencySelector(viewModel)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { /* действие */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.profile),
                        contentDescription = "Профиль",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun GoalCard(goal: GoalItem, currency: AppCurrency, onClick: () -> Unit) {
    val targetColor = Color(android.graphics.Color.parseColor(goal.colorHex))
    val progress = (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)

    // Конвертация
    val savedDisplay = goal.savedAmount * currency.rateToEuro
    val targetDisplay = goal.targetAmount * currency.rateToEuro

    val daysInfo = remember(goal.deadline, goal.id) {
        val start = Instant.ofEpochMilli(goal.id)
        val end = Instant.ofEpochMilli(goal.deadline)
        val now = Instant.now()
        val totalDays = Duration.between(start, end).toDays().coerceAtLeast(1)
        val elapsedDays = Duration.between(start, now).toDays().coerceAtLeast(0)
        Pair(totalDays, elapsedDays)
    }

    val requiredPace = targetDisplay / daysInfo.first
    val shouldHaveSaved = requiredPace * daysInfo.second
    val deviation = savedDisplay - shouldHaveSaved

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4D4F52))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(painterResource(id = goal.iconRes), contentDescription = goal.name, modifier = Modifier.size(32.dp), tint = targetColor)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Deadline: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(goal.deadline))}", color = Color.LightGray, fontSize = 12.sp)
                }
                Text("${currency.symbol}${"%,.2f".format(deviation)}", color = if (deviation >= 0) Color(0xFF67D38B) else Color(0xFFF44336), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = targetColor,
                trackColor = Color(0xAA2E3033),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("${currency.symbol}${"%,.2f".format(savedDisplay)} / ${currency.symbol}${"%,.2f".format(targetDisplay)}", color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.align(Alignment.End))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onAddGoal: (GoalItem) -> Unit,
    currencySymbol: String
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var periodValue by remember { mutableStateOf("") }
    val periodUnits = listOf("Days", "Weeks", "Months", "Years")
    var selectedUnit by remember { mutableStateOf("Months") }
    var expandedUnit by remember { mutableStateOf(false) }
    var selectedColorName by remember { mutableStateOf(GoalColors.keys.first()) }
    var expandedColor by remember { mutableStateOf(false) }
    val selectedColor = GoalColors[selectedColorName] ?: Color.Gray
    var selectedIconName by remember { mutableStateOf(GoalIcons.keys.first()) }
    var expandedIcon by remember { mutableStateOf(false) }
    val selectedIconRes = GoalIcons[selectedIconName] ?: android.R.drawable.ic_menu_help
    var isError by remember { mutableStateOf(false) }

    val deadlineDate = remember(periodValue, selectedUnit) {
        val calendar = Calendar.getInstance()
        val value = periodValue.toIntOrNull() ?: 1
        val field = when (selectedUnit) {
            "Days" -> Calendar.DAY_OF_YEAR
            "Weeks" -> Calendar.WEEK_OF_YEAR
            "Months" -> Calendar.MONTH
            "Years" -> Calendar.YEAR
            else -> Calendar.MONTH
        }
        calendar.add(field, value)
        calendar.timeInMillis
    }
    val formattedDeadline = remember(deadlineDate) { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(deadlineDate)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundColor,
        title = { Text("New Savings Goal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; isError = false },
                    label = { Text("Goal Name", color = Color.LightGray) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = selectedColor, unfocusedBorderColor = Color.Gray, focusedLabelColor = selectedColor, cursorColor = selectedColor)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount, onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) { amount = it; isError = false } },
                    label = { Text("Target Amount ($currencySymbol)", color = Color.LightGray) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = selectedColor, unfocusedBorderColor = Color.Gray, focusedLabelColor = selectedColor, cursorColor = selectedColor)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = periodValue, onValueChange = { if (it.all { char -> char.isDigit() }) periodValue = it },
                        label = { Text("Duration", color = Color.LightGray) },
                        placeholder = { Text("1", color = Color.Gray) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = selectedColor, unfocusedBorderColor = Color.Gray, focusedLabelColor = selectedColor, cursorColor = selectedColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedUnit, onValueChange = {}, readOnly = true,
                            label = { Text("Unit", color = Color.LightGray) },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, tint = Color.White) },
                            modifier = Modifier.fillMaxWidth().clickable { expandedUnit = true },
                            enabled = false, textStyle = TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.White, disabledBorderColor = Color.Gray, disabledLabelColor = Color.LightGray)
                        )
                        DropdownMenu(expanded = expandedUnit, onDismissRequest = { expandedUnit = false }, modifier = Modifier.background(Color(0xFF424242))) {
                            periodUnits.forEach { unit -> DropdownMenuItem(text = { Text(unit, color = Color.White) }, onClick = { selectedUnit = unit; expandedUnit = false }) }
                        }
                    }
                }
                Text("Deadline: $formattedDeadline", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, start = 4.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedColorName, onValueChange = {}, readOnly = true,
                            label = { Text("Color", color = Color.LightGray) },
                            leadingIcon = { Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(selectedColor).border(1.dp, Color.White, CircleShape)) },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, tint = Color.White) },
                            modifier = Modifier.fillMaxWidth().clickable { expandedColor = true },
                            enabled = false, textStyle = TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.White, disabledBorderColor = Color.Gray, disabledLabelColor = Color.LightGray, disabledLeadingIconColor = Color.Unspecified)
                        )
                        DropdownMenu(expanded = expandedColor, onDismissRequest = { expandedColor = false }, modifier = Modifier.background(Color(0xFF424242))) {
                            GoalColors.forEach { (name, color) -> DropdownMenuItem(text = { Text(name, color = Color.White) }, leadingIcon = { Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(color)) }, onClick = { selectedColorName = name; expandedColor = false }) }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedIconName, onValueChange = {}, readOnly = true,
                            label = { Text("Icon", color = Color.LightGray) },
                            leadingIcon = { Icon(painterResource(id = selectedIconRes), null, modifier = Modifier.size(24.dp), tint = selectedColor) },
                            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, tint = Color.White) },
                            modifier = Modifier.fillMaxWidth().clickable { expandedIcon = true },
                            enabled = false, textStyle = TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.White, disabledBorderColor = Color.Gray, disabledLabelColor = Color.LightGray, disabledLeadingIconColor = selectedColor)
                        )
                        DropdownMenu(expanded = expandedIcon, onDismissRequest = { expandedIcon = false }, modifier = Modifier.background(Color(0xFF424242))) {
                            GoalIcons.forEach { (name, resId) -> DropdownMenuItem(text = { Text(name, color = Color.White) }, leadingIcon = { Icon(painterResource(id = resId), null, tint = Color.White, modifier = Modifier.size(24.dp)) }, onClick = { selectedIconName = name; expandedIcon = false }) }
                        }
                    }
                }
                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Please enter a valid name and amount", color = Color(0xFFF44336), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validAmount = amount.toDoubleOrNull()
                    if (name.isBlank() || validAmount == null || validAmount <= 0) {
                        isError = true
                    } else {
                        val hexColor = "#" + Integer.toHexString(selectedColor.toArgb()).uppercase()
                        val newGoal = GoalItem(name = name, targetAmount = validAmount, deadline = deadlineDate, colorHex = hexColor, iconRes = selectedIconRes, periodUnit = selectedUnit)
                        onAddGoal(newGoal)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create", color = Color.White, fontSize = 16.sp) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancel") }
        }
    )
}
@Composable
fun GoalDetailPage(
    navController: NavController,
    goalId: Long,
    viewModel: TransactionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val goals by viewModel.goalsFlow.collectAsState()
    val goal = goals.find { it.id == goalId }
    val currency by viewModel.currencyFlow.collectAsState()
    var amountInput by remember { mutableStateOf("") }

    if (goal == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2E3033)), contentAlignment = Alignment.Center) {
            Text("Goal not found", color = Color.Gray)
        }
        return
    }

    val targetColor = Color(android.graphics.Color.parseColor(goal.colorHex))
    val progress = (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)

    // Конвертация
    val savedDisplay = goal.savedAmount * currency.rateToEuro
    val targetDisplay = goal.targetAmount * currency.rateToEuro

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF2E3033)).padding(20.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("◄ Back", color = Color.Gray, modifier = Modifier.clickable { navController.popBackStack() }, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(40.dp))
        Icon(painter = painterResource(id = goal.iconRes), contentDescription = null, modifier = Modifier.size(80.dp), tint = targetColor)
        Spacer(modifier = Modifier.height(16.dp))
        Text(goal.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBackground), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saved", color = Color.Gray)
                    Text("${(progress * 100).toInt()}%", color = targetColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = targetColor,
                    trackColor = BackgroundColor,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("${currency.symbol}${"%,.2f".format(savedDisplay)}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("/ ${currency.symbol}${"%,.0f".format(targetDisplay)}", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Text("Manage Funds", color = Color.Gray, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = amountInput,
            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amountInput = it },
            label = { Text("Amount (${currency.symbol})", color = Color.Gray) },
            textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = targetColor, unfocusedBorderColor = Color.Gray, cursorColor = targetColor)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        viewModel.updateGoalAmount(goalId, -amount)
                        amountInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Icon(painterResource(id = R.drawable.transactionsicon), contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Withdraw", fontSize = 16.sp)
            }
            Button(
                onClick = {
                    val amount = amountInput.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        viewModel.updateGoalAmount(goalId, amount)
                        amountInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorOliveLight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Icon(painterResource(id = R.drawable.budgeticon), contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Deposit", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun FooterItem(navController: NavController, route: String, iconRes: Int, width: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(width, 66.dp)
            .clickable { navController.navigate(route) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = route,
            modifier = Modifier.fillMaxSize(),
            tint = Color.White
        )
    }
}

// --- ЭТУ ФУНКЦИЮ НУЖНО ВСТАВИТЬ В КОНЕЦ ФАЙЛА MainActivity.kt ---

fun filterTransactionsByPeriod(transactions: List<TransactionItem>, period: String): List<TransactionItem> {
    val now = LocalDate.now()
    val zoneId = ZoneId.systemDefault()

    val startDate = when (period) {
        "Week" -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        "Month" -> now.with(TemporalAdjusters.firstDayOfMonth())
        "Year" -> now.with(TemporalAdjusters.firstDayOfYear())
        else -> now
    }

    // Начало периода (00:00)
    val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

    // Конец периода (для фильтрации "текущий месяц" берем до конца текущего дня или до конца месяца - логика "все транзакции ПОСЛЕ начала")
    // В данном случае мы просто берем все транзакции, дата которых больше или равна startMillis

    return transactions.filter { it.date >= startMillis }
}

// --- ВСТАВИТЬ В КОНЕЦ MainActivity.kt ---

fun getCategoryIcon(category: String): Int? {
    return when (category) {
        "Supermarket" -> R.drawable.groserries
        "Medicine" -> R.drawable.medicine
        "Flowers" -> R.drawable.flowers
        "Fast food" -> R.drawable.fastfood
        "Clothes" -> R.drawable.clothes
        "Transport" -> R.drawable.transport
        "Taxi" -> R.drawable.taxi
        "Rent" -> R.drawable.rent
        "Finance" -> R.drawable.finance
        else -> null
    }
}
@Composable
fun ReportProgressBar(spent: Double, limit: Double) {
    val overspent = max(0.0, spent - limit)
    val isOverspent = spent > limit

    // Вычисляем веса для отображения (зеленая и красная части)
    val totalProgressBase = if (isOverspent) spent else limit
    val limitWeight = if (limit > 0) min(1f, (limit / totalProgressBase).toFloat()) else 0f
    val overspentWeight = if (overspent > 0) (overspent / totalProgressBase).toFloat() else 0f

    // Прогресс для обычного случая (без перерасхода)
    val progress = if (limit > 0) (spent / limit).toFloat() else 0f

    if (!isOverspent && limit > 0) {
        // Случай 1: Траты в пределах лимита
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = colorOliveLight,
            trackColor = Color(0x884D4F52)
        )
    } else if (isOverspent) {
        // Случай 2: Перерасход (составной бар)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x884D4F52))
        ) {
            Box(
                modifier = Modifier
                    .weight(limitWeight)
                    .fillMaxHeight()
                    .background(colorOliveLight)
            )
            Box(
                modifier = Modifier
                    .weight(overspentWeight)
                    .fillMaxHeight()
                    .background(Coral)
            )
        }
    } else {
        // Случай 3: Пустой (лимит 0 или трат нет)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x884D4F52))
        )
    }
}