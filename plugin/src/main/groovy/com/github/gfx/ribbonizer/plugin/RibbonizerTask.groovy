package com.github.gfx.ribbonizer.plugin

import com.android.build.gradle.api.ApplicationVariant
import com.android.builder.model.SourceProvider
import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.awt.image.BufferedImage
import java.util.function.Consumer

@CompileStatic
class RibbonizerTask extends DefaultTask {
    static final String NAME = "ribbonize"

    ApplicationVariant variant

    //@OutputDirectory
    File outputDir

    Set<String> iconNames

    List<Consumer<BufferedImage>> filters = []

    @TaskAction
    public void run() {
        if (filters.size() == 0) {
            return;
        }

        def t0 = System.currentTimeMillis()

        def names = new HashSet<String>(iconNames)
        names.add(launcherIconName)

        variant.sourceSets.forEach { SourceProvider sourceSet ->
            sourceSet.resDirectories.forEach { File resDir ->
                if (resDir == outputDir) {
                    return
                }

                names.forEach { String name ->
                    project.fileTree(
                            dir: resDir,
                            includes: [
                                    "drawable*/${name}.png",
                                    "mipmap*/${name}.png",
                            ]
                    ).forEach { File inputFile ->
                        info "process $inputFile"

                        def basename = inputFile.name
                        def resType = inputFile.parentFile.name
                        def outputFile = new File(outputDir, "${resType}/${basename}")
                        outputFile.parentFile.mkdirs()

                        def ribbonizer = new Ribbonizer(inputFile, outputFile)
                        ribbonizer.process(filters)
                        ribbonizer.save()
                    }
                }
            }
        }

        info("${this.name} ${System.currentTimeMillis() - t0}ms")
    }

    public void info(String message) {
        System.out.println("[$name] $message")
        project.logger.info("[$name] $message")
    }

    String getLauncherIconName() {
         def manifestXml = new XmlSlurper().parse(androidManifestFile)
         def applicationNode = manifestXml.getProperty('application') as GPathResult
         def iconDrawable = applicationNode.getProperty('@android:icon') as String // "@drawable/ic_launcher"
        return iconDrawable.split('/')[1]
    }

     File getAndroidManifestFile() {
        return new File(project.buildDir,
                "intermediates/manifests/full/${variant.flavorName}/${variant.buildType.name}/AndroidManifest.xml");
    }
}