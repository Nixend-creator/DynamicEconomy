val Unit.FIRST: Any
val Unit.FIRST: DuplicatesStrategy
val Unit.FIRST: DuplicatesStrategy

plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "dev.n1xend"
version = "1.0.0"
description = "Dynamic supply & demand economy plugin for Paper 1.21.1"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains:annotations:24.0.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

// Fat JAR — собираем все implementation-зависимости прямо в плагинный JAR
// Без Shadow Plugin, совместимо с Gradle 9+
tasks.jar {
    archiveFileName.set("${project.name}-${project.version}.jar")

    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    }) {
        // Убираем подписи и лицензии из сторонних JAR
        exclude(
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
            "META-INF/MANIFEST.MF"
        )
        duplicatesStrategy = DuplicatesStrategy.FIRST
    }

    DuplicatesStrategy.FIRST.also { duplicatesStrategy = it }
}

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "description" to description,
        "author" to "n1xend"
    )
    inputs.properties(props)
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.runServer {
    minecraftVersion("1.21.1")
}
