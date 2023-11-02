// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

import cc.tweaked.gradle.*
import net.neoforged.gradle.common.extensions.ProjectHolderExtension
import net.neoforged.gradle.common.runs.run.RunImpl
import net.neoforged.gradle.common.util.TaskDependencyUtils
import net.neoforged.gradle.dsl.common.extensions.ProjectHolder
import net.neoforged.gradle.dsl.common.extensions.RunnableSourceSet

plugins {
    id("cc-tweaked.forge")
    id("cc-tweaked.gametest")
    id("cc-tweaked.illuaminate")
    id("cc-tweaked.mod-publishing")
}

val modVersion: String by extra

val allProjects = listOf(":core-api", ":core", ":forge-api").map { evaluationDependsOn(it) }
cct {
    inlineProject(":common")
    allProjects.forEach { externalSources(it) }
}

sourceSets {
    main {
        resources.srcDir("src/generated/resources")
    }
}

minecraft {
    accessTransformers {
        file("src/main/resources/META-INF/accesstransformer.cfg")
    }
}

jarJar {

}

runs {
    configureEach {
        systemProperty("forge.logging.markers", "REGISTRIES")
        systemProperty("forge.logging.console.level", "debug")

        cct.sourceDirectories.get().forEach {
            if (it.classes) {
                if (it.sourceSet.extensions.findByType<RunnableSourceSet>() == null) {
                    it.sourceSet.extensions.create<RunnableSourceSet>(RunnableSourceSet.NAME, project)
                }

                modSource(it.sourceSet)
            }
        }
    }

    val client by registering {
        workingDirectory(file("run"))
    }

    val server by registering {
        workingDirectory(file("run/server"))
        programArgument("--nogui")
    }

    val data by registering {
        workingDirectory(file("run"))
        programArguments.addAll(
            "--mod", "computercraft", "--all",
            "--output", file("src/generated/resources/").absolutePath,
            "--existing", project(":common").file("src/main/resources/").absolutePath,
            "--existing", file("src/main/resources/").absolutePath,
        )
        systemProperty("cct.pretty-json", "true")
    }
    /*
            fun RunConfig.configureForGameTest() {
                val old = lazyTokens["minecraft_classpath"]
                lazyToken("minecraft_classpath") {
                    // We do some terrible hacks here to basically find all things not already on the runtime classpath
                    // and add them. /Except/ for our source sets, as those need to load inside the Minecraft classpath.
                    val testMod = configurations["testModRuntimeClasspath"].resolve()
                    val implementation = configurations.runtimeClasspath.get().resolve()
                    val new = (testMod - implementation)
                        .asSequence()
                        .filter { it.isFile && !it.name.endsWith("-test-fixtures.jar") }
                        .map { it.absolutePath }
                        .joinToString(File.pathSeparator)

                    val oldVal = old?.get()
                    if (oldVal.isNullOrEmpty()) new else oldVal + File.pathSeparator + new
                }

                property("cctest.sources", project(":common").file("src/testMod/resources/data/cctest").absolutePath)

                arg("--mixin.config=computercraft-gametest.mixins.json")

                mods.register("cctest") {
                    source(sourceSets["testMod"])
                    source(sourceSets["testFixtures"])
                }
            }

            val testClient by registering {
                workingDirectory(file("run/testClient"))
                parent(client.get())
                configureForGameTest()

                property("cctest.tags", "client,common")
            }

            val gameTestServer by registering {
                workingDirectory(file("run/testServer"))
                configureForGameTest()

                property("forge.logging.console.level", "info")
                jvmArg("-ea")
            }
        }
     */
}

/*mixin {
    config("computercraft.mixins.json")
    config("computercraft-client.mixins.json")
    config("computercraft-client.forge.mixins.json")
}*/

configurations {
    register("cctJavadoc")
    // minecraftLibrary { extendsFrom(minecraftEmbed.get()) }
}

dependencies {
    compileOnly(libs.jetbrainsAnnotations)
    annotationProcessorEverywhere(libs.autoService)

    clientCompileOnly(variantOf(libs.emi) { classifier("api") })
    compileOnly(libs.bundles.externalMods.forge.compile)
    runtimeOnly(libs.bundles.externalMods.forge.runtime)

    // Depend on our other projects.
    api(commonClasses(project(":forge-api")))
    api(clientClasses(project(":forge-api")))
    implementation(project(":core"))

    /*
    minecraftEmbed(libs.cobalt) {
        jarJar.ranged(this, "[${libs.versions.cobalt.asProvider().get()},${libs.versions.cobalt.next.get()})")
    }
    minecraftEmbed(libs.jzlib) {
        jarJar.ranged(this, "[${libs.versions.jzlib.get()},)")
    }
    minecraftEmbed(libs.netty.http) {
        jarJar.ranged(this, "[${libs.versions.netty.get()},)")
        isTransitive = false
    }
    minecraftEmbed(libs.netty.socks) {
        jarJar.ranged(this, "[${libs.versions.netty.get()},)")
        isTransitive = false
    }
    minecraftEmbed(libs.netty.proxy) {
        jarJar.ranged(this, "[${libs.versions.netty.get()},)")
        isTransitive = false
    }*/

    testFixturesApi(libs.bundles.test)
    testFixturesApi(libs.bundles.kotlin)

    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.testRuntime)

    testModImplementation(testFixtures(project(":core")))
    testModImplementation(testFixtures(project(":forge")))

    testFixturesImplementation(testFixtures(project(":core")))

    "cctJavadoc"(libs.cctJavadoc)
}

illuaminate {
    version.set(libs.versions.illuaminate)
}

// Compile tasks

val luaJavadoc by tasks.registering(Javadoc::class) {
    description = "Generates documentation for Java-side Lua functions."
    group = JavaBasePlugin.DOCUMENTATION_GROUP

    source(sourceSets.main.get().java)
    source(project(":core").sourceSets.main.get().java)
    source(project(":common").sourceSets.main.get().java)

    destinationDir = layout.buildDirectory.dir("docs/luaJavadoc").get().asFile
    classpath = sourceSets.main.get().compileClasspath

    val options = options as StandardJavadocDocletOptions
    options.docletpath = configurations["cctJavadoc"].files.toList()
    options.doclet = "cc.tweaked.javadoc.LuaDoclet"
    options.addStringOption("project-root", rootProject.file(".").absolutePath)
    options.noTimestamp(false)

    javadocTool.set(
        javaToolchains.javadocToolFor {
            languageVersion.set(cc.tweaked.gradle.CCTweakedPlugin.JAVA_VERSION)
        },
    )
}

tasks.processResources {
    inputs.property("modVersion", modVersion)
    inputs.property("neoVersion", libs.versions.neoForge.get())

    filesMatching("META-INF/mods.toml") {
        expand(mapOf("neoVersion" to libs.versions.neoForge.get(), "file" to mapOf("jarVersion" to modVersion)))
    }
}

tasks.jar {
    archiveClassifier.set("slim")

    for (source in cct.sourceDirectories.get()) {
        if (source.classes && source.external) from(source.sourceSet.output)
    }
}

tasks.sourcesJar {
    for (source in cct.sourceDirectories.get()) from(source.sourceSet.allSource)
}

tasks.jarJar {
    archiveClassifier.set("")

    for (source in cct.sourceDirectories.get()) {
        if (source.classes) from(source.sourceSet.output)
    }
}

tasks.assemble { dependsOn("jarJar") }

// Check tasks

tasks.test {
    systemProperty("cct.test-files", layout.buildDirectory.dir("tmp/testFiles").getAbsolutePath())
}

val lintLua by tasks.registering(IlluaminateExec::class) {
    group = JavaBasePlugin.VERIFICATION_GROUP
    description = "Lint Lua (and Lua docs) with illuaminate"

    // Config files
    inputs.file(rootProject.file("illuaminate.sexp")).withPropertyName("illuaminate.sexp")
    // Sources
    inputs.files(rootProject.fileTree("doc")).withPropertyName("docs")
    inputs.files(project(":core").fileTree("src/main/resources/data/computercraft/lua")).withPropertyName("lua rom")
    inputs.files(luaJavadoc)

    args = listOf("lint")
    workingDir = rootProject.projectDir

    doFirst { if (System.getenv("GITHUB_ACTIONS") != null) println("::add-matcher::.github/matchers/illuaminate.json") }
    doLast { if (System.getenv("GITHUB_ACTIONS") != null) println("::remove-matcher owner=illuaminate::") }
}

/*
val runGametest by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs tests on a temporary Minecraft instance."
    dependsOn("cleanRunGametest")
    usesService(MinecraftRunnerService.get(gradle))

    setRunConfig(minecraft.runs["gameTestServer"])

    systemProperty("cctest.gametest-report", layout.buildDirectory.dir("test-results/$name.xml").getAbsolutePath())
}
cct.jacoco(runGametest)
tasks.check { dependsOn(runGametest) }

val runGametestClient by tasks.registering(ClientJavaExec::class) {
    description = "Runs client-side gametests with no mods"
    setRunConfig(minecraft.runs["testClient"])
    tags("client")
}
cct.jacoco(runGametestClient)

tasks.register("checkClient") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs all client-only checks."
    dependsOn(runGametestClient)
}
*/
// Upload tasks

modPublishing {
    output.set(tasks.jarJar)
}

// Don't publish the slim jar
for (cfg in listOf(configurations.apiElements, configurations.runtimeElements)) {
    cfg.configure { artifacts.removeIf { it.classifier == "slim" } }
}

publishing {
    publications {
        named("maven", MavenPublication::class) {
            // jarJar.component is broken (https://github.com/MinecraftForge/ForgeGradle/issues/914), so declare the
            // artifact explicitly.
            // artifact(tasks.jarJar)

            mavenDependencies {
                exclude(dependencies.create("cc.tweaked:"))
                exclude(libs.jei.forge.get())
            }
        }
    }
}
