plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group   = "dev.n1xend"
version = "1.2.2"
description = "Dynamic supply & demand economy plugin for Paper 1.21.1"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    // ── Server API ────────────────────────────────────────────────────────────
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")

    // ── Optional integrations ─────────────────────────────────────────────────
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9") {
        exclude(group = "com.sk89q.worldedit", module = "worldedit-libs")
    }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.1")

    // ── Bundled ───────────────────────────────────────────────────────────────
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")   // SQLite driver
    implementation("org.jetbrains:annotations:24.1.0")

    // ── Test ──────────────────────────────────────────────────────────────────
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation("org.apache.commons:commons-lang3:3.17.0")
    constraints {
        testImplementation("junit:junit:4.13.2") { because("CVE-2020-15250") }
    }
}

// ── Fat JAR ───────────────────────────────────────────────────────────────────
tasks.jar {
    archiveFileName.set("${project.name}-${project.version}.jar")
    isZip64 = true
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })
    exclude(
        "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA",
        "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/MANIFEST.MF",
        "module-info.class", "META-INF/versions/**"
    )
}

tasks.processResources {
    val props = mapOf(
        "version"     to version,
        "description" to description,
        "author"      to "n1xend"
    )
    inputs.properties(props)
    filesMatching("paper-plugin.yml") { expand(props) }
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}

tasks.runServer {
    minecraftVersion("1.21.1")
}
