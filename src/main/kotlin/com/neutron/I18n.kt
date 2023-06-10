package com.neutron

import java.text.MessageFormat
import java.util.*

object I18n {
	private const val MESSAGES_KEY = "translation"
	private var bundle: ResourceBundle? = null
	var locale: Locale
		get() = Locale.getDefault()
		set(l) {
			Locale.setDefault(l)
		}

	fun isSupported(l: Locale): Boolean {
		val availableLocales = Locale.getAvailableLocales()
		return listOf(*availableLocales).contains(l)
	}

	fun getMessage(key: String): String {
		if (bundle == null) {
			try {
				bundle = ResourceBundle.getBundle(MESSAGES_KEY)
			} catch (e: MissingResourceException) {
				System.err.println(e.message)
				locale = Locale.ENGLISH
				bundle = ResourceBundle.getBundle(MESSAGES_KEY)
			}
		}
		return bundle!!.getString(key)
	}

	fun getMessage(key: String, vararg arguments: Any): String {
		return MessageFormat.format(getMessage(key), *arguments)
	}
}
