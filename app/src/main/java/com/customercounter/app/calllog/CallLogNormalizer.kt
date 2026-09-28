package com.customercounter.app.calllog

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

object CallLogNormalizer {
    private val util = PhoneNumberUtil.getInstance()

    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        if (cleaned.isBlank() || cleaned.contains("*") || cleaned.contains("#")) return null

        val prepared = if (cleaned.startsWith("00")) "+${cleaned.drop(2)}" else cleaned
        return try {
            val parsed = util.parse(prepared, "JO")
            if (!util.isPossibleNumber(parsed) || !util.isValidNumber(parsed)) return fallback(prepared)
            util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (_: NumberParseException) {
            fallback(prepared)
        }
    }

    private fun fallback(value: String): String? {
        val digits = value.filter(Char::isDigit)
        if (digits.length < 7) return null
        return if (digits.startsWith("962") && digits.length >= 11) "+$digits" else digits
    }
}
