package com.example.monetto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import com.example.monetto.ui.theme.Coral
import com.example.monetto.ui.theme.colorOliveLight
import kotlin.math.absoluteValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import kotlin.math.max // Нужен для логики ProgressBar
import kotlin.math.min // Нужен для логики ProgressBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CardDefaults
import java.util.concurrent.TimeUnit
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit.*
import java.time.Duration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState  // <-- Добавить
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.filled.Circle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.ArrowDropDown
import android.graphics.Color as AndroidColor


val GoalColors = mapOf(
    // 1. Используем AndroidColor.parseColor() для преобразования строки в Int.
    // 2. Используем Color() для преобразования Int в Compose Color.
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
            val navController = rememberNavController() // 1. Инициализация контроллера навигации. Он управляет стеком экранов.

            Scaffold( // 2. Главный макет Scaffold (Каркас): обеспечивает структуру с местом для контента, нижней панелью (bottomBar) и т.д.
                containerColor = Color(0xFF2E3033), // Установка темно-серого фона для всего приложения.
                bottomBar = { Footer(navController = navController) } // 3. Установка нижней навигационной панели (футера).
            ) { innerPadding ->

                NavHost( // 4. Навигационный хост: контейнер, в котором будут отображаться Composable-функции (экраны) в зависимости от текущего маршрута.
                    navController = navController,
                    startDestination = "HomePage", // Установка "HomePage" как первого, стартового экрана.
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding) // 5. Ключевой отступ: предотвращает перекрытие контента нижней панелью (Footer).
                ) {
                    // Определение маршрутов и Composable-функций для каждого экрана:
                    composable("HomePage") { AppHomePage(navController = navController) }
                    composable("TransactionsPage") { AppTransactionsPage(navController = navController) }
                    composable("ReportsPage") { AppReportsPage(navController = navController) } // Экран отчетов (пока заглушка)
                    composable("BudgetPage") { AppBudgetPage(navController =navController) } // Экран бюджета (пока заглушка)
                }
            }
        }
    }
}



@Composable
fun AppHomePage(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel() // 1. Получение экземпляра ViewModel для доступа к данным (требует определения TransactionViewModel).
) {
    // 2. Сбор данных о транзакциях в реальном времени. При изменении данных, UI автоматически обновится.
    val transactions by viewModel.transactionsFlow.collectAsState()

    // 3. Общие итоги за всё время (предполагается, что они предварительно рассчитаны в ViewModel):
    val totalIncomeAllTime = viewModel.totalIncomeAllTime
    val totalExpensesAllTime = viewModel.totalExpensesAllTime
    val balanceAllTime = viewModel.balanceAllTime

    // 4. Расчет чистого баланса (NetBalance) за текущий период
    // Используется абсолютное значение (absoluteValue), чтобы убедиться, что суммы прибавляются/вычитаются корректно.
    val totalIncomeNet = transactions.filter { it.isIncome }.sumOf { it.amount.absoluteValue }
    val totalExpensesNet = transactions.filter { !it.isIncome }.sumOf { it.amount.absoluteValue }

    Column( // Вертикальный макет для организации элементов страницы
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E3033)) // Установка темно-серого фона
    ) {
        HomePageHeader() // Компонент шапки (логотип + профиль)
        Spacer(modifier = Modifier.height(100.dp)) // Большой отступ

        // Баланс за всё время.
        BalanceText(balance = balanceAllTime) // 5. Компонент для отображения общего баланса (требует определения BalanceText).

        // Чистый баланс за текущий период (разница между Net Income и Net Expenses).
        NetBalanceCard(income = totalIncomeNet, expenses = totalExpensesNet)

        Spacer(modifier = Modifier.height(16.dp))

        // Горизонтальный ряд для карточек доходов и расходов
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp) // Расстояние между карточками
        ) {
            // Карточка доходов. weight(1f) обеспечивает равное распределение ширины между IncomeCard и ExpensesCard.
            IncomeCard(modifier = Modifier.weight(1f), totalIncome = totalIncomeAllTime)
            ExpensesCard(modifier = Modifier.weight(1f), totalExpenses = totalExpensesAllTime)
        }
        // Список последних двух транзакций.
        LastTransactionsList(transactions = transactions, count = 2)
    }
}

//==========================================HomePage==========================================

@Composable
fun HomePageHeader() {
    //Задний фон (Контейнер)
    Box(
    ) {
        // Header (Внутренний контейнер, задающий отступы)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 1. Установка отступов: сверху (top) 44.dp, по бокам (start/end) 20.dp.
                .padding(top = 44.dp, start = 20.dp, end = 20.dp)
        ) {
            Row( // 2. Горизонтальный макет для размещения элементов в одну строку
                modifier = Modifier.fillMaxWidth(),
                // 3. Распределение: 'SpaceBetween' гарантирует, что логотип и профиль будут прижаты к противоположным краям.
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monetto",
                    color = Color.White,
                    fontSize = 40.sp
                )

                IconButton( // 5. Кнопка-контейнер для иконки профиля (обеспечивает кликабельную область)
                    onClick = { /* действие */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon( // 6. Иконка профиля
                        painter = painterResource(id = R.drawable.profile), // (Требуется ресурс 'profile')
                        contentDescription = "Профиль",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape), // Обрезка изображения/иконки в круг
                        tint = Color.White
                    )
                }

            }
        }


    }
}


@Composable
fun IncomeCard(modifier: Modifier = Modifier, totalIncome: Double) {
    Box( // Контейнер для карточки
        modifier = modifier
            .border( // 1. Основной элемент дизайна: граница
                width = 2.dp,
                color = colorOliveLight, // Зеленый цвет для дохода
                shape = RoundedCornerShape(16.dp) // Скругленные углы
            )
            .padding(vertical = 16.dp), // Внутренний вертикальный отступ
        contentAlignment = Alignment.Center // Центрирование содержимого внутри Box
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { // Вертикальное расположение текста (заголовок + сумма)
            Text( // Заголовок
                text = "Income",
                color = colorOliveLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text( // Сумма дохода
                // 2. Форматирование суммы: "%+, .2f" добавляет знак '+' и разделитель тысячных (запятую),
                // а также округляет до двух знаков после запятой, добавляя символ евро.
                text = "${"+%,.2f".format(totalIncome)} €",
                color = colorOliveLight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@Composable
fun ExpensesCard(modifier: Modifier = Modifier, totalExpenses: Double) {
    Box( // Контейнер для карточки
        modifier = modifier
            .border( // 1. Красная граница
                width = 2.dp,
                color = Color(0xFFF56C6F), // Красный цвет для расхода
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text( // Заголовок
                text = "Expenses",
                color = Color(0xFFF56C6F),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text( // Сумма расхода
                // 2. Форматирование суммы: использует знак '-' в начале строки,
                // чтобы явно обозначить расход.
                text = "${"-%,.2f".format(totalExpenses)} €",
                color = Color(0xFFF56C6F),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@Composable
fun NetBalanceCard(income: Double, expenses: Double) {
    val net = income - expenses // 1. Вычисление чистого баланса
    val isPositive = net >= 0 // 2. Проверка, является ли баланс положительным или нулевым

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center // Центрирование всего содержимого
    ) {
        Row( // Горизонтальный макет для размещения текста и стрелки
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text( // 3. Метка "Net balance:"
                text = "Net balance: ",
                // Цвет зависит от знака баланса: зеленый (плюс) или красный (минус)
                color = if (isPositive) colorOliveLight else Color(0xFFF56C6F),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Text( // 4. Отображение суммы баланса
                // Форматирование: ",.2f" использует разделитель тысячных и 2 знака после запятой.
                text = "${"%,.2f".format(net)} €",
                color = if (isPositive) colorOliveLight else Color(0xFFF56C6F),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 5. Стрелка вверх/вниз для визуальной индикации
            Text(
                text = if (isPositive) "▲" else "▼",
                color = if (isPositive) colorOliveLight else Color(0xFFF56C6F),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}



@Composable
fun LastTransactionsList(
    // 1. Принимает список объектов TransactionItem (Требуется определение TransactionItem).
    transactions: List<TransactionItem>,
    count: Int = 2 // Сколько последних транзакций показывать
) {
    if (transactions.isEmpty()) { // Обработка случая, когда транзакций нет
        Text(
            text = "No recent transactions",
            color = Color.Gray,
            modifier = Modifier.padding(top = 20.dp, start = 20.dp)
        )
        return
    }

    Column( // Основной вертикальный контейнер
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // Расстояние между строками транзакций
    ) {
        // Заголовок
        Text(
            text = "Recent Transactions",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        // 2. Логика отбора: берем только первые `count` элементов из списка.
        // Это предполагает, что новые транзакции добавляются в начало списка (индекс 0).
        val recent = transactions.take(count)

        // 3. Отображение: для каждой транзакции вызывается Composable-функция TransactionRow.
        recent.forEach { t ->
            TransactionRow(transaction = t) // (Требуется определение TransactionRow).
        }
    }
}
@Composable
fun Footer(
    navController: NavController // 1. Контроллер для выполнения навигации
) {

    Box( // Основной контейнер, который задает высоту и отступы футера
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp) // Фиксированная высота футера (может быть большой для стандартной панели)
            .padding(start = 20.dp, end = 20.dp),
    ) {
        Column( // Вертикальный макет для размещения разделителя и иконок
            modifier = Modifier
                .fillMaxSize()
                .padding(top=10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween // Разделение элементов (разделитель сверху, иконки снизу)
        ) {
            Divider( // 2. Горизонтальный разделитель (тонкая линия)
                color = Color(0xFF545659), // Темно-серый цвет для линии
                modifier = Modifier.fillMaxWidth()
            )
            Row( // 3. Горизонтальный макет для иконок навигации
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly, // 4. Равномерное распределение элементов по ширине
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home (Кнопка 1)
                Box(
                    modifier = Modifier
                        .size(60.dp, 66.dp)
                        // 5. Обработчик клика: переводит на маршрут "HomePage"
                        .clickable { navController.navigate("HomePage") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.homeicon), // (Требуется ресурс 'homeicon')
                        contentDescription = "HomeIcon",
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.White
                    )
                }

                // Transactions (Кнопка 2)
                Box(
                    modifier = Modifier
                        .size(100.dp, 66.dp) // Более широкая область для нажатия
                        .clickable { navController.navigate("TransactionsPage") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.transactionsicon), // (Требуется ресурс 'transactionsicon')
                        contentDescription = "TransactionsIcon",
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.White
                    )

                }
                // Reports (Кнопка 3)
                Box(
                    modifier = Modifier
                        .size(62.dp, 66.dp)
                        .clickable { navController.navigate("ReportsPage") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.reportsicon), // (Требуется ресурс 'reportsicon')
                        contentDescription = "ReportsIcon",
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.White
                    )
                }
                // Budget (Кнопка 4)
                Box(
                    modifier = Modifier
                        .size(57.dp, 68.dp)
                        .clickable { navController.navigate("BudgetPage") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.budgeticon), // (Требуется ресурс 'budgeticon')
                        contentDescription = "BudgetIcon",
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.White
                    )
                }
            }
        }
    }
}



//==========================================TransactionsPage==========================================
@Composable
fun AppTransactionsPage(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel() // 1. Получение ViewModel для доступа к данным и методам добавления.
) {
    // 2. StateFlow: Собираем список транзакций в реальном времени.
    val transactions by viewModel.transactionsFlow.collectAsState()

    // 3. Состояние UI: Флаг для управления видимостью диалогового окна добавления.
    var showAddDialog by remember { mutableStateOf(false) }

    // 4. Состояние UI: Текущий выбранный фильтр ("Month", "Week", "Year").
    var selectedFilter by remember { mutableStateOf("Month") }

    // 5. Инструмент для форматирования даты в формате "день.месяц.год" для отображения.
    val displayDateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    // --- Логика фильтрации транзакций ---
    val filteredTransactions = remember(transactions, selectedFilter) {
        val now = Calendar.getInstance()
        val startDate = Calendar.getInstance()

        // 6. Установка начальной даты в зависимости от выбранного фильтра:
        when (selectedFilter) {
            "Week" -> {
                // Начинаем неделю с понедельника
                startDate.firstDayOfWeek = Calendar.MONDAY
                // Вычисляем, на сколько дней нужно вернуться, чтобы попасть на начало недели
                val diff = (now.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                startDate.add(Calendar.DAY_OF_MONTH, -diff)
            }
            "Month" -> {
                // Устанавливаем день месяца в 0 (это приведет к последнему дню предыдущего месяца,
                // что эффективно, но обычно лучше использовать `set(Calendar.DAY_OF_MONTH, 1)`)
                startDate.set(Calendar.DAY_OF_MONTH, 0)
            }
            "Year" -> {
                // Устанавливаем начало года
                startDate.set(Calendar.MONTH, Calendar.JANUARY)
                startDate.set(Calendar.DAY_OF_MONTH, 0)
            }
        }

        val endDate = Calendar.getInstance() // Конечная дата — сегодня

        // 7. Фильтрация: оставляем только те транзакции, дата которых находится между startDate и endDate.
        transactions.filter { t ->
            val transactionDate = Calendar.getInstance().apply { time = Date(t.date) }

            !transactionDate.before(startDate) && !transactionDate.after(endDate)
        }
    }
    // --- Конец логики фильтрации ---

    // UI Макет
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E3033))
            .padding(top = 20.dp, start = 20.dp, end = 20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TransactionsPageHeader() // (Требуется определение TransactionsPageHeader)
            Spacer(modifier = Modifier.height(20.dp))

            // Dropdown фильтра
            FilterDropdown( // (Требуется определение FilterDropdown)
                currentFilter = selectedFilter,
                onFilterSelected = { newFilter -> selectedFilter = newFilter }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Список транзакций
            if (filteredTransactions.isEmpty()) {
                Box( // Отображение сообщения, если список пуст
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет транзакций за выбранный период",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn( // 8. Оптимизированный список (отображает только видимые элементы)
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredTransactions) { t ->
                        TransactionRow(transaction = t) // (Требуется определение TransactionRow)

                        // 9. Отображение даты под транзакцией
                        Text(
                            text = displayDateFormat.format(Date(t.date)),
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // 10. Floating Action Button (FAB) для добавления транзакции
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd) // Прижимаем к правому нижнему углу
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true }, // Открываем диалог
                containerColor = colorOliveLight // Зеленый цвет
            ) {
                Text("+", color = Color.White, fontSize = 24.sp)
            }
        }

        // 11. Диалог добавления
        if (showAddDialog) {
            AddTransactionDialog( // (Требуется определение AddTransactionDialog)
                onDismiss = { showAddDialog = false },
                onAdd = { name, category, amount, isIncome ->
                    // Вызываем метод ViewModel для сохранения данных и закрываем диалог
                    viewModel.addTransaction(name, category, amount, isIncome)
                    showAddDialog = false
                }
            )
        }
    }
}



@Composable
fun FilterDropdown(
    currentFilter: String, // Текущее выбранное значение ("Month", "Week", "Year")
    onFilterSelected: (String) -> Unit // Callback, вызываемый при выборе нового фильтра
) {
    // 1. Состояние: Управляет видимостью выпадающего меню.
    var expanded by remember { mutableStateOf(false) }
    val filters = listOf("Week", "Month", "Year")

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) { // 2. Контейнер, который позволяет меню "расширяться"
        Row( // Элемент, который виден всегда и по клику открывает меню
            modifier = Modifier
                .clickable { expanded = true } // 3. Открываем меню по клику
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text( // 4. Основной текст, показывающий текущий фильтр
                text = "This $currentFilter",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "▼", color = Color.White) // Иконка-стрелка
        }

        DropdownMenu( // 5. Само выпадающее меню
            expanded = expanded,
            onDismissRequest = { expanded = false }, // Закрываем по клику вне области
            modifier = Modifier.background(Color(0xFF545659)) // Темный фон меню
        ) {
            filters.forEach { filter ->
                DropdownMenuItem( // Элемент списка меню
                    text = {
                        Text(
                            text = filter,
                            // Выделяем зеленым цветом текущий выбранный фильтр
                            color = if (filter == currentFilter) colorOliveLight else Color.White
                        )
                    },
                    onClick = {
                        onFilterSelected(filter) // 6. Вызываем callback с новым значением
                        expanded = false // Закрываем меню
                    }
                )
            }
        }
    }
}
@Composable
fun TransactionRow(transaction: TransactionItem) {
    // 1. Определение цвета на основе типа транзакции: зеленый для дохода, красный для расхода.
    val color = if (transaction.isIncome) colorOliveLight else Color(0xFFF56C6F)

    // 2. Логика выбора иконки: сопоставление строки категории с ресурсом R.drawable.*
    // Если категория не найдена, iconRes будет null, и иконка не отобразится.
    val iconRes = when (transaction.category) {
        "Supermarket" -> R.drawable.groserries // (Требуется ресурс 'groserries')
        "Medicine" -> R.drawable.medicine // (Требуется ресурс 'medicine')
        "Flowers" -> R.drawable.flowers // (Требуется ресурс 'flowers')
        "Fast food" -> R.drawable.fastfood // (Требуется ресурс 'fastfood')
        "Clothes" -> R.drawable.clothes // (Требуется ресурс 'clothes')
        "Transport" -> R.drawable.transport // (Требуется ресурс 'transport')
        "Taxi" -> R.drawable.taxi // (Требуется ресурс 'taxi')
        "Rent" -> R.drawable.rent // (Требуется ресурс 'rent')
        "Finance" -> R.drawable.finance // (Требуется ресурс 'finance')
        else -> null
    }

    Box( // Контейнер строки транзакции
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)) // Скругление углов
            .background(Color(0xFF3A3B3E)) // Темно-серый фон для строки
            .padding(16.dp)
    ) {
        Row( // Горизонтальный макет: Иконка/Название слева, Сумма справа
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // Прижимает элементы к противоположным краям
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 3. Условное отображение иконки: только если ресурс был найден (iconRes != null).
                iconRes?.let {
                    Icon(
                        painter = painterResource(id = it),
                        contentDescription = transaction.category,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(end = 12.dp),
                        tint = Color.White
                    )
                }

                Column { // Название и Категория
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
            }

            // 4. Отображение суммы с соответствующим знаком (+/-)
            Text(
                text = (if (transaction.isIncome) "+" else "-") + "${"%,.2f".format(transaction.amount)} €",
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


@Composable
fun TransactionsPageHeader() {
    // Заголовок страницы с логотипом и профилем
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp) // Немного меньший верхний отступ, чем на HomePage
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // Логотип слева, профиль справа
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions",
                color = Color.White,
                fontSize = 40.sp
            )

            IconButton( // Иконка профиля
                onClick = { /* действие по профилю */ },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.profile), // (Требуется ресурс 'profile')
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
@Composable
fun TransactionsPageMain(){
    // Этот компонент-обертка не используется явно в AppTransactionsPage,
    // но сохраняется для целей структуры.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding( start = 20.dp, end = 20.dp)
    ){
        // Основное содержимое будет внутри AppTransactionsPage
    }

}

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }

    val categories = listOf(
        "Supermarket", "Medicine", "Flowers", "Fast food", "Clothes",
        "Transport", "Taxi", "Rent", "Finance"
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    val isCategorySelectionEnabled = !isIncome
    LaunchedEffect(isIncome) {
        if (isIncome) {
            // Если выбран Доход, принудительно устанавливаем Finance
            selectedCategory = "Finance"
        }
        // Если isIncome = false, категория остается той, которую выбрал пользователь
    }

    // Выбираем символ в зависимости от состояния
    val arrowSymbol = if (expanded) "▲" else "▼"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && amt > 0.0) {
                    onAdd(name, selectedCategory, amt, isIncome)
                    onDismiss()
                }
            }) {
                Text("Add", color = colorOliveLight)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        title = { Text("Add Transaction", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name", color = Color.Gray) },
                    textStyle = TextStyle(color = Color.White)
                )

                // 3. Выбор категории с использованием DropdownMenu
                Box {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        label = { Text("Category", color = Color.Gray) },
                        readOnly = true,
                        trailingIcon = {
                            Text(
                                text = arrowSymbol,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { expanded = !expanded }
                                    .padding(end = 8.dp)
                            )
                        },
                        enabled = isCategorySelectionEnabled,
                        colors = if (isCategorySelectionEnabled) OutlinedTextFieldDefaults.colors() else OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = Color(0xFF2E3033),
                            disabledTextColor = Color.White
                        ),
                        // ИЗМЕНЕНИЕ: Выравнивание текста по правому краю в поле категории
                        textStyle = TextStyle(
                            color = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                    )

                    if (isCategorySelectionEnabled) {
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .background(Color(0xFF2E3033))
                                .width(IntrinsicSize.Max)
                        ) {
                            // Исключаем Finance из списка, если это не Доход (чтобы избежать дублирования)
                            categories.filter { it != "Finance" }.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category, color = Color.White) },
                                    onClick = {
                                        selectedCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { char -> char.isDigit() || char == '.' || char == ',' }
                    },
                    label = { Text("Amount", color = Color.Gray) },
                    textStyle = TextStyle(color = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isIncome, onCheckedChange = { isIncome = it })
                    Text("Income", color = Color.White)
                }
            }
        },
        containerColor = Color(0xFF2E3033)
    )
}

//==========================================ReportPage==========================================
@Composable
fun AppReportsPage(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }

    // 1. Корневой Box для фона и наложения слоев (Кнопка поверх Контента)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E3033))
        // УБРАНО: .padding(...) - это ломало позицию хедера
    ) {
        // 2. Вертикальный стек: Сначала Хедер, потом Контент
        Column(modifier = Modifier.fillMaxSize()) {

            HeaderReportsPage() // Хедер с правильным отступом внутри

            // Контент занимает все оставшееся место и имеет отступы по бокам
            Box(
                modifier = Modifier
                    .weight(1f) // Занимает всё место под хедером
                    .padding(horizontal = 20.dp) // Отступы только для контента
            ) {
                MainReportsPage(viewModel)
            }
        }

        // 3. Плавающая кнопка (прижата к низу справа)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp) // Отступ от краев экрана
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = colorOliveLight
            ) {
                Text("+", color = Color.White, fontSize = 24.sp)
            }
        }

        // 4. Диалог
        if (showAddDialog) {
            AddReportLimitDialog(
                onDismiss = { showAddDialog = false },
                onSetLimit = { category, limit, period ->
                    viewModel.setReportLimit(category, limit, period)
                    showAddDialog = false
                }
            )
        }
    }
}
@Composable
fun HeaderReportsPage(){
    //Задний фон (Контейнер)
    Box(
    ) {
        // Header (Внутренний контейнер, задающий отступы)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 1. Установка отступов: сверху (top) 44.dp, по бокам (start/end) 20.dp.
                .padding(top = 44.dp, start = 20.dp, end = 20.dp)
        ) {
            Row( // 2. Горизонтальный макет для размещения элементов в одну строку
                modifier = Modifier.fillMaxWidth(),
                // 3. Распределение: 'SpaceBetween' гарантирует, что логотип и профиль будут прижаты к противоположным краям.
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reports",
                    color = Color.White,
                    fontSize = 40.sp
                )

                IconButton( // 5. Кнопка-контейнер для иконки профиля (обеспечивает кликабельную область)
                    onClick = { /* действие */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon( // 6. Иконка профиля
                        painter = painterResource(id = R.drawable.profile), // (Требуется ресурс 'profile')
                        contentDescription = "Профиль",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape), // Обрезка изображения/иконки в круг
                        tint = Color.White
                    )
                }

            }
        }


    }
}

@Composable
fun MainReportsPage(
    viewModel: TransactionViewModel
) {
    val reports by viewModel.reportsFlow.collectAsState()
    val allTransactions by viewModel.transactionsFlow.collectAsState()

    val selectedFilter = "Month"

    // 1. Calculate spending per category
    val spentByCategoryMap: Map<String, Double> = remember(allTransactions, reports, selectedFilter) {
        val filteredTransactions = filterTransactionsByPeriod(allTransactions, selectedFilter)
            .filter { !it.isIncome }

        filteredTransactions
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    // 2. Identify overspent categories
    val overspentItems = remember(reports, spentByCategoryMap) {
        reports.filter { report ->
            val spent = spentByCategoryMap[report.category] ?: 0.0
            spent > report.limitAmount
        }.map { report ->
            val spent = spentByCategoryMap[report.category] ?: 0.0
            // Return a Triple: Category, Overspent Amount, Icon Resource
            Triple(report.category, spent - report.limitAmount, getCategoryIcon(report.category))
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
            // Section 1: Progress Bars
            items(reports) { report ->
                val spent = spentByCategoryMap[report.category] ?: 0.0
                ReportLimitCard(report = report, spentAmount = spent)
            }

            // Section 2: Categories Over Budget (Only if there are any)
            if (overspentItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Divider Line
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
                    OverspentRow(category = category, amount = amount, iconRes = iconRes)
                }
            }
        }
    }
}
@Composable
fun AddReportLimitDialog(
    onDismiss: () -> Unit,
    // Сигнатура для передачи данных в ViewModel: (Категория, Лимит, Период)
    onSetLimit: (String, Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }

    // Категории, на которые можно ставить лимит (исключаем "Finance", т.к. это доход)
    val limitCategories = listOf(
        "Supermarket", "Medicine", "Flowers", "Fast food", "Clothes",
        "Transport", "Taxi", "Rent"
    )

    // Периоды для лимита
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
                // Заменяем запятую на точку перед конвертацией
                val amt = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                if (amt > 0.0) {
                    onSetLimit(selectedCategory, amt, selectedPeriod)
                    onDismiss()
                }
            }) {
                Text("Set Limit", color = colorOliveLight)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        title = { Text("Set Spending Limit", color = Color.White) },
        text = {
            Column {
                // 1. Выбор категории
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
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
                        modifier = Modifier.background(Color(0xFF2E3033))
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

                // 2. Поле для ввода суммы
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { char -> char.isDigit() || char == '.' || char == ',' }
                    },
                    label = { Text("Limit Amount", color = Color.Gray) },
                    textStyle = TextStyle(color = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                // 3. Выбор периода (Week/Month/Year)
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(
                        value = selectedPeriod,
                        onValueChange = {},
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
                        modifier = Modifier.background(Color(0xFF2E3033))
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
        containerColor = Color(0xFF2E3033)
    )
}
fun filterTransactionsByPeriod(transactions: List<TransactionItem>, period: String): List<TransactionItem> {
    val currentTime = System.currentTimeMillis()
    val calendar = Calendar.getInstance()

    // Определяем начальную точку периода
    val startTime: Long = when (period) {
        "Week" -> {
            calendar.timeInMillis = currentTime
            // Начинаем с начала недели (например, понедельника)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            // Устанавливаем время на 00:00:00
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        "Month" -> {
            calendar.timeInMillis = currentTime
            // Начинаем с 1-го числа текущего месяца
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        "Year" -> {
            calendar.timeInMillis = currentTime
            // Начинаем с 1 января текущего года
            calendar.set(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        else -> 0L
    }

    return transactions.filter { it.date >= startTime }
}
@Composable
fun ReportProgressBar(spent: Double, limit: Double) {
    // 1. Цвета
    val greenColor = colorOliveLight // Зеленый для лимита
    val redColor = Coral   // Красный для перерасхода

    // 2. Расчет
    val overspent = max(0.0, spent - limit)
    val isOverspent = spent > limit

    // Общая база: если перерасход, база — это общая трата, иначе — лимит.
    val totalProgressBase = if (isOverspent) spent else limit

    // Доля лимита (зеленая часть)
    val limitWeight = if (limit > 0) min(1f, (limit / totalProgressBase).toFloat()) else 0f

    // Доля перерасхода (красная часть)
    val overspentWeight = if (overspent > 0) (overspent / totalProgressBase).toFloat() else 0f

    // Прогресс для LinearProgressIndicator
    val progress = if (limit > 0) (spent / limit).toFloat() else 0f


    // 3. Компоновка шкалы прогресса
    if (!isOverspent && limit > 0) {
        // Case 1: Траты <= Лимита (Используем стандартный индикатор)
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)), // Скругленные углы
            color = greenColor,
            trackColor = Color(0x884D4F52)
        )
    } else if (isOverspent) {
        // Case 2: Траты > Лимита (Используем Row для двух цветов)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x884D4F52)) // Фон для всей шкалы
        ) {
            // Зеленая часть (Лимит)
            Box(
                modifier = Modifier
                    .weight(limitWeight)
                    .fillMaxHeight()
                    .background(greenColor)
            )
            // Красная часть (Перерасход)
            Box(
                modifier = Modifier
                    .weight(overspentWeight)
                    .fillMaxHeight()
                    .background(redColor)
            )
        }
    } else {
        // Case 3: Лимит или траты равны 0 (показываем пустой трек)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x884D4F52))
        )
    }
}

@Composable
fun ReportLimitCard(
    report: ReportItem,
    spentAmount: Double
) {
    val overspent = max(0.0, spentAmount - report.limitAmount)
    val isOverspent = spentAmount > report.limitAmount

    // Форматирование
    val spentFormatted = "%.2f".format(spentAmount)
    val limitFormatted = "%.2f".format(report.limitAmount)
    val overspentFormatted = "%.2f".format(overspent)

    // Выбираем цвет для текста "Spent"
    val spentTextColor = when {
        isOverspent -> Coral // Красный при перерасходе
        spentAmount > 0.0 -> colorOliveLight // Зеленый, если траты есть, но в пределах
        else -> Color.White // Белый, если траты 0
    }

    val redColor = Coral

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4D4F52)), // Темно-серый фон карточки
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Заголовок (Категория и Период)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = report.category,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Limit: €$limitFormatted / ${report.period}",
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Прогресс Бар
            ReportProgressBar(spent = spentAmount, limit = report.limitAmount)

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Суммы (Потрачено и Перерасход)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: €$spentFormatted",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = spentTextColor
                )

                if (isOverspent) {
                    Text(
                        text = "Overspent: €$overspentFormatted",
                        fontSize = 14.sp,
                        color = redColor
                    )
                } else {
                    // Placeholder для выравнивания
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
        }
    }
}
// Helper function to map category string to drawable resource
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
fun OverspentRow(category: String, amount: Double, iconRes: Int?) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Icon (Tinted Red)
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = category,
                    modifier = Modifier.size(32.dp),
                    tint = Coral // Red tint
                )
            } else {
                // Fallback circle if no icon found
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Coral)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Text "Overspent by: €..."
            Text(
                text = "Overspent by: €${"%.2f".format(amount)}",
                color = Coral, // Red Text
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 3. Small underline separator for each item
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Color(0xFF4A4B4D), thickness = 1.dp)
    }
}

//==========================================BudgetPage==========================================
@Composable
fun AppBudgetPage(
    navController: NavController,
    viewModel: TransactionViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    // Собираем Flow целей как State
    val goals by viewModel.goalsFlow.collectAsState(initial = emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2E3033)) // Основной темный фон
            .padding(horizontal = 20.dp)
    ) {
        // Контент: Список целей
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderBudgetPage() // Переиспользуем заголовок

            if (goals.isEmpty()) {
                // Сообщение при отсутствии целей
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No goals set yet.", color = Color.Gray, fontSize = 16.sp)
                    Text("Tap '+' to create a savings goal.", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                // Список целей
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp), // Отступ для FAB
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(goals, key = { it.id }) { goal ->
                        GoalCard(goal = goal, onClick = {
                            // TODO: Здесь будет логика для просмотра/пополнения цели
                            println("Goal clicked: ${goal.name}")
                        })
                    }
                }
            }
        }

        // FAB (Кнопка добавления)
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color(0xFF67D38B), // Фирменный зеленый
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Text("+", color = Color.White, fontSize = 24.sp)
        }

        // Диалог добавления
        if (showAddDialog) {
            AddGoalDialog(
                onDismiss = { showAddDialog = false },
                onAddGoal = { goal ->
                    viewModel.addGoal(goal)
                    showAddDialog = false
                }
            )
        }
    }
}


@Composable
fun HeaderBudgetPage(){
    // Header (Внутренний контейнер, задающий отступы)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Установка отступов: сверху (top) 44.dp, по бокам (start/end) 20.dp.
            .padding(top = 44.dp, start = 0.dp, end = 0.dp)
    ) {
        Row( // Горизонтальный макет для размещения элементов в одну строку
            modifier = Modifier.fillMaxWidth(),
            // Распределение: 'SpaceBetween' гарантирует, что логотип и профиль будут прижаты к противоположным краям.
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Savings & Goals",
                color = Color.White,
                fontWeight = FontWeight.Bold, // Добавим жирный шрифт
                fontSize = 30.sp
            )

            // Профиль (Используем заглушку R.drawable.profile)
            IconButton(
                onClick = { /* действие */ },
                modifier = Modifier.size(48.dp)
            ) {
                // Иконка профиля - предполагаем, что ресурс R.drawable.profile существует
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

@Composable
fun GoalCard(goal: GoalItem, onClick: () -> Unit) {

    // 1. Расчет темпа накопления (Saving Pace Logic)
    val targetColor = Color(android.graphics.Color.parseColor(goal.colorHex))
    val targetAmount = goal.targetAmount
    val savedAmount = goal.savedAmount

    // Переводим Long в Temporal для расчетов
    // ID используется как дата создания
    val deadlineInstant = Instant.ofEpochMilli(goal.deadline)
    val startInstant = Instant.ofEpochMilli(goal.id)
    val nowInstant = Instant.now()

    // Полный срок в днях
    val totalDurationDays = Duration.between(startInstant, deadlineInstant).toDays().toDouble().coerceAtLeast(1.0)

    // Сколько времени прошло (в днях)
    val elapsedDurationDays = Duration.between(startInstant, nowInstant).toDays().toDouble().coerceAtLeast(0.0)

    // Ежедневный необходимый темп накопления
    val requiredDailyPace = targetAmount / totalDurationDays

    // Требуемая сумма на текущий момент
    val requiredSavedAmount = requiredDailyPace * elapsedDurationDays

    // Отклонение от темпа (отрицательное — отставание, положительное — опережение)
    val paceDeviation = savedAmount - requiredSavedAmount

    // Форматирование
    val paceFormatted = "€${"%,.2f".format(paceDeviation)}"
    val amountFormatted = "€${"%,.2f".format(savedAmount)} / €${"%,.2f".format(targetAmount)}"

    // Прогресс
    val progress = (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f)

    // 2. Визуальные элементы
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // Уменьшим вертикальный отступ для плотного списка
            .clickable(onClick = onClick), // Делаем карточку кликабельной
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4D4F52)) // Темно-серый
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Иконка
                Icon(
                    painter = painterResource(id = goal.iconRes),
                    contentDescription = goal.name,
                    modifier = Modifier.size(32.dp),
                    tint = targetColor // Цвет иконки
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Название и дата
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = goal.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = "Deadline: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(goal.deadline))}",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                // Темп накопления (Pace Deviation)
                Text(
                    text = paceFormatted,
                    color = if (paceDeviation >= 0) Color(0xFF67D38B) else Color(0xFFF44336), // Зеленый или Красный
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = targetColor, // Цвет прогресс-бара
                trackColor = Color(0xAA2E3033) // Более темный трек
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Суммы
            Text(
                text = amountFormatted,
                color = Color.LightGray,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onAddGoal: (GoalItem) -> Unit
) {
    // --- Состояния полей ввода ---
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var periodValue by remember { mutableStateOf("") } // Срок (число)

    // --- Состояния выпадающих списков ---
    val periodUnits = listOf("Days", "Weeks", "Months", "Years")
    var selectedUnit by remember { mutableStateOf("Months") }
    var expandedUnit by remember { mutableStateOf(false) }

    // Выбор цвета (по умолчанию первый из карты)
    var selectedColorName by remember { mutableStateOf(GoalColors.keys.first()) }
    var expandedColor by remember { mutableStateOf(false) }
    val selectedColor = GoalColors[selectedColorName] ?: Color.Gray

    // Выбор иконки (по умолчанию первая из карты)
    var selectedIconName by remember { mutableStateOf(GoalIcons.keys.first()) }
    var expandedIcon by remember { mutableStateOf(false) }
    val selectedIconRes = GoalIcons[selectedIconName] ?: android.R.drawable.ic_menu_help

    // Ошибка валидации
    var isError by remember { mutableStateOf(false) }

    // --- Логика расчета дедлайна ---
    val deadlineDate = remember(periodValue, selectedUnit) {
        val calendar = Calendar.getInstance()
        val value = periodValue.toIntOrNull()

        // Если срок не введен, берем 1 год по умолчанию
        val duration = value ?: 1
        // Единица измерения
        val field = when (selectedUnit) {
            "Days" -> Calendar.DAY_OF_YEAR
            "Weeks" -> Calendar.WEEK_OF_YEAR
            "Months" -> Calendar.MONTH
            "Years" -> Calendar.YEAR
            else -> Calendar.MONTH
        }

        calendar.add(field, duration)
        calendar.timeInMillis
    }

    val formattedDeadline = remember(deadlineDate) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(deadlineDate))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2E3033), // Темный фон диалога
        title = {
            Text(
                text = "New Savings Goal",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()), // Скролл для маленьких экранов
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 1. Название цели
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; isError = false },
                    label = { Text("Goal Name", color = Color.LightGray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedColor as Color,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = selectedColor,
                        cursorColor = selectedColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Сумма
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        // Разрешаем только цифры и точку
                        if (it.all { char -> char.isDigit() || char == '.' }) {
                            amount = it
                            isError = false
                        }
                    },
                    label = { Text("Target Amount (€)", color = Color.LightGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = selectedColor,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = selectedColor,
                        cursorColor = selectedColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Срок и Единицы измерения (в одну строку)
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Поле ввода числа (необязательно)
                    OutlinedTextField(
                        value = periodValue,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) periodValue = it
                        },
                        label = { Text("Duration", color = Color.LightGray) },
                        placeholder = { Text("1", color = Color.Gray) }, // Подсказка по умолчанию
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = selectedColor,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = selectedColor,
                            cursorColor = selectedColor
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Выбор единиц (Days, Weeks, ...)
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit", color = Color.LightGray) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedUnit = true },
                            enabled = false, // Отключаем стандартный ввод, используем клик по Box/Icon
                            textStyle = TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.White,
                                disabledBorderColor = Color.Gray,
                                disabledLabelColor = Color.LightGray
                            )
                        )
                        DropdownMenu(
                            expanded = expandedUnit,
                            onDismissRequest = { expandedUnit = false },
                            modifier = Modifier.background(Color(0xFF424242))
                        ) {
                            periodUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit, color = Color.White) },
                                    onClick = {
                                        selectedUnit = unit
                                        expandedUnit = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Подсказка о дате
                Text(
                    text = "Deadline: $formattedDeadline",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, start = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Выбор Цвета и Иконки
                Row(modifier = Modifier.fillMaxWidth()) {
                    // --- Выбор Цвета ---
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedColorName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Color", color = Color.LightGray) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(selectedColor)
                                        .border(1.dp, Color.White, CircleShape)
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Filled.ArrowDropDown, null, tint = Color.White)
                            },
                            modifier = Modifier.fillMaxWidth().clickable { expandedColor = true },
                            enabled = false,
                            textStyle = TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.White,
                                disabledBorderColor = Color.Gray,
                                disabledLabelColor = Color.LightGray,
                                disabledLeadingIconColor = Color.Unspecified
                            )
                        )
                        DropdownMenu(
                            expanded = expandedColor,
                            onDismissRequest = { expandedColor = false },
                            modifier = Modifier.background(Color(0xFF424242))
                        ) {
                            GoalColors.forEach { (name, color) ->
                                DropdownMenuItem(
                                    text = { Text(name, color = Color.White) },
                                    leadingIcon = {
                                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(color)) // Ошибка здесь
                                    },
                                    onClick = {
                                        selectedColorName = name
                                        expandedColor = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // --- Выбор Иконки ---
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedIconName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Icon", color = Color.LightGray) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = selectedIconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = selectedColor
                                )
                            },
                            trailingIcon = {
                                Icon(Icons.Filled.ArrowDropDown, null, tint = Color.White)
                            },
                            modifier = Modifier.fillMaxWidth().clickable { expandedIcon = true },
                            enabled = false,
                            textStyle = TextStyle(color = Color.White),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color.White,
                                disabledBorderColor = Color.Gray,
                                disabledLabelColor = Color.LightGray,
                                disabledLeadingIconColor = selectedColor
                            )
                        )
                        DropdownMenu(
                            expanded = expandedIcon,
                            onDismissRequest = { expandedIcon = false },
                            modifier = Modifier.background(Color(0xFF424242))
                        ) {
                            GoalIcons.forEach { (name, resId) ->
                                DropdownMenuItem(
                                    text = { Text(name, color = Color.White) },
                                    leadingIcon = {
                                        Icon(painterResource(id = resId), null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    },
                                    onClick = {
                                        selectedIconName = name
                                        expandedIcon = false
                                    }
                                )
                            }
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
                        // Преобразование Color в HEX String (#AARRGGBB)
                        val hexColor = "#" + Integer.toHexString(selectedColor.toArgb()).uppercase()

                        val newGoal = GoalItem(
                            name = name,
                            targetAmount = validAmount,
                            deadline = deadlineDate,
                            colorHex = hexColor,
                            iconRes = selectedIconRes,
                            periodUnit = selectedUnit
                        )
                        onAddGoal(newGoal)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create", color = Color.White, fontSize = 16.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    )
}
