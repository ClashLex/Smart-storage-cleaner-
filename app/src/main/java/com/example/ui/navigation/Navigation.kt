package com.example.ui.navigation

object Routes {
    const val SIGN_IN = "signin"
    const val HOME = "home"
    const val DUPLICATES = "duplicates"
    const val PAYWALL = "paywall"
    const val SETTINGS = "settings"
    const val PERMISSION = "permission"
    const val JUNK_CLEANER = "junk_cleaner/{category}"

    fun buildJunkRoute(category: String): String = "junk_cleaner/$category"
}
