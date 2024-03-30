package com.blookliu.clean_apk_plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Plugin
import org.gradle.api.Project


class CleanApkPlugin : Plugin<Project> {

    private lateinit var project: Project

    companion object {
        const val LOG_TAG = "CleanApk"
    }

    override fun apply(project: Project) {
        this.project = project
        val cleanApk = CleanApkArgsExtension()
        cleanApk.excludeFiles = project.container(ExcludeInfo::class.java)
        cleanApk.webp = project.container(WebpInfo::class.java)

        project.extensions.add("cleanApk", cleanApk)

        project.afterEvaluate {
            log("evaluate clean apk")
            val cleanApkExtension =
                project.extensions.findByName("cleanApk") as CleanApkArgsExtension
            val appExtension = project.extensions.findByName("android") as AppExtension

            if (cleanApkExtension.useSingleTask) {
                appExtension.applicationVariants.all {
                    createTask(project, it)
                }
            } else {
                appExtension.applicationVariants.all {variant ->
                    val packageTask = variant.packageApplicationProvider.get()
                    packageTask.doLast {
                        CleanApkHelper.doCleanApk(variant, project)
                    }
                }
            }
        }
    }

    private fun createTask(project: Project, variant: ApplicationVariant) {
        val action = CleanApkTask.CreateAction(variant)
        if (project.tasks.findByName(action.name) == null) {
            println("create task ${action.name}")
            project.tasks.register(action.name, action.type, action)
        }
    }

    private fun log(msg: String) {
        project.logger.info("$LOG_TAG: $msg")
    }
}