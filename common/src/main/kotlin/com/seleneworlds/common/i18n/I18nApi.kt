package com.seleneworlds.common.i18n

import com.seleneworlds.common.serialization.SerializedMap
import java.util.Locale

class I18nApi(private val messages: Messages) {
    fun get(key: String, locale: Locale?): String? = messages.get(key, locale)
    fun format(key: String, args: SerializedMap, locale: Locale?): String? = messages.format(key, args, locale)
    fun hasKey(key: String, locale: Locale?): Boolean = messages.has(key, locale)
}
