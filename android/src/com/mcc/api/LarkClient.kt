package com.mcc.api

import java.io.BufferedReader
import java.io.InputStreamReader

object LarkClient {

    private const val LARK_CLI = "lark-cli"

    fun isAvailable(): Boolean {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("which", LARK_CLI))
            val exitCode = proc.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    fun execute(vararg args: String): String {
        val cmd = arrayOf(LARK_CLI, *args)
        val proc = Runtime.getRuntime().exec(cmd)

        // Read stdout
        val stdoutReader = BufferedReader(InputStreamReader(proc.inputStream))
        val stdout = stdoutReader.readText()
        stdoutReader.close()

        // Read stderr (for diagnostics)
        val stderrReader = BufferedReader(InputStreamReader(proc.errorStream))
        val stderr = stderrReader.readText()
        stderrReader.close()

        val exitCode = proc.waitFor()

        return if (exitCode == 0) {
            stdout.trim()
        } else {
            val errorMsg = stderr.trim().ifEmpty { stdout.trim() }
            "Error (exit $exitCode): $errorMsg"
        }
    }

    fun authStatus(): String {
        return try {
            execute("auth", "status")
        } catch (e: Exception) {
            "Error: ${e.message ?: "Unknown error"}"
        }
    }
}
