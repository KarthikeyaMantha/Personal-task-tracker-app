package com.personal.daymark.core.focus

enum class FocusSessionType(val displayName: String, val isWork: Boolean) {
    FOCUS("Focus", true),
    SHORT_BREAK("Short Break", false),
    LONG_BREAK("Long Break", false)
}
