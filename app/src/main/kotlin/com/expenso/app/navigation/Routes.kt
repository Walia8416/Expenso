package com.expenso.app.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SCANNER = "scanner"
    const val HISTORY = "history"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val CATEGORY_MANAGER = "settings/categories"
    const val EXPENSE_DETAIL = "expense/{expenseId}"

    fun expenseDetail(id: String) = "expense/$id"
}
