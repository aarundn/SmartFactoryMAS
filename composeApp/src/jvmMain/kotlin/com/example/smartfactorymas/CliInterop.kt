package com.example.smartfactorymas

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Bridges the Kotlin UI to the C++ MAS engine.
 *
 * Protocol:
 *  - stdin:  optional JSON config (EngineInput)
 *  - stdout: negotiation logs (human-readable)
 *  - stderr: JSON_RESULT:<json> line (machine-readable)
 */
object CliInterop {

    private val json = Json { ignoreUnknownKeys = true }

    data class EngineResult(
        val output: MASOutput,
        val logs: List<String>
    )

    /**
     * Runs the engine with optional dynamic configuration.
     */
    fun runSimulation(
        input: EngineInput? = null,
        onLogLine: (String) -> Unit = {}
    ): EngineResult {
        val exe = resolveExePath()

        val process = ProcessBuilder(exe.absolutePath)
            .redirectErrorStream(false)
            .start()

        // Send JSON config to stdin (or empty string for defaults)
        process.outputStream.bufferedWriter().use { writer ->
            if (input != null && input.arhAgents.isNotEmpty()) {
                writer.write(json.encodeToString(EngineInput.serializer(), input))
            }
        }

        // Collect JSON from stderr
        var jsonResult: String? = null
        val stderrThread = Thread {
            process.errorStream.bufferedReader().lines().forEach { line ->
                if (line.startsWith("JSON_RESULT:")) {
                    jsonResult = line.removePrefix("JSON_RESULT:")
                }
            }
        }.also { it.isDaemon = true; it.start() }

        // Read stdout logs
        val logs = mutableListOf<String>()
        process.inputStream.bufferedReader().lines().forEach { line ->
            logs.add(line)
            onLogLine(line)
        }

        process.waitFor()
        stderrThread.join(3000)

        val rawJson = jsonResult
            ?: throw RuntimeException("Engine produced no JSON output")

        val parsed = json.decodeFromString(MASOutput.serializer(), rawJson)
        return EngineResult(output = parsed, logs = logs)
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
