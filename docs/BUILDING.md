# 构建与测试

项目使用 Gradle Wrapper 8.5；Java/Kotlin 工具链 **JDK 21**（Paper 1.21.8 要求）。Gradle 本身可用 JDK 17 启动，工具链会自行解析 JDK 21（`org.gradle.toolchains.foojay-resolver-convention`）。首次构建需要访问依赖仓库；`--offline` 仅适用于依赖已缓存的环境。

MythicNPCLib 与 MythicFramework 已随源码内置：`cn.mythicland.npclib` 在 `core/src/main/java`，MythicFramework 在 `vendor/mythic-framework/`（当前只编译 `math`、`path`、`task`、`bukkit/world` 四个包）。无需安装 Citizens。

## NMS 编译类路径

`io.papermc.paper:paper-api` **不包含** `net.minecraft.*` 与 `org.bukkit.craftbukkit.*`。仓库里仍有 NMS 代码（Bot、NPC 发包层、物品/告示牌等），编译这些文件需要真实服务端 jar：

1. 用 Paper 1.21.8 的 paperclip jar 只做解包（不启动服务器）：

   ```powershell
   java "-Dpaperclip.patchonly=true" -jar paper-1.21.8-<build>.jar
   ```

   首次运行会下载 Mojang 原版 jar，解包出 `versions/1.21.8/paper-1.21.8.jar` 与 `libraries/`。

2. 把这两部分放进 `<仓库>/.gradle/paper-nms/`，结构保持：

   ```
   .gradle/paper-nms/versions/1.21.8/paper-1.21.8.jar
   .gradle/paper-nms/libraries/**/*.jar
   ```

   `.gradle/` 已在 `.gitignore` 中，不会入库。也可用 `-PpaperNmsRoot=<目录>` 指向别处。

3. `core/build.gradle.kts` 会把该目录下 `versions/**/*.jar` 与 `libraries/**/*.jar` 挂到 `compileOnly`。
   目录缺失时构建不会报错，但 NMS 文件会以 `package net.minecraft.server does not exist` 失败。

编译期不需要把服务端 jar 打包进产物；运行时这些类由服务端提供。

## 本地依赖

| 文件 | 位置 | 说明 |
| --- | --- | --- |
| Paper 1.21.8 服务端 + 运行库 | `.gradle/paper-nms/` | 见上节，NMS 编译必需 |
| `libs/*.jar` | 仓库根 `libs/` | Citizens / WorldEdit / FAWE / MythicMobs 等，`compileOnly` |
| `packLib/magic-license-1.1.3.jar` | 仓库内 | 授权校验库，`implementation`，shadowJar 会 relocate 到 `net.mizukilab.pit.license` |

## 构建核心

Windows：

```powershell
.\gradlew.bat :core:shadowJar --no-daemon --console=plain
```

Linux/macOS 使用 `./gradlew`。输出为 `core/build/libs/ThePitGay-V.jar`。此命令仅构建核心，不执行测试。

只看编译：

```powershell
.\gradlew.bat :core:compileJava --console=plain --no-daemon
```

Java 编译器默认用中文输出，日志在窄终端里会被折行截断；需要可读日志时先设 `JAVA_TOOL_OPTIONS='-Duser.language=en -Duser.country=US'`。

## 完整回归测试

完整测试会加载扩展真实二进制，需要额外提供以下测试依赖；扩展不随测试源码分发：

| 扩展 | 默认路径 | 可覆盖参数 |
| --- | --- | --- |
| ThePitRepairExTend 4.0.0 | `test-fixtures/ThePitRepairExTend-4.0.0.jar` | `-PrepairExtendJar=/path/to/repair.jar` |
| ThePitRepairExTend 5.3.0（原生 Bot 类型检查） | `test-fixtures/ThePitRepairExTend-5.3.0.jar` | `-PnativeRepairJar=/path/to/repair-5.3.0.jar` |
| ThePitAddon 6.7 Server | `test-fixtures/ThePitAddon-MistralNetWork-Server-6.7-SNAPSHOT.jar` | `-PpitAddonJar=/path/to/addon.jar` |
| ThePitAddon 6.7 Loader | `test-fixtures/ThePitAddon-MistralNetWork-Loader-6.7-SNAPSHOT.jar` | `-PpitAddonLoaderJar=/path/to/loader.jar` |
| NPCView 1.0 | `test-fixtures/NPCView-1.0-SNAPSHOT.jar` | `-PnpcViewJar=/path/to/npcview.jar` |

```powershell
.\gradlew.bat :core:build --no-daemon --console=plain
```

相对路径均以仓库根目录解析，也可传入绝对路径。HTML 测试报告位于 `core/build/reports/tests/test/index.html`，XML 位于 `core/build/test-results/test/`。

> 已知缺口：`:core:test` 的 classpath 仍固定引用 1.8 的 `libs/pandaspigot-221.jar` 与 `core/libs/spigot-1.8.8-R0.1-SNAPSHOT.jar`，测试源码里也有 1.8 NMS 断言（如 `MythicBotIdentityTest`）。回归测试要等 NMS 清理完成后再一起迁移，当前以 `compileJava` 与实服冒烟为准。测试中的 Bukkit 调度、数据库等边界部分使用模拟对象，测试通过不能替代实服多人压测。
