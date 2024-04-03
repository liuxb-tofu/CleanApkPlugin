package io.github.blookliu.clean_apk_plugin

import com.android.build.gradle.api.ApplicationVariant
import com.google.common.io.Files
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue
import com.google.devrel.gmscore.tools.apk.arsc.ResourceTableChunk
import com.google.devrel.gmscore.tools.apk.arsc.StringPoolChunk
import com.google.devrel.gmscore.tools.apk.arsc.TypeChunk
import io.github.blookliu.clean_apk_plugin.CleanApkPlugin.Companion.LOG_TAG
import org.gradle.api.Project
import java.io.File
import java.lang.Float.max
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.zip.ZipEntry

class WebpConvertAction(
    private val project: Project,
    private val variant: ApplicationVariant,
    private val cleanApkArgsExtension: CleanApkArgsExtension
) {
    private val drawableConfig = ImageConfig().apply {
        this.includes = ArrayList()
        this.excludes = ArrayList()
    }
    private val mipmapConfig = ImageConfig().apply {
        this.includes = ArrayList()
        this.excludes = ArrayList()
    }

    val needExecute: Boolean
        get() {
            return drawableConfig.enable || mipmapConfig.enable
        }

    init {
        val flavor = variant.flavorName
        val buildType = variant.buildType.name
        with(cleanApkArgsExtension) {
            webp?.findByName(flavor)?.let {
                setupImageConfig(it)
            }
            webp?.findByName(buildType)?.let {
                setupImageConfig(it)
            }
            webp?.findByName(variant.name)?.let {
                setupImageConfig(it)
            }

        }

    }

    private fun setupImageConfig(it: WebpInfo) {
        if (it.enable) {
            it.drawableIncludes?.forEach { include ->
                if (!drawableConfig.includes!!.contains(include)) {
                    (drawableConfig.includes as ArrayList).add(include)
                }
            }
            it.drawableExcludes?.forEach { exclude ->
                if (!drawableConfig.excludes!!.contains(exclude)) {
                    (drawableConfig.excludes as ArrayList).add(exclude)
                }
            }

            it.mipmapIncludes?.forEach { include ->
                if (!mipmapConfig.includes!!.contains(include)) {
                    (mipmapConfig.includes as ArrayList).add(include)
                }
            }
            it.mipmapExcludes?.forEach { exclude ->
                if (!mipmapConfig.excludes!!.contains(exclude)) {
                    (mipmapConfig.excludes as ArrayList).add(exclude)
                }
            }

            it.quality?.let { newQuality ->
                if (drawableConfig.quality == null) {
                    drawableConfig.quality = newQuality
                    mipmapConfig.quality = newQuality
                } else {
                    drawableConfig.quality = max(newQuality, drawableConfig.quality!!)
                    mipmapConfig.quality = max(newQuality, mipmapConfig.quality!!)
                }
            }
            drawableConfig.enable = true
            mipmapConfig.enable = true
        }
    }

    private fun log(message: String) {
        project.logger.info("$LOG_TAG: $message")
    }

    fun execute(
        archivesDir: String,
        collectedFiles: CopyOnWriteArrayList<File>,
        compressedData: ConcurrentHashMap<String, Int>
    ) {
        // 转webp
        val arscFile = File(archivesDir, "resources.arsc")
        arscFile.inputStream().use { it ->
            BinaryResourceFile.fromInputStream(it).apply {
                (chunks as List<ResourceTableChunk>).forEach { tableChunk ->
                    tableChunk.packages.forEach { packageChunk ->
//                        log("package: ${packageChunk.packageName}")
                        val webpTasks = ArrayList<WebPTask>()
                        if (drawableConfig.enable) {
                            val drawableSpecChunk = packageChunk.getTypeSpecChunk("drawable")
                            val drawableChunks = packageChunk.getTypeChunks(
                                drawableSpecChunk.id
                            )
                            drawableChunks.forEach { drawableChunk ->
                                if (drawableChunk.configuration.isDefault) {
                                    log("dir name: ${drawableChunk.configuration}")
                                } else {
                                    log("dir name: ${drawableChunk.typeName}-${drawableChunk.configuration}")
                                }

                                drawableChunk.entries.forEach { (index, entry) ->
                                    log("entry: ${entry.key()}")
                                    val includeMode = !drawableConfig.includes.isNullOrEmpty()
                                    if ((includeMode && drawableConfig.includes?.contains(entry.key()) == true)
                                        || (!includeMode && drawableConfig.excludes?.contains(entry.key()) == false)
                                    ) {
//                                        log("convert webp drawable: ${entry.key()}")
                                        generateWebpTask(
                                            entry,
                                            tableChunk,
                                            archivesDir,
                                            collectedFiles,
                                            compressedData,
                                            drawableConfig.quality ?: 75f
                                        )?.let {
                                            webpTasks.add(it)
                                        }
                                    }
                                }
                            }
                        }
                        if (mipmapConfig.enable) {
                            val mipmapSpecChunk = packageChunk.getTypeSpecChunk("mipmap")
                            val mipmapChunks = packageChunk.getTypeChunks(
                                mipmapSpecChunk.id
                            )
                            mipmapChunks.forEach { mipmapChunk ->
                                if (mipmapChunk.configuration.isDefault) {
                                    log("dir name: ${mipmapChunk.configuration}")
                                } else {
                                    log("dir name: ${mipmapChunk.typeName}-${mipmapChunk.configuration}")
                                }

                                mipmapChunk.entries.forEach { (index, entry) ->
                                    val includeMode = !mipmapConfig.includes.isNullOrEmpty()
                                    if ((includeMode && mipmapConfig.includes?.contains(entry.key()) == true) || (!includeMode && mipmapConfig.excludes?.contains(
                                            entry.key()
                                        ) == false)
                                    ) {
//                                        log("convert webp drawable: ${entry.key()}")
                                        generateWebpTask(
                                            entry,
                                            tableChunk,
                                            archivesDir,
                                            collectedFiles,
                                            compressedData,
                                            drawableConfig.quality ?: 75f
                                        )?.let {
                                            webpTasks.add(it)
                                        }
                                    }
                                }
                            }
                        }

                        if (webpTasks.isNotEmpty()) {
                            val startTick = System.currentTimeMillis()
                            val coreNum = Runtime.getRuntime().availableProcessors()
                            val results = ArrayList<Future<WebPTask>>()
                            val pool = Executors.newFixedThreadPool(coreNum)
                            webpTasks.forEach {
                                results.add(pool.submit(Callable {
                                    return@Callable it.performConvert()
                                }))
                            }

                            results.forEach {
                                val ret = it.get()
                            }

                            webpTasks.forEach {
//                                log(
//                                    "[${it.success}] ${
//                                        it.sourceFile.toPath().relativeTo(Paths.get(archivesDir))
//                                    } -> ${
//                                        it.outputFile.toPath().relativeTo(Paths.get(archivesDir))
//                                    }"
//                                )
                            }
                            log("convert webp task cost: ${System.currentTimeMillis() - startTick}ms")
                        }
                        Files.write(this.toByteArray(), arscFile)
                    }
                }
            }
        }
    }

    private fun generateWebpTask(
        entry: TypeChunk.Entry,
        tableChunk: ResourceTableChunk,
        archivesDir: String,
        collectedFiles: CopyOnWriteArrayList<File>,
        compressedData: ConcurrentHashMap<String, Int>,
        quality: Float
    ): WebPTask? {
        if (!entry.isComplex) {
            entry.value()?.apply {
                if (this.type() == BinaryResourceValue.Type.STRING) {
                    val drawableValue = tableChunk.stringPool.getString(this.data())
                    log("drawable value: $drawableValue")
                    if (drawableValue.endsWith(".9.png")) {
                        log("can not convert .9.png")
                        return null
                    } else if (drawableValue.endsWith(".webp")) {
                        log("it is already a webp file")
                        return null
                    } else if (drawableValue.endsWith(".png") || drawableValue.endsWith(".jpg")) {
                        return WebPTask(
                            project,
                            archivesDir,
                            collectedFiles,
                            compressedData,
                            tableChunk.stringPool,
                            data(),
                            drawableValue,
                            quality
                        )
                    }
                }
            }
        }
        return null
    }

    inner class WebPTask(
        private val project: Project,
        archivesDir: String,
        private val collectFiles: CopyOnWriteArrayList<File>,
        private val compressData: ConcurrentHashMap<String, Int>,
        private val stringPool: StringPoolChunk,
        private val stringPoolIndex: Int,
        private val value: String,
        private val quality: Float
    ) {
        var success = false
        val sourceFile: File = File(archivesDir, value)
        val outputFile: File = File(archivesDir, value.substringBeforeLast(".") + ".webp")

        fun performConvert(): WebPTask {
            success = WebpUtil.executeConvert(
                project, sourceFile.absolutePath, outputFile.absolutePath, quality
            )
//            project.logger.info("$LOG_TAG: convert webp $value - ${value.substringBeforeLast(".")}.webp, result: $success")
            if (success) {
                val oldFileSize = sourceFile.length()
                val newFileSize = outputFile.length()
                if (newFileSize >= oldFileSize) {
                    project.logger.error("$LOG_TAG: [${outputFile.name}:$newFileSize] is larger than [${sourceFile.name}:$oldFileSize], drop webp file")
                    success = false
                    return this
                }
                val target = collectFiles.find {
                    it.absolutePath == sourceFile.absolutePath
                }
                collectFiles.remove(target)
                collectFiles.add(outputFile)
                compressData["${value.substringBeforeLast(".")}.webp"] = ZipEntry.DEFLATED
                stringPool.updateString(stringPoolIndex, value.substringBeforeLast(".") + ".webp")
            }
            return this
        }
    }
}