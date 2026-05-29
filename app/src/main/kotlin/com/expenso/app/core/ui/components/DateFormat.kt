package com.expenso.app.core.ui.components

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DD_MMM_YYYY: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)

private val DD_MMM: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd-MMM", Locale.ENGLISH)

fun formatDayDdMmmYyyy(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().format(DD_MMM_YYYY)

fun formatDayDdMmmYyyy(date: LocalDate): String = date.format(DD_MMM_YYYY)

fun formatDayDdMmm(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().format(DD_MMM)

fun formatDayDdMmm(date: LocalDate): String = date.format(DD_MMM)
