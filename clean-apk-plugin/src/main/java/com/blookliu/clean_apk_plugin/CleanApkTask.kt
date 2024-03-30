package com.blookliu.clean_apk_plugin

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.configurationcache.extensions.capitalized

class CleanApkTask: DefaultTask() {
    private lateinit var variant: ApplicationVariant
    @TaskAction
    fun run() {
        CleanApkHelper.doCleanApk(variant, project)
    }

    class CreateAction(private val variant: ApplicationVariant) : Action<CleanApkTask> {

        val name = "cleanApk${variant.name.capitalized()}"
        val type = CleanApkTask::class.java

        override fun execute(task: CleanApkTask) {
            task.variant = variant
            task.dependsOn("assemble${variant.name.capitalized()}")
        }

    }
}