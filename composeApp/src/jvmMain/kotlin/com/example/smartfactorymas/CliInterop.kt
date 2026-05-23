package com.example.smartfactorymas

import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * CliInterop — all communication between the Kotlin UI and the C++ engine.
 *
 * Parses the JSON stream emitted by main.cpp.
 * IMPORTANT: BatchResultEvent now uses a NESTED stability model.
 */
object CliInterop {

    private val json = Json {
        ignoreUnknownKeys = true        // forward-compatible with future C++ additions
        classDiscriminator = "type"     // sealed-class dispatch
        isLenient = true                // tolerates trailing commas, unquoted keys
        coerceInputValues = true        // converts nulls to defaults
        encodeDefaults = true
    }

    // ── Result container for single / multi mode ──────────────────────────────
    data class EngineResult(
        val output: MASOutput,
        val logs: List<LogEvent>,
        val multiMachineResult: MultiMachineResultEvent? = null
    )

    // ══════════════════════════════════════════════════════════════════════════
    //  Single / Multi simulation
    // ══════════════════════════════════════════════════════════════════════════
    fun runSimulation(
        input: EngineInput? = null,
        onEvent: (EngineEvent) -> Unit = {}
    ): EngineResult {
        val exe     = resolveExePath()
        val process = ProcessBuilder(exe.absolutePath)
            .redirectErrorStream(true)
            .start()

        process.outputStream.bufferedWriter().use { writer ->
            if (input != null && input.arhAgents.isNotEmpty())
                writer.write(json.encodeToString(EngineInput.serializer(), input))
        }

        var resultEvent: ResultEvent? = null
        var mmResultEvent: MultiMachineResultEvent? = null
        val logEvents = mutableListOf<LogEvent>()

        BufferedReader(InputStreamReader(process.inputStream)).forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            println("🤖 [C++ OUT]: $line")
            try {
                when {
                    line.contains("\"type\":\"result\"") -> {
                        val ev = json.decodeFromString<ResultEvent>(line)
                        resultEvent = ev; onEvent(ev)
                    }
                    line.contains("\"type\":\"multi_machine_result\"") -> {
                        val ev = json.decodeFromString<MultiMachineResultEvent>(line)
                        mmResultEvent = ev; onEvent(ev)
                        ev.messages.forEach { msg ->
                            val log = LogEvent(
                                agent = msg.from,
                                msg   = "${msg.type} → ${msg.to}: job=${msg.jobId} " +
                                        "t${msg.originalTime.toInt()}→t${msg.requestedTime.toInt()} " +
                                        if (msg.accepted) "✓ accepted" else "✗ rejected",
                                level = if (msg.accepted) "info" else "warn"
                            )
                            logEvents.add(log); onEvent(log)
                        }
                    }
                    line.contains("\"type\":\"log\"") -> {
                        val ev = json.decodeFromString<LogEvent>(line)
                        logEvents.add(ev); onEvent(ev)
                    }
                    line.contains("\"type\":\"proposal\"") -> {
                        val ev = json.decodeFromString<ProposalEvent>(line); onEvent(ev)
                    }
                    line.contains("\"type\":\"batch_result\"") -> {
                        // Ignored here
                    }
                    line.contains("\"type\":\"academic_benchmark\"") -> {
                        // Ignored here
                    }
                    else -> {
                        val fb = LogEvent(agent = "SYS", msg = line, level = "warn")
                        logEvents.add(fb); onEvent(fb)
                    }
                }
            } catch (e: Exception) {
                val fb = LogEvent(
                    agent = "SYS",
                    msg   = "Parse error: ${e.message} | ${line.take(120)}",
                    level = "error"
                )
                logEvents.add(fb); onEvent(fb)
            }
        }

        process.waitFor()
        val result = resultEvent
            ?: throw RuntimeException("C++ engine produced no ResultEvent.")

        return EngineResult(
            output = MASOutput(
                result.chosenArh,
                result.w1,
                result.w2,
                result.alertTime,
                result.proposals
            ),
            logs = logEvents,
            multiMachineResult = mmResultEvent
        )
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Batch simulation  →  Live Top Dashboard
    // ══════════════════════════════════════════════════════════════════════════
    fun runBatchSimulation(inputJson: String): BatchResultEvent? {
        val exe = resolveExePath()
        val process = ProcessBuilder(exe.absolutePath)
            .redirectErrorStream(true)
            .start()

        process.outputStream.bufferedWriter().use { writer ->
            writer.write(inputJson)
            writer.flush()
        }

        var batchResult: BatchResultEvent? = null

        BufferedReader(InputStreamReader(process.inputStream)).forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            println("🧪 [C++ BATCH]: $line")

            try {
                if (line.contains("\"type\":\"batch_result\"")) {
                    batchResult = json.decodeFromString<BatchResultEvent>(line)
                }
            } catch (e: Exception) {
                println("⚠️ Batch parse error: ${e.message} | ${line.take(200)}")
            }
        }

        process.waitFor()
        return batchResult
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Academic Benchmark  →  Bottom Tables (4.6 & 4.7)
    // ══════════════════════════════════════════════════════════════════════════
    fun runAcademicBenchmark(inputJson: String): AcademicBenchmarkEvent? {
        val exe = resolveExePath()
        val process = ProcessBuilder(exe.absolutePath)
            .redirectErrorStream(true)
            .start()

        process.outputStream.bufferedWriter().use { writer ->
            writer.write(inputJson)
            writer.flush()
        }

        var benchmarkResult: AcademicBenchmarkEvent? = null

        BufferedReader(InputStreamReader(process.inputStream)).forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            // We truncate the log output because this JSON string will be massive!
            println("📚 [C++ BENCHMARK]: ${line.take(150)}...")

            try {
                if (line.contains("\"type\":\"academic_benchmark\"")) {
                    benchmarkResult = json.decodeFromString<AcademicBenchmarkEvent>(line)
                }
            } catch (e: Exception) {
                println("⚠️ Benchmark parse error: ${e.message} | ${line.take(200)}")
            }
        }

        process.waitFor()
        return benchmarkResult
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Executable resolution
    // ══════════════════════════════════════════════════════════════════════════
    private fun resolveExePath(): File {
        // 1. Bundled resources (works in packaged MSI/EXE installs)
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) {
            val f = File(resourcesDir, "core_engine.exe")
            if (f.exists()) {
                println("🔧 [Engine] Found in bundled resources: ${f.absolutePath}")
                return f
            }
        }

        // 2. Relative paths from working directory (works in dev/IDE mode)
        var dir = File(System.getProperty("user.dir"))
        if (dir.name == "composeApp") dir = dir.parentFile ?: dir

        val candidates = listOf(
            "backend/build/core_engine.exe",
            "backend/core_engine.exe",
            "build/core_engine.exe",
            "build_mas/core_engine.exe",
            "core_engine.exe"
        )
        for (rel in candidates) {
            val f = File(dir, rel)
            if (f.exists()) {
                println("🔧 [Engine] Found at relative path: ${f.absolutePath}")
                return f
            }
        }

        // 3. Last resort: hardcoded dev path
        val absolute = File(
            "C:\\Users\\HP\\AndroidStudioProjects\\SmartFactoryMAS2\\backend\\build\\core_engine.exe"
        )
        println("🔧 [Engine] Falling back to hardcoded path: ${absolute.absolutePath} (exists=${absolute.exists()})")
        return absolute
    }
}