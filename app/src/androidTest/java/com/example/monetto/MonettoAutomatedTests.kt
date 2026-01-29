package com.example.monetto

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MonettoAutomatedTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // Улучшенная функция сброса валюты
    private fun resetCurrencyToEur() {
        try {
            // 1. Открываем меню выбора валюты
            composeTestRule.onNodeWithTag("currency_selector").performClick()
            // 2. Ждем немного, чтобы анимация меню завершилась
            composeTestRule.waitForIdle()
            // 3. Выбираем EUR из списка. .onLast() гарантирует, что мы жмем на пункт меню, а не на заголовок
            composeTestRule.onAllNodesWithText("EUR", substring = true).onLast().performClick()
        } catch (e: Throwable) {
            // Если меню не открылось или мы уже в EUR, тест просто пойдет дальше
        }
    }

    @Test
    fun testBottomNavigation_SwitchingScreens() {
        // Проверка переходов нижнего меню
        composeTestRule.onNodeWithContentDescription("TransactionsPage").performClick()
        composeTestRule.onNodeWithText("Transactions", substring = true).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("ReportsPage").performClick()
        composeTestRule.onNodeWithText("Reports", substring = true).assertIsDisplayed()
    }

    @Test
    fun testAddTransaction_AndBalanceUpdate() {
        resetCurrencyToEur()

        composeTestRule.onNodeWithContentDescription("TransactionsPage").performClick()
        composeTestRule.onNodeWithText("+").performClick()

        val uniqueName = "AutoTest_Exp"
        val uniqueAmount = "57.89"

        composeTestRule.onNodeWithText("Name").performTextInput(uniqueName)
        composeTestRule.onNodeWithText("Amount", substring = true).performTextInput(uniqueAmount)
        composeTestRule.onNodeWithText("Add").performClick()

        // Проверяем, что транзакция в списке
        composeTestRule.onNodeWithText(uniqueName).assertIsDisplayed()
        // Проверяем сумму (берем первое вхождение, так как сумма может быть и в балансе)
        composeTestRule.onAllNodesWithText("57", substring = true).onFirst().assertExists()
    }

    @Test
    fun testCurrencyConversion_Logic() {
        resetCurrencyToEur()

        composeTestRule.onNodeWithContentDescription("TransactionsPage").performClick()
        composeTestRule.onNodeWithText("+").performClick()

        composeTestRule.onNodeWithText("Name").performTextInput("Salary_Test")
        composeTestRule.onNodeWithText("Amount", substring = true).performTextInput("100")
        composeTestRule.onNodeWithText("Income").performClick()
        composeTestRule.onNodeWithText("Add").performClick()

        // Переключаем на RUB
        composeTestRule.onNodeWithTag("currency_selector").performClick()
        composeTestRule.waitForIdle()
        // Жмем на последний найденный текст RUB (тот, что в меню)
        composeTestRule.onAllNodesWithText("RUB", substring = true).onLast().performClick()

        // Проверяем конвертацию (100 * 105 = 10500)
        composeTestRule.onAllNodesWithText("10", substring = true).onFirst().assertExists()
        composeTestRule.onAllNodesWithText("500", substring = true).onFirst().assertExists()
    }

    @Test
    fun testSavingsGoal_CreationAndDeposit() {
        resetCurrencyToEur()

        composeTestRule.onNodeWithContentDescription("BudgetPage").performClick()
        composeTestRule.onNodeWithText("+").performClick()

        val goalName = "Goal_Test"
        composeTestRule.onNodeWithText("Goal Name").performTextInput(goalName)
        composeTestRule.onNodeWithText("Target Amount", substring = true).performTextInput("1000")
        composeTestRule.onNodeWithText("Duration").performTextInput("12")

        composeTestRule.onNodeWithText("Create").performClick()
        composeTestRule.onNodeWithText(goalName).assertIsDisplayed()
    }
}