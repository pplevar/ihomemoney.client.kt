package ru.levar

import java.util.*


object AppConfig {
    private val properties = Properties().apply {
        val stream = ClassLoader.getSystemResourceAsStream("config/config.properties")
            ?: throw IllegalStateException("Configuration file config/config.properties not found")
        load(stream)
    }

    val serviceUri: String
        get() = properties.getProperty("serviceUri")
            ?: throw IllegalStateException("serviceUri not defined in configuration")
}
