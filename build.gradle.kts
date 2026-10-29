plugins {
    id("java")
    id("com.gradleup.shadow") version("8.3.9")
    id("io.github.revxrsal.zapper") version("1.0.2")
    id("io.freefair.lombok") version("8.11")
    id("xyz.jpenilla.run-paper") version("3.1.0")
}

group = "com.playerpicked"
version = "1.0.0"

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    compileOnly("org.projectlombok:lombok:1.18.42")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude(group = "org.bukkit")
    }

    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.17") {
        exclude(module = "lamp.common")
        exclude(module = "lamp.brigadier")
    }
    zap("io.github.revxrsal:lamp.common:4.0.0-rc.17")
    zap("io.github.revxrsal:lamp.brigadier:4.0.0-rc.17")

    zap("com.github.Anon8281:UniversalScheduler:0.1.6")
    implementation("dev.dejvokep:boosted-yaml:1.3.6")

    zap("com.h2database:h2:2.3.232")
    zap("com.mysql:mysql-connector-j:9.1.0")
    zap("com.zaxxer:HikariCP:6.2.1")
}

lombok {
    version = "1.18.42"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.runServer {
    minecraftVersion("26.2")
    jvmArgs("-Xms2G", "-Xmx2G")
}

zapper {
    libsFolder = "libs"
    relocationPrefix = "com.playerpicked.clickableblocks"

    repositories {
        includeProjectRepositories()
    }

    relocate("com.github.Anon8281.universalScheduler", "universalScheduler")
}
