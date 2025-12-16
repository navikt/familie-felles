package no.nav.familie.log.logg

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import kotlin.reflect.KClass

class Logg(
    private val logger: Logger,
) {
    private val teamLogsMarker = MarkerFactory.getMarker("TEAM_LOGS")

    // Secure logging (Default)
    fun info(msg: String) = logger.info(teamLogsMarker, msg)

    fun info(
        format: String,
        arg: Any?,
    ) = logger.info(teamLogsMarker, format, arg)

    fun info(
        format: String,
        arg1: Any?,
        arg2: Any?,
    ) = logger.info(teamLogsMarker, format, arg1, arg2)

    fun info(
        format: String,
        vararg arguments: Any?,
    ) = logger.info(teamLogsMarker, format, *arguments)

    fun info(
        msg: String,
        t: Throwable?,
    ) = logger.info(teamLogsMarker, msg, t)

    fun warn(msg: String) = logger.warn(teamLogsMarker, msg)

    fun warn(
        format: String,
        arg: Any?,
    ) = logger.warn(teamLogsMarker, format, arg)

    fun warn(
        format: String,
        vararg arguments: Any?,
    ) = logger.warn(teamLogsMarker, format, *arguments)

    fun warn(
        msg: String,
        t: Throwable?,
    ) = logger.warn(teamLogsMarker, msg, t)

    fun error(msg: String) = logger.error(teamLogsMarker, msg)

    fun error(
        format: String,
        arg: Any?,
    ) = logger.error(teamLogsMarker, format, arg)

    fun error(
        format: String,
        vararg arguments: Any?,
    ) = logger.error(teamLogsMarker, format, *arguments)

    fun error(
        msg: String,
        t: Throwable?,
    ) = logger.error(teamLogsMarker, msg, t)

    // Vanlig logging
    fun vanligInfo(msg: String) = logger.info(msg)

    fun vanligInfo(
        format: String,
        arg: Any?,
    ) = logger.info(format, arg)

    fun vanligInfo(
        format: String,
        arg1: Any?,
        arg2: Any?,
    ) = logger.info(format, arg1, arg2)

    fun vanligInfo(
        format: String,
        vararg arguments: Any?,
    ) = logger.info(format, *arguments)

    fun vanligInfo(
        msg: String,
        t: Throwable?,
    ) = logger.info(msg, t)

    fun vanligWarn(msg: String) = logger.warn(msg)

    fun vanligWarn(
        format: String,
        arg: Any?,
    ) = logger.warn(format, arg)

    fun vanligWarn(
        format: String,
        arg1: Any?,
        arg2: Any?,
    ) = logger.warn(format, arg1, arg2)

    fun vanligWarn(
        format: String,
        vararg arguments: Any?,
    ) = logger.warn(format, *arguments)

    fun vanligWarn(
        msg: String,
        t: Throwable?,
    ) = logger.warn(msg, t)

    fun vanligError(msg: String) = logger.error(msg)

    fun vanligError(
        format: String,
        arg: Any?,
    ) = logger.error(format, arg)

    fun vanligError(
        format: String,
        arg1: Any?,
        arg2: Any?,
    ) = logger.error(format, arg1, arg2)

    fun vanligError(
        format: String,
        vararg arguments: Any?,
    ) = logger.error(format, *arguments)

    fun vanligError(
        msg: String,
        t: Throwable?,
    ) = logger.error(msg, t)

    companion object {
        fun getLogger(clazz: KClass<*>): Logg = Logg(LoggerFactory.getLogger(clazz.java))

        fun getLogger(name: String): Logg = Logg(LoggerFactory.getLogger(name))
    }
}
