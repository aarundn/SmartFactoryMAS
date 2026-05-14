import com.example.smartfactorymas.EngineEvent
import com.example.smartfactorymas.EngineInput
import com.example.smartfactorymas.LogEvent
import com.example.smartfactorymas.MASOutput
import com.example.smartfactorymas.MultiMachineResultEvent
import com.example.smartfactorymas.ProposalEvent
import com.example.smartfactorymas.ResultEvent
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object CliInterop {
    private val json = Json {
        ignoreUnknownKeys = true; classDiscriminator = "type"; isLenient = true
        coerceInputValues = true; encodeDefaults = true
    }

    data class EngineResult(
        val output: MASOutput,
        val logs: List<LogEvent>,
        val multiMachineResult: MultiMachineResultEvent? = null   // ← NEW
    )

    fun runSimulation(input: EngineInput? = null, onEvent: (EngineEvent) -> Unit = {}): EngineResult {
        val exe     = resolveExePath()
        val process = ProcessBuilder(exe.absolutePath).redirectErrorStream(true).start()

        process.outputStream.bufferedWriter().use { writer ->
            if (input != null && input.arhAgents.isNotEmpty())
                writer.write(json.encodeToString(EngineInput.serializer(), input))
        }

        var resultEvent: ResultEvent? = null
        var mmResultEvent: MultiMachineResultEvent? = null
        val logEvents = mutableListOf<LogEvent>()

        BufferedReader(InputStreamReader(process.inputStream)).forEachLine { line ->
            if (line.isBlank()) return@forEachLine

            // 🌟 1. أضف هذا السطر لطباعة كل شيء يرسله C++ إلى كونسول أندرويد/JVM
            println("🤖 [C++ RAW OUTPUT]: $line")
            try {
                when {
                    line.contains("\"type\":\"result\"") || line.startsWith("JSON_RESULT:") -> {
                        val stripped = if (line.startsWith("JSON_RESULT:")) line.removePrefix("JSON_RESULT:") else line
                        val sanitised = if (!stripped.contains("\"type\"")) stripped.replaceFirst("{", "{\"type\":\"result\",") else stripped
                        val ev = json.decodeFromString<ResultEvent>(sanitised)
                        resultEvent = ev; onEvent(ev)
                    }
                    line.contains("\"type\":\"multi_machine_result\"") -> {          // ← NEW
                        val ev = json.decodeFromString<MultiMachineResultEvent>(line)
                        mmResultEvent = ev; onEvent(ev)
                        // Echo negotiation messages as log entries so they appear in the log panel
                        ev.messages.forEach { msg ->
                            val logEv = LogEvent(agent = msg.from,
                                msg = "${msg.type} → ${msg.to}: job=${msg.jobId} t${msg.originalTime.toInt()}→t${msg.requestedTime.toInt()} ${if (msg.accepted) "✓ accepted" else "✗ rejected"}",
                                level = if (msg.accepted) "info" else "warn")
                            logEvents.add(logEv); onEvent(logEv)
                        }
                    }
                    line.contains("\"type\":\"log\"") -> {
                        val ev = json.decodeFromString<LogEvent>(line); logEvents.add(ev); onEvent(ev)
                    }
                    line.contains("\"type\":\"proposal\"") -> {
                        val ev = json.decodeFromString<ProposalEvent>(line); onEvent(ev)
                    }
                    else -> {
                        val fb = LogEvent(agent = "SYS", msg = line, level = "warn")
                        logEvents.add(fb); onEvent(fb)
                    }
                }
            } catch (e: Exception) {
                val fb = LogEvent(
                    agent = "SYS",
                    msg = "Parse error: ${e.message} | ${line.take(120)}",
                    level = "error"
                )
                logEvents.add(fb); onEvent(fb)
            }
        }
        process.waitFor()
        val result = resultEvent ?: throw RuntimeException("Engine produced no final ResultEvent.")
        return EngineResult(
            output = MASOutput(
                result.chosenArh,
                result.w1,
                result.w2,
                result.alertTime,
                result.proposals
            ),
            logs = logEvents,
            multiMachineResult = mmResultEvent)
    }

    private fun resolveExePath(): File {
        val resourcesDir = System.getProperty("compose.application.resources.dir")
        if (resourcesDir != null) { val f = File(resourcesDir, "core_engine.exe"); if (f.exists()) return f }
        var dir = File(System.getProperty("user.dir"))
        if (dir.name == "composeApp") dir = dir.parentFile
        listOf("build/core_engine.exe", "build_mas/core_engine.exe").forEach {
            val f = File(dir, it); if (f.exists()) return f
        }
        return File(dir, "build/core_engine.exe")
    }
}