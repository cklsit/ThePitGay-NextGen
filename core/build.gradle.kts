import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.Scanner
import org.apache.tools.ant.filters.ReplaceTokens
import proguard.gradle.ProGuardTask
import java.io.ByteArrayOutputStream
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.7.0")
    }
}
plugins {
    kotlin("plugin.lombok")
    id("io.freefair.lombok")
    kotlin("jvm")
    alias(libs.plugins.shadow)
}

var devBuild = true
if (devBuild) {
    println("WARN! 当前使用DevBuild模式构建!!,请详细斟酌是否构建")
}
group = "cn.klee"
version = rootProject.extra["gitVersionString"].toString()
repositories {

    mavenCentral()
    maven("https://maven.cleanroommc.com")
    maven("https://maven.aliyun.com/repository/public/")
    maven("https://repo.crazycrew.us/releases")
    maven("https://repo.codemc.io/repository/nms/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.inventivetalent.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.panda-lang.org/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
}
val gitVersion = version.toString()

val injectGitVersion by tasks.registering {
    group = "versioning"
    description = "Injects Git version into source code before compilation."


    val inputDirs = listOf("src/main/kotlin", "src/main/java")
    val outputDir = file("build/generated/gitProcessed")

    outputs.dir(outputDir)

    doLast {
        delete(outputDir)
        inputDirs.forEach { srcDir ->
            fileTree(srcDir).matching {
                include("**/*.kt")
            }.forEach { srcFile ->
                val relativePath = srcFile.relativeTo(file(srcDir))
                val targetFile = outputDir.resolve(relativePath)
                targetFile.parentFile.mkdirs()

                val content = srcFile.readText()
                val replaced = content.replace("%git_version%", gitVersion)
                targetFile.writeText(replaced)
            }
        }
        println("🔄 Git version injected into generated sources.")
    }
}
tasks.register<ProGuardTask>("proguard") {
    configuration(file("proguard.pro"))

    injars(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    if (System.getProperty("java.version").startsWith("1.")) {
        libraryjars(file("${System.getProperty("java.home")}/lib/rt.jar"))
    } else {
        libraryjars(file("${System.getProperty("java.home")}/jmods/java.base.jmod"))
    }
    repositories
    libraryjars(configurations.compileClasspath)

    verbose()

    outjars(layout.buildDirectory.file("libs/proguard-tpu-minified.jar"))
}
val lastFin by tasks.registering {
    group = "versioning"
    description = "Injects Git version into source code before compilation."


    val inputDirs = listOf("src/main/kotlin", "src/main/java")
    val outputDir = file("build/generated/gitProcessed")

    outputs.dir(outputDir)

    doLast {
        delete(outputDir)
        inputDirs.forEach { srcDir ->
            fileTree(srcDir).matching {
                include("**/*.kt")
            }.forEach { srcFile ->
                val relativePath = srcFile.relativeTo(file(srcDir))
                val targetFile = outputDir.resolve(relativePath)
                targetFile.parentFile.mkdirs()

                val content = srcFile.readText()
                val replaced = content.replace(gitVersion, "%git_version%")
                targetFile.writeText(replaced)
            }
        }
        println("🔄 Git version restored into generated sources.")
    }
}
tasks.withType<JavaCompile>().configureEach {
    dependsOn("injectGitVersion")

    finalizedBy(lastFin)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn("injectGitVersion")

    finalizedBy(lastFin)
}
// Java 编译任务（如果你启用了 Java 插件）

tasks.named<ShadowJar>("shadowJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("ThePitUltimate-$version" + (if (devBuild) "-dev" else "") + ".jar")
    exclude("META-INF/**")
    relocate("panda", "net.mizukilab.pit.libs")
    relocate("dev.rollczi", "net.mizukilab.pit.libs")
    relocate("cn.hutool", "net.mizukilab.pit.libs")
    relocate("net.kyori", "net.mizukilab.pit.libs")
    relocate("net.jodah", "net.mizukilab.pit.libs")
    relocate("net.jitse", "net.mizukilab.pit.libs")
    relocate("xyz.upperlevel.spigot", "net.mizukilab.pit.libs")
    relocate("org.mongojack", "net.mizukilab.pit.libs.org.mongojack")
    relocate("com.fasterxml.jackson", "net.mizukilab.pit.libs.com.fasterxml.jackson")
    relocate("com.mongodb", "net.mizukilab.pit.libs.com.mongodb")
    relocate("org.bson", "net.mizukilab.pit.libs.org.bson")
    relocate("de.undercouch.bson4jackson", "net.mizukilab.pit.libs.de.undercouch.bson4jackson")
    if (!devBuild) {
        exclude("org/**")
    }

    exclude("kotlin/**", "junit/**", "org/junit/**")
    from("build/tmp/processed-resources")
    mergeServiceFiles()
}
dependencies {
    var dependencyNotation = project(":base")
    compileOnly(dependencyNotation)
    if (devBuild) {
        implementation(dependencyNotation)
    }
    compileOnly(fileTree(mapOf("dir" to "../libs", "include" to listOf("*.jar"))))
    if (devBuild) {
        api(libs.reflectionhelper)
    }else{
        compileOnly(libs.reflectionhelper)
    }
    compileOnly(libs.hutool.crypto)
    compileOnly(libs.book)
    compileOnly(libs.slf4j)
    compileOnly(libs.litecommands)
    compileOnly("com.caoccao.javet:javet:3.1.4")
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    compileOnly(libs.luckperms)
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")
    compileOnly("com.github.f4b6a3:uuid-creator:6.0.0")
    compileOnly(libs.papi)
    compileOnly(libs.narshorn)
    compileOnly(libs.protocollib)
    compileOnly(libs.jedis)
    implementation("org.mongojack:mongojack:5.0.1")
    implementation("org.mongodb:mongodb-driver-sync:5.2.0")

    compileOnly(fileTree("libs"))

    compileOnly(libs.fastutil)

    compileOnly("us.crazycrew.crazycrates:api:0.7")
    compileOnly(libs.paper.api)
    compileOnly(libs.luckperms)
    compileOnly(libs.playerpoints)
    compileOnly(libs.decentholograms)
    implementation(kotlin("reflect"))
}
kotlin {
    jvmToolchain(21)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "6000", "-Xlint:none"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.register("printCp") {
    doLast { println("CP=" + sourceSets["main"].compileClasspath.asPath) }
}
