package ru.levar

import java.util.*


object AppConfig {
    private val properties = Properties().apply {
        load(ClassLoader.getSystemResourceAsStream("config/config.properties"))
    }

    val serviceUri: String get() = properties.getProperty("serviceUri")
}
