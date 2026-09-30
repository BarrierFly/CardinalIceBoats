import masecla.modrinth4j.model.version.ProjectVersion.*

val cloth_config_version: String by project

repositories {
    maven ("https://maven.shedaniel.me/")
}

dependencies {
    modImplementation ("me.shedaniel.cloth:cloth-config-fabric:$cloth_config_version")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

plugins {
    alias(libs.plugins.easymod)
}

easyMod {
    enableShadow = false
    enableProGuard = false
    jarPostProcessConfig = {
        advzipArguments = listOf("-4", "-z", "-i", "100")
    }
}

// Cross-version shared code lives in platforms/shared-pre26 (Yarn mappings).
// easymod adds its default source dirs in afterEvaluate, so ours are (re-)added
// there as well; the per-platform src/shared trees were retired in favour of
// the shared modules.
afterEvaluate {
    sourceSets {
        main {
            kotlin.srcDir(rootProject.file("platforms/shared-pre26/main/kotlin"))
            java.srcDir(rootProject.file("platforms/shared-pre26/main/java"))
            resources.srcDir(rootProject.file("platforms/shared-pre26/main/resources"))
        }
        test {
            kotlin.srcDir(rootProject.file("platforms/shared-pre26/test/kotlin"))
        }
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

afterEvaluate {
    modrinth {
        failSilently.set(true)
        if (System.getenv("IPNEXT_RELEASE") != null) {
            token.set(System.getenv("MODRINTH_TOKEN"))
        }
        versionType.set(VersionType.RELEASE.name)
    }
}
