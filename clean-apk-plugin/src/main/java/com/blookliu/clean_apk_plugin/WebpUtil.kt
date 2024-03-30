package com.blookliu.clean_apk_plugin

import com.google.common.io.Files
import com.blookliu.clean_apk_plugin.CleanApkPlugin.Companion.LOG_TAG
import org.gradle.api.Project
import java.io.File

object WebpUtil {
    private var webpDir: String? = null

    @Synchronized
    private fun ensureWebp(project: Project) {
        if (webpDir == null || !File(webpDir).exists()) {
            val executeName = if (OS.isWindows()) {
                "bin/windows/cwebp.exe"
            } else if (OS.isLinux()) {
                "bin/linux/cwebp"
            } else if (OS.isMac()) {
                "bin/mac/cwebp"
            } else {
                ""
            }
            val webpFile = File(project.buildDir, executeName)
            if (!webpFile.exists()) {
                webpFile.parentFile.mkdirs()
                javaClass.classLoader.getResource(executeName).openStream().use {
                    Files.write(it.readAllBytes(), webpFile)
                    project.logger.info("$LOG_TAG: install webp execute file")
                }
            }
            webpDir = webpFile.absolutePath

            project.exec {
                it.commandLine = when {
                    OS.isLinux() || OS.isMac() -> listOf("chmod", "+x", webpDir)
                    OS.isWindows() -> listOf("cmd", "/c echo Y|cacls $webpDir /t /p everyone:f")
                    else -> TODO("Unsupported OS ${OS.name}")
                }
            }
        }
    }

    fun executeConvert(project: Project, sourceFile: String, outputFile: String, quality: Float) : Boolean {
        ensureWebp(project)
        if (webpDir.isNullOrEmpty()) {
            return false
        }
        val rc = project.exec { spec ->
            spec.isIgnoreExitValue = true
            val command = listOf(webpDir, "-mt", "-quiet", "-q", "$quality", sourceFile, "-o", outputFile)
//            project.logger.info("$LOG_TAG: command: ${command.joinToString(" ")}")
            spec.commandLine = command
        }
        return rc.exitValue == 0
    }
}