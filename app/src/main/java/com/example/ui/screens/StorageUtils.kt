package com.example.ui.screens

import java.text.DecimalFormat

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val df = DecimalFormat("#,##0.0")
    if (bytes >= 1024L * 1024L * 1024L) {
        return "${df.format(bytes.toDouble() / (1024L * 1024L * 1024L))} GB"
    } else if (bytes >= 1024L * 1024L) {
        return "${df.format(bytes.toDouble() / (1024L * 1024L))} MB"
    } else if (bytes >= 1024L) {
        return "${df.format(bytes.toDouble() / 1024L)} KB"
    }
    return "$bytes B"
}
