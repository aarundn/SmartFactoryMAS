package com.example.smartfactorymas

import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object CliInterop {

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    data class EngineResult(
        val output: MASOutput,
        val logs: List<LogEvent>
    )

    fun runSimulation(
        input: EngineInput? = null,
        onEvent: (EngineEvent) -> Unit = {}
    ): EngineResult {
        val exe = resolveExePath()

        val process = ProcessBuilder(exe.absolutePath)
            .redirectErrorStream(true)   // merge stderr into stdout
            .start()

        // Write config to stdin then close (sends EOF so C++ stops blocking)
        process.outputStream.bufferedWriter().use { writer ->
            if (input != null && input.arhAgents.isNotEmpty()) {
                writer.write(json.encodeToString(EngineInput.serializer(), input))
            }
        }

        var resultEvent: ResultEvent? = null
        val logEvents = mutableListOf<LogEvent>()

        // Read every line from the merged stdout+stderr stream
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        reader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine

            try {
                when {

                    line.contains("\"type\":\"result\"") || line.startsWith("JSON_RESULT:") -> {
                        val stripped =
                            if (line.startsWith("JSON_RESULT:")) line.removePrefix("JSON_RESULT:") else line
                        val jsonToparse = if (!stripped.contains("\"type\"")) stripped.replaceFirst(
                            "{",
                            "{\"type\":\"result\","
                        ) else stripped
// 🟢 السطر السحري: طباعة النص الخام الذي أرسله C++ 🟢
                        println("\n==================================================")
                        println("🕵️ RAW JSON FROM C++:")
                        println(jsonToparse)
                        println("==================================================\n")
                        val event = json.decodeFromString<ResultEvent>(jsonToparse)
                        resultEvent = event

                        // 🟢 ADD CONSOLE LOGS HERE 🟢
                        println("\n==================================================")
                        println("🏆 MAS NEGOTIATION COMPLETE")
                        println("🏆 WINNING TECHNICIAN: ${event.chosenArh}")
                        println("==================================================")

                        event.proposals.forEach { prop ->
                            println("\n👷 TECHNICIAN: ${prop.arhId}")
                            println("   - Proposed Start Time: ${prop.cbmStart}")
                            println("   - Repair Duration (Min, Prob, Max): [${prop.cbmDurMin}, ${prop.cbmDurProb}, ${prop.cbmDurMax}]")
                            println("   - f1 (Production Delay): ${prop.f1Prob}")
                            println("   - f2 (Maintenance Risk): ${prop.f2}")
                            println("   - f (Global Score): ${prop.fProb}")

                            println("   - 📅 NEW OPTIMIZED SCHEDULE (Rescheduled List):")
                            prop.schedule.forEach { block ->
                                println("       [${block.type}] ${block.id} -> Start: ${block.startProb}, End: ${block.endProb}")
                            }
                        }
                        println("==================================================\n")
                        // 🟢 END CONSOLE LOGS 🟢

                        onEvent(event)
                    }

                    line.contains("\"type\":\"log\"") -> {
                        val event = json.decodeFromString<LogEvent>(line)
                        logEvents.add(event)
                        onEvent(event)
                    }

                    line.contains("\"type\":\"proposal\"") -> {
                        val event = json.decodeFromString<ProposalEvent>(line)
                        onEvent(event)
                    }
                    // FIX: detect BOTH "type":"result" AND the legacy JSON_RESULT: prefix
                    line.contains("\"type\":\"result\"") || line.startsWith("JSON_RESULT:") -> {
                        // Strip the prefix if present
                        val stripped = if (line.startsWith("JSON_RESULT:"))
                            line.removePrefix("JSON_RESULT:")
                        else
                            line

                        // If the old binary omitted "type":"result", inject it so
                        // kotlinx.serialization can discriminate the sealed class
                        val jsonToparse = if (!stripped.contains("\"type\""))
                            stripped.replaceFirst("{", "{\"type\":\"result\",")
                        else
                            stripped

                        val event = json.decodeFromString<ResultEvent>(jsonToparse)
                        resultEvent = event
                        onEvent(event)
                    }

                    else -> {
                        val fallback = LogEvent(agent = "SYS", msg = line, level = "warn")
                        logEvents.add(fallback)
                        onEvent(fallback)
                    }
                }
            } catch (e: Exception) {
                val fallback = LogEvent(
                    agent = "SYS",
                    msg = "Parse error: ${e.message} | line=${line.take(120)}",
                    level = "error"
                )
                logEvents.add(fallback)
                onEvent(fallback)
            }
        }

        process.waitFor()

        val result = resultEvent
            ?: throw RuntimeException("Engine produced no final ResultEvent. Check C++ logs.")

        val masOutput = MASOutput(
            chosenArh = result.chosenArh,
            w1 = result.w1,
            w2 = result.w2,
            alertTime = result.alertTime,
            proposals = result.proposals
        )
        return EngineResult(output = masOutput, logs = logEvents)
    }

    private fun resolveExePath(): File {
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            val f = File(resourcesDir, "core_engine.exe")
            if (f.exists()) {
                println("🚀 RUNNING ENGINE FROM RESOURCES: ${f.absolutePath}")
                return f
            }
        }
        var dir = File(System.getProperty("user.dir"))
        if (dir.name == "composeApp") dir = dir.parentFile

        listOf("build/core_engine.exe", "build_mas/core_engine.exe").forEach {
            val f = File(dir, it)
            if (f.exists()) {
                println("🚀 RUNNING ENGINE FROM BUILD DIR: ${f.absolutePath}")
                return f
            }
        }

        val defaultFile = File(dir, "build/core_engine.exe")
        println("⚠️ ENGINE NOT FOUND! DEFAULTING TO: ${defaultFile.absolutePath}")
        return defaultFile
    }
}