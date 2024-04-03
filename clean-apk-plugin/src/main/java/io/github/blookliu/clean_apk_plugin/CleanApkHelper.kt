package io.github.blookliu.clean_apk_plugin

import apksigner.ApkSignerTool
import com.android.apksig.internal.apk.AndroidBinXmlParser
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.matcher.GlobPathMatcherFactory
import org.gradle.api.Project
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

object CleanApkHelper {
    fun doCleanApk(
        variant: ApplicationVariant,
        project: Project,
    ) {
        val cleanApkExtension =
            project.extensions.findByName("cleanApk") as CleanApkArgsExtension
        val appExtension = project.extensions.findByName("android") as AppExtension

        val flavor = variant.flavorName
        val buildType = variant.buildType.name
        val excludes = HashSet<String>()

        cleanApkExtension.excludeFiles?.findByName(flavor)?.excludes?.let {
            project.log("add flavor exclude: $flavor, $it")
            excludes.addAll(it)
        }
        cleanApkExtension.excludeFiles?.findByName(buildType)?.excludes?.let {
            project.log("add buildType exclude: $buildType, $it")
            excludes.addAll(it)
        }
        cleanApkExtension.excludeFiles?.findByName(variant.name)?.excludes?.let {
            project.log("add variant exclude: ${variant.name}, $it")
            excludes.addAll(it)
        }

        val webpConvertAction = WebpConvertAction(project, variant, cleanApkExtension)

        val packageTask = variant.packageApplicationProvider.get()

        if (excludes.isNotEmpty() || webpConvertAction.needExecute) {
            project.log("${variant.name} collect excludes: $excludes")
            val apkFiles = packageTask.outputs.files.asFileTree.filter {
                return@filter it.isFile && it.extension == "apk"
            }

            apkFiles.forEach { apkFile ->
                val archivesDir =
                    "${project.buildDir}/tmp/cleanApk/${apkFile.nameWithoutExtension}-${UUID.randomUUID()}"
                // 解压apk
                val compressedData = ConcurrentHashMap(
                    FileOperation.unZipAPK(
                        apkFile.absolutePath, archivesDir
                    )
                )

                val archivesFileTree = project.fileTree(archivesDir)
                val archivesPath = Paths.get(archivesDir)
                var filteredFileList: Collection<File> = archivesFileTree.files

                // 清理资源
                if (excludes.isNotEmpty()) {
                    val pathMatchers: List<PathMatcher> = excludes.map { compileGlob(it) }

//                                    println("archives path: ${archivesPath.pathString}")

                    filteredFileList = archivesFileTree.files.filter { file ->
                        var relativePath = file.toPath().relativeTo(archivesPath)
                        relativePath = Paths.get("${File.separatorChar}$relativePath")
                        val matches = !pathMatchers.none { it.matches(relativePath) }
                        if (matches) {
                            project.log("exclude file ${relativePath.pathString}")
                        }
                        return@filter !matches
                    }
                }

                filteredFileList = CopyOnWriteArrayList(filteredFileList)

                webpConvertAction.execute(archivesDir, filteredFileList, compressedData)

                val finalOutputFile = if (cleanApkExtension.overwriteOutput) {
                    apkFile
                } else if (!cleanApkExtension.outputFileName.isNullOrEmpty()) {
                    File(apkFile.parentFile, cleanApkExtension.outputFileName)
                } else {
                    File(
                        apkFile.parentFile, "${apkFile.nameWithoutExtension}-cleanApk.apk"
                    )
                }

//                                    println("write output file: ${apkFile.absolutePath}")

                // 打包过滤后的文件
                FileOperation.zipFiles(
                    filteredFileList, archivesPath.toFile(), finalOutputFile, compressedData
                )

                if (variant.signingConfig == null) {
                    project.log("signingConfig is null")
                }

                // V2签名需要先做zipalign
                val zipAlignPath =
                    "${appExtension.sdkDirectory.absolutePath}/build-tools/${appExtension.buildToolsVersion}/zipalign"
                val zipAlignFile = File(
                    finalOutputFile.parent, "${finalOutputFile.nameWithoutExtension}-aligned.apk"
                )

                if (zipAlignFile.exists()) {
                    zipAlignFile.delete()
                }
                val xmlParser =
                    AndroidBinXmlParser(ByteBuffer.wrap(File("$archivesDir/AndroidManifest.xml").readBytes()))
                while (xmlParser.name != "application") {
                    xmlParser.next()
                }

                var compressNativeLib = variant.buildType.name != "debug" // debug默认不压缩so
                for (idx in 0 until xmlParser.attributeCount) {
                    if (xmlParser.getAttributeName(idx) == "extractNativeLibs") {
                        compressNativeLib = xmlParser.getAttributeBooleanValue(idx)
                        break
                    }
                }

                FileOperation.alignApk(
                    zipAlignPath, finalOutputFile, zipAlignFile, compressNativeLib
                )

                val signedFile = File(
                    zipAlignFile.parent, "${zipAlignFile.nameWithoutExtension}-signed.apk"
                )

                if (signedFile.exists()) {
                    signedFile.delete()
                }
                // 签名apk
                variant.signingConfig?.apply {
                    if (isSigningReady) {
                        val params = arrayOf(
                            "sign",
                            "--v1-signing-enabled=$isV1SigningEnabled",
                            "--v2-signing-enabled=$isV2SigningEnabled",
                            "--ks=${storeFile?.absolutePath}",
                            "--ks-key-alias=$keyAlias",
                            "--ks-pass=pass:$storePassword",
                            "--key-pass=pass:$keyPassword",
                            "--in=${zipAlignFile.absolutePath}",
                            "--out=${signedFile.absolutePath}",
                            "--v"
                        )
                        ApkSignerTool.main(params)
                    }
                }

                if (!signedFile.exists()) {
                    throw IOException("can not find signed file, ApkSigner may failed")
                }
                finalOutputFile.delete()
                zipAlignFile.delete()
                signedFile.renameTo(finalOutputFile)
                project.delete(archivesPath.toFile())
            }
        }
    }

    private fun compileGlob(pattern: String): PathMatcher {

        return GlobPathMatcherFactory.create(
            if (!pattern.startsWith("/") && !pattern.startsWith("*"))
                "/$pattern"
            else pattern
        )
    }

    fun Project.log(msg: String) {
        this.logger.info("${CleanApkPlugin.LOG_TAG}: $msg")
    }
}