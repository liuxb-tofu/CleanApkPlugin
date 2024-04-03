package io.github.blookliu.clean_apk_plugin

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer

open class CleanApkArgsExtension {

    var overwriteOutput: Boolean = false

    var outputFileName: String? = null

    var excludeFiles: NamedDomainObjectContainer<ExcludeInfo>? = null

    var webp: NamedDomainObjectContainer<WebpInfo>? = null

    var useSingleTask = false

    fun excludeFiles(configureClosure: Closure<ExcludeInfo>) {
        excludeFiles?.configure(configureClosure)
    }

    fun excludeFiles(action: NamedDomainObjectContainer<ExcludeInfo>.() -> Unit) {
        action.invoke(excludeFiles!!)
    }

    fun webp(action: NamedDomainObjectContainer<WebpInfo>.() -> Unit) {
        action.invoke(webp!!)
    }
    fun webp(configureClosure: Closure<WebpInfo>) {
        webp?.configure(configureClosure)
    }

    fun overwriteOutput(overwrite: Boolean) {
        overwriteOutput = overwrite
    }

    fun outputFileName(output: String) {
        outputFileName = output
    }

    fun useSingleTask(singleTask: Boolean) {
        useSingleTask = singleTask
    }
}

open class ExcludeInfo(val name: String) {
    var excludes: List<String>? = null
}

open class WebpInfo(val name: String) {
    var enable: Boolean = false
    var quality: Float? = null
    var drawableIncludes: List<String>? = null
    var drawableExcludes: List<String>? = null
    var mipmapIncludes: List<String>? = null
    var mipmapExcludes: List<String>? = null

    fun enable(enable: Boolean) {
        this.enable = enable
    }
}

open class ImageConfig(val name: String? = null) {
    var enable: Boolean = false
    var quality: Float? = null
    var includes: List<String>? = null
    var excludes: List<String>? = null

    fun enable(enable: Boolean) {
        this.enable = enable
    }
}
