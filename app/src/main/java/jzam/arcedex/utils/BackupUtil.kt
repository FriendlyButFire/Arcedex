package jzam.arcedex.utils

import jzam.arcedex.models.PokeResearch
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/*
 * Backup/restore for research progress. Only goalProgress is exported (per Pokemon+task) since
 * everything else (name, task text, goals, points) is static data already baked into the app.
 *
 * Format: JSON -> gzip -> Base64. Safe to select/copy as a single line string.
 */

private const val BACKUP_VERSION = 1
private const val INVALID_BACKUP_MESSAGE = "This doesn't look like a valid Arcedex backup code."

//Builds a compact backup string containing only tasks with actual progress (goalProgress > 0).
@OptIn(ExperimentalEncodingApi::class)
fun exportProgress(tasks: List<PokeResearch>): String {
    val json = buildString {
        append("{\"version\":").append(BACKUP_VERSION).append(",\"progress\":[")
        var first = true
        for (task in tasks) {
            if (task.goalProgress <= 0) continue
            if (!first) append(",")
            first = false
            append("{\"name\":\"").append(jsonEscape(task.name))
            append("\",\"task\":\"").append(jsonEscape(task.task))
            append("\",\"goalProgress\":").append(task.goalProgress)
            append("}")
        }
        append("]}")
    }
    val gzipped = ByteArrayOutputStream().use { byteStream ->
        GZIPOutputStream(byteStream).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        byteStream.toByteArray()
    }
    return Base64.encode(gzipped)
}

//Parses a backup string produced by exportProgress. Throws IllegalArgumentException with a
//user-facing message if the string isn't a valid backup.
@OptIn(ExperimentalEncodingApi::class)
fun parseBackup(backup: String): List<Triple<String, String, Int>> {
    val gzipped = try {
        Base64.decode(backup.trim())
    } catch (e: Exception) {
        throw IllegalArgumentException(INVALID_BACKUP_MESSAGE)
    }
    val json = try {
        GZIPInputStream(gzipped.inputStream()).use { it.readBytes().toString(Charsets.UTF_8) }
    } catch (e: Exception) {
        throw IllegalArgumentException(INVALID_BACKUP_MESSAGE)
    }
    return parseProgressJson(json)
}

private fun jsonEscape(text: String): String {
    val sb = StringBuilder()
    for (c in text) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> sb.append(c)
        }
    }
    return sb.toString()
}

private fun jsonUnescape(text: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '\\' && i + 1 < text.length) {
            when (text[i + 1]) {
                '"' -> { sb.append('"'); i += 2 }
                '\\' -> { sb.append('\\'); i += 2 }
                'n' -> { sb.append('\n'); i += 2 }
                'r' -> { sb.append('\r'); i += 2 }
                't' -> { sb.append('\t'); i += 2 }
                else -> { sb.append(c); i += 1 }
            }
        } else {
            sb.append(c)
            i += 1
        }
    }
    return sb.toString()
}

//Minimal hand-rolled JSON parser for the fixed 3-field shape this app produces.
private fun parseProgressJson(json: String): List<Triple<String, String, Int>> {
    if (!json.contains("\"progress\"")) {
        throw IllegalArgumentException(INVALID_BACKUP_MESSAGE)
    }
    val results = mutableListOf<Triple<String, String, Int>>()
    val nameRegex = Regex("\"name\":\"((?:[^\"\\\\]|\\\\.)*)\"")
    val taskRegex = Regex("\"task\":\"((?:[^\"\\\\]|\\\\.)*)\"")
    val progressRegex = Regex("\"goalProgress\":(\\d+)")
    val entryRegex = Regex("\\{[^{}]*\\}")
    for (match in entryRegex.findAll(json)) {
        val entry = match.value
        val name = nameRegex.find(entry)?.groupValues?.get(1)?.let { jsonUnescape(it) } ?: continue
        val task = taskRegex.find(entry)?.groupValues?.get(1)?.let { jsonUnescape(it) } ?: continue
        val progress = progressRegex.find(entry)?.groupValues?.get(1)?.toIntOrNull() ?: continue
        results.add(Triple(name, task, progress))
    }
    return results
}