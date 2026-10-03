import masecla.modrinth4j.model.version.ProjectVersion.*

val cloth_config_version: String by project

repositories {
    maven ("https://maven.shedaniel.me/")
}

dependencies {
    modImplementation ("me.shedaniel.cloth:cloth-config-fabric:$cloth_config_version")
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
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

// Cross-version shared code lives in platforms/shared-pre26 (Yarn mappings), and
// mapping-independent code in platforms/shared-pure.
// easymod adds its default source dirs in afterEvaluate, so ours are (re-)added
// there as well; the per-platform src/shared trees were retired in favour of
// the shared modules.
afterEvaluate {
    sourceSets {
        main {
            kotlin.srcDir(rootProject.file("platforms/shared-pre26/main/kotlin"))
            java.srcDir(rootProject.file("platforms/shared-pre26/main/java"))
            resources.srcDir(rootProject.file("platforms/shared-pre26/main/resources"))
            kotlin.srcDir(rootProject.file("platforms/shared-pure/main/kotlin"))
        }
        test {
            kotlin.srcDir(rootProject.file("platforms/shared-pure/test/kotlin"))
        }
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
    modrinth {
        failSilently.set(true)
        if (System.getenv("IPNEXT_RELEASE") != null) {
            token.set(System.getenv("MODRINTH_TOKEN"))
        }
        versionType.set(VersionType.RELEASE.name)
    }
}
