import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.Scanner
import org.apache.tools.ant.filters.ReplaceTokens
import proguard.gradle.ProGuardTask
import java.io.ByteArrayOutputStream
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
buildscript {
    repositories {
    mavenCentral()
    maven("https://maven.cleanroommc.com")
    maven("https://repo.codemc.io/repository/nms/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://jitpack.io")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.panda-lang.org/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
}
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.7.0")
        classpath("org.ow2.asm:asm:9.7.1")
    }
}
plugins {
    kotlin("plugin.lombok") version "2.1.20"
    id("io.freefair.lombok") version "8.10"
    kotlin("jvm") version "2.1.20"
    alias(libs.plugins.shadow)
}

group = "top.mcrw"

version = "V"
repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-public/")
    exclusiveContent {
        forRepository { maven("https://repo.panda-lang.org/releases") }
        filter { includeGroup("dev.rollczi") }
    }
    exclusiveContent {
        forRepository { mavenCentral() }
        filter {
            includeGroup("junit")
            includeGroup("org.hamcrest")
            includeGroup("org.mockito")
            includeGroup("net.bytebuddy")
            includeGroup("org.objenesis")
        }
    }
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public/") {
        content {
            includeGroupByRegex(".*")
        }
    }
    maven("https://maven.cleanroommc.com")
    maven("https://repo.crazycrew.us/releases")
    maven("https://repo.codemc.io/repository/nms/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.inventivetalent.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}
val gitVersion = rootProject.extra["gitVersionString"].toString()

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

tasks.named<ShadowJar>("shadowJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveFileName.set("ThePitGay-$version" + ".jar")
    exclude("META-INF/**")

    exclude("org.hamcrest")

    exclude("org.intellij")

    exclude("org.jetbrains")
    relocate("pku.yim.license", "net.mizukilab.pit.license")
    relocate("panda", "net.mizukilab.pit.libs")
    relocate("dev.rollczi", "net.mizukilab.pit.libs")
    relocate("cn.hutool", "net.mizukilab.pit.libs")
    // 注意：不要重定位 net.kyori —— Paper API 的方法签名用的是服务端自带的 adventure 类型，
    // 重定位后调用 PlayerJoinEvent#joinMessage 等会 NoSuchMethodError。
    relocate("net.jodah", "net.mizukilab.pit.libs")
    // Preserve the NPC API descriptors consumed by existing extensions.
    relocate("cn.mythicland.npclib", "net.mizukilab.pit.libs.npclib")
    relocate("cn.mythicland.framework", "net.mizukilab.pit.libs.mythicframework")
    relocate("org.reflections", "net.mizukilab.pit.libs.mythicframework.reflections")
    relocate("javassist", "net.mizukilab.pit.libs.mythicframework.javassist")
    relocate("com.google.gson", "net.mizukilab.pit.libs.mythicframework.gson")
    relocate("net.sourceforge.pinyin4j", "net.mizukilab.pit.libs.mythicframework.pinyin4j")
    relocate("org.yaml.snakeyaml", "net.mizukilab.pit.libs.mythicframework.yaml")
    relocate("xyz.upperlevel.spigot", "net.mizukilab.pit.libs")
    // 发包层统一走 PacketEvents，随核心 shade 进 jar 并重定位，避免与外部安装的 PacketEvents 冲突。
    relocate("com.github.retrooper.packetevents", "net.mizukilab.pit.libs.packetevents")
    relocate("io.github.retrooper.packetevents", "net.mizukilab.pit.libs.packetevents")
    // Kotlin 运行库必须打包：服务端不提供 kotlin/*，插件自身是 Kotlin 重代码库。
    exclude("junit/**", "org/junit/**")
    from("build/tmp/processed-resources")
    mergeServiceFiles()
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-inline:4.11.0")

    // P1：构建目标从 Spigot 1.8.8 切到 Paper 1.21.8
    compileOnly(libs.paper.api)
    // 收包/发包统一走 PacketEvents（shade 进产物），不再依赖 NMS。
    // PacketEvents 依赖 gson，而本项目把 com.google.gson 重定位成
    // net.mizukilab.pit.libs.mythicframework.gson，所以 gson 必须以 implementation
    // 打包进来，否则重定位后的引用会悬空（启动时 NoClassDefFoundError）。
    implementation(libs.packetevents)
    implementation("com.google.code.gson:gson:2.11.0")
    compileOnly(fileTree(mapOf("dir" to "../libs", "include" to listOf("*.jar"),
        "exclude" to listOf("citizens*.jar", "Citizens*.jar"))))
    // 授权校验库不再打包：其类整体混淆，会让 Paper 的 PluginRemapper 因重复字段名崩溃；
    // 代码只用到 nya.Skip 标记注解，已用 core/src/main/java/nya/Skip.java 内置等价定义。
    // Paper 1.21.8 服务端（Mojang 映射）与运行库，供 NMS 层编译；本地解包产物不入库。
    // 见 docs/BUILDING.md「NMS 编译类路径」。
    val paperNmsRoot = rootProject.file(
        providers.gradleProperty("paperNmsRoot").orElse(".gradle/paper-nms").get()
    )
    if (paperNmsRoot.isDirectory) {
        compileOnly(fileTree(paperNmsRoot) { include("versions/**/*.jar", "libraries/**/*.jar") })
    }
    implementation(libs.reflectionhelper)

    implementation(libs.hutool.crypto)
    implementation(libs.book)
    compileOnly(libs.slf4j)
    implementation(libs.litecommands)
    implementation("zone.rong:imaginebreaker:2.1")
    compileOnly(libs.adventure.bukkit)
    compileOnly("com.caoccao.javet:javet:3.1.4")
    // 运行时必需库随 jar 打包（单 jar 部署；不再用 ImagineBreaker 在启动阶段联网下载，
    // 重定位后其 Maven URL 会失效）。
    implementation("commons-lang:commons-lang:2.6")
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    compileOnly(libs.luckperms)
    compileOnly("com.github.f4b6a3:uuid-creator:6.0.0")
    compileOnly(libs.papi)
    compileOnly(libs.narshorn)
    compileOnly(libs.protocollib)
    implementation(libs.jedis)
    implementation("org.mongojack:mongojack:5.0.1")
    implementation("org.mongodb:mongodb-driver-sync:5.2.0")

    // 1.8.8 的 Citizens 依赖与 paper-api 冲突，编译期排除
    compileOnly(fileTree("libs") { exclude("citizens*.jar", "Citizens*.jar") })

    implementation(libs.fastutil)
    compileOnly("us.crazycrew.crazycrates:api:0.7")
    compileOnly(libs.luckperms)
    compileOnly(libs.playerpoints)
    compileOnly(libs.decentholograms)
    implementation(libs.adventure.bukkit)
    implementation(kotlin("reflect"))
}
sourceSets {
    main {
        java {
            // MythicFramework 以源码形式内置；当前构建只需其中的数学/路径/任务工具与网格寻路。
            srcDir(rootProject.file("vendor/mythic-framework/core/src/main/java/cn/mythicland/framework/math"))
            srcDir(rootProject.file("vendor/mythic-framework/core/src/main/java/cn/mythicland/framework/path"))
            srcDir(rootProject.file("vendor/mythic-framework/core/src/main/java/cn/mythicland/framework/task"))
            srcDir(rootProject.file("vendor/mythic-framework/bukkit/src/main/java/cn/mythicland/framework/bukkit/world"))
        }
    }
}
sourceSets.test {
    compileClasspath += configurations.compileClasspath.get()
    runtimeClasspath += configurations.compileClasspath.get()
}
tasks.test {
    useJUnit()
    // Keep the 1.8 server's bundled libraries ahead of raw framework dependencies.
    // Production isolates those dependencies through Shadow relocation.
    classpath = files(rootProject.file(providers.gradleProperty("pandaSpigotJar")
        .orElse("libs/pandaspigot-221.jar").get()), file("libs/spigot-1.8.8-R0.1-SNAPSHOT.jar")) + classpath
    // Binary integration tests need the same relocated libraries shipped to Bukkit.
    dependsOn(tasks.named("shadowJar"))
    val repairExtendJar = rootProject.file(providers.gradleProperty("repairExtendJar")
        .orElse("test-fixtures/ThePitRepairExTend-4.0.0.jar").get())
    inputs.files(repairExtendJar).withPropertyName("repairExtendJar")
    systemProperty("repairExtendJar", repairExtendJar.absolutePath)
    for ((property, path) in mapOf(
        "pitAddonJar" to "test-fixtures/ThePitAddon-MistralNetWork-Server-6.7-SNAPSHOT.jar",
        "pitAddonLoaderJar" to "test-fixtures/ThePitAddon-MistralNetWork-Loader-6.7-SNAPSHOT.jar",
        "npcViewJar" to "test-fixtures/NPCView-1.0-SNAPSHOT.jar"
    )) {
        val fixture = rootProject.file(providers.gradleProperty(property).orElse(path).get())
        inputs.files(fixture).withPropertyName(property)
        systemProperty(property, fixture.absolutePath)
    }
    val nativeRepairJar = rootProject.file(providers.gradleProperty("nativeRepairJar")
        .orElse("test-fixtures/ThePitRepairExTend-5.3.0.jar").get())
    inputs.files(nativeRepairJar).withPropertyName("nativeRepairJar")
    systemProperty("nativeRepairJar", nativeRepairJar.absolutePath)
}
kotlin {
    // Paper 1.21.8 要求 Java 21
    jvmToolchain(21)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.named<JavaCompile>("compileJava") {
    // JVM descriptors include the return type. Preserve both the old List getter
    // and the current fastutil Map getter; Java source cannot express this overload.
    doLast {
        val classFile = destinationDirectory.file("cn/charlotte/pit/data/LeaderBoardEntry.class").get().asFile
        val reader = ClassReader(classFile.readBytes())
        val writer = ClassWriter(reader, 0)
        var hasBridge = false
        reader.accept(object : org.objectweb.asm.ClassVisitor(Opcodes.ASM9, writer) {
            override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): org.objectweb.asm.MethodVisitor? {
                if (name == "getLeaderBoardEntries" && descriptor == "()Ljava/util/List;") hasBridge = true
                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }
            override fun visitEnd() {
                if (!hasBridge) {
                    val method = super.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC or Opcodes.ACC_BRIDGE,
                        "getLeaderBoardEntries", "()Ljava/util/List;", null, null)
                    method.visitCode()
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "cn/charlotte/pit/data/LeaderBoardEntry",
                        "getLegacyLeaderBoardEntries", "()Ljava/util/List;", false)
                    method.visitInsn(Opcodes.ARETURN)
                    method.visitMaxs(1, 0)
                    method.visitEnd()
                }
                super.visitEnd()
            }
        }, 0)
        classFile.writeBytes(writer.toByteArray())
    }
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
tasks.check {
    dependsOn(":mythic-framework-core:test", ":mythic-framework-bukkit:test")
}
