package com.example.smartfactorymas

import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object CliInterop {

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
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

            // Route based on the "type" field in each JSON line
            try {
                when {
                    line.contains("\"type\":\"log\"") -> {
                        val event = json.decodeFromString<LogEvent>(line)
                        logEvents.add(event)
                        onEvent(event)
                    }
                    line.contains("\"type\":\"proposal\"") -> {
                        val event = json.decodeFromString<ProposalEvent>(line)
                        onEvent(event)
                    }
                    line.contains("\"type\":\"result\"") -> {
                        val event = json.decodeFromString<ResultEvent>(line)
                        resultEvent = event
                        onEvent(event)
                    }
                    else -> {
                        // Non-JSON line (unlikely) — log it for visibility
                        val fallback = LogEvent(agent = "SYS", msg = line, level = "warn")
                        logEvents.add(fallback)
                        onEvent(fallback)
                    }
                }
            } catch (e: Exception) {
                val fallback = LogEvent(agent = "SYS", msg = "Parse error: ${e.message} | line=$line", level = "error")
                logEvents.add(fallback)
                onEvent(fallback)
            }
        }

        process.waitFor()

        val result = resultEvent
            ?: throw RuntimeException("Engine produced no final ResultEvent. Check C++ logs.")

        val masOutput = MASOutput(
            chosenArh  = result.chosenArh,
            w1         = result.w1,
            w2         = result.w2,
            alertTime  = result.alertTime,
            proposals  = result.proposals
        )
        return EngineResult(output = masOutput, logs = logEvents)
    }

    private fun resolveExePath(): File {
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            val f = File(resourcesDir, "core_engine.exe")
            if (f.exists()) return f
        }
        var dir = File(System.getProperty("user.dir"))
        if (dir.name == "composeApp") dir = dir.parentFile

        listOf("build/core_engine.exe", "build_mas/core_engine.exe").forEach {
            val f = File(dir, it)
            if (f.exists()) return f
        }
        return File(dir, "build/core_engine.exe")
    }
}