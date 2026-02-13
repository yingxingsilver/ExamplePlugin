import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml
import java.util.*

plugins {
  `java-library`
  id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
  id("xyz.jpenilla.run-paper") version "3.0.2"
  id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.0"
  `maven-publish`
}

// === 版本策略：CI 中优先使用 -Pversion，本地开发用 SNAPSHOT ===
val gitTag: String? = try {
  ProcessBuilder("git", "describe", "--tags", "--exact-match")
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.DISCARD)
    .start()
    .inputStream
    .bufferedReader()
    .readText()
    .trim()
    .replace(Regex("^v"), "")
} catch (e: Exception) {
  null
}

version = project.findProperty("version")?.toString() // 优先：-Pversion
  ?: System.getenv("VERSION")
  ?: gitTag
  ?: "1.0.0-SNAPSHOT" // 本地开发默认值

group = "top.rainmc.testplugin"
description = "Test plugin for paperweight-userdev"

java {
  toolchain.languageVersion = JavaLanguageVersion.of(21)
}

dependencies {
  paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
}

tasks {
  compileJava {
    options.release = 21
    options.encoding = Charsets.UTF_8.name()
  }
  javadoc {
    options.encoding = Charsets.UTF_8.name()
  }

  // === 智能版本校验：允许快照格式（含提交哈希），禁止纯 SNAPSHOT ===
  register("checkReleaseVersion") {
    doLast {
      val ver = project.version.toString()
      // 允许: 1.0.0-abc123, 1.2.3-20240520.123456
      // 禁止: 1.0.0-SNAPSHOT
      require(!ver.contains("-SNAPSHOT", ignoreCase = true)) {
        "❌ 禁止发布纯 SNAPSHOT 版本: $ver。请使用带提交哈希的快照格式（如 1.0.0-abc123）"
      }
      logger.lifecycle("✅ 版本校验通过: $ver")
    }
  }

  named("publish") {
    dependsOn("checkReleaseVersion")
  }
}

bukkitPluginYaml {
  main = "top.rainmc.testplugin.ExamplePlugin"
  load = BukkitPluginYaml.PluginLoadOrder.STARTUP
  authors.add("YingXingSilver")
  apiVersion = "1.21.10"
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = project.group.toString()
      artifactId = project.name
      version = project.version.toString()

      artifact(tasks.named("reobfJar").get()) {
        classifier = ""
      }

      pom {
        name = project.name
        description = project.description
        url = "https://github.com/yingxingsilver/ExamplePlugin" // ✅ 无空格

        licenses {
          license {
            name = "GNU General Public License v3.0"
            url = "https://www.gnu.org/licenses/gpl-3.0.html"
          }
        }
        developers {
          developer {
            id = "yingxingsilver"
            name = "YingXingSilver"
            email = "1@rainmc.top"
          }
        }
        scm {
          connection = "scm:git:https://github.com/yingxingsilver/ExamplePlugin.git"
          developerConnection = "scm:git:ssh://git@github.com:yingxingsilver/ExamplePlugin.git"
          url = "https://github.com/yingxingsilver/ExamplePlugin"
        }
      }
    }
  }

  repositories {
    maven {
      name = "GitHubPackages"
      // ✅ 修复：移除所有 URL 尾部空格！
      url = uri("https://maven.pkg.github.com/yingxingsilver/ExamplePlugin")
      credentials {
        username = System.getenv("GITHUB_ACTOR") ?: "yingxingsilver"
        password = System.getenv("GITHUB_TOKEN") ?: ""
      }
    }
  }
}