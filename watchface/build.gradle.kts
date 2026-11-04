plugins {
    alias(libs.plugins.android.application)
}

/** Placeholder values for src/main/watchface/watchface.xml, per edition (= product flavor). */
val editions = mapOf(
    "vier" to mapOf(
        "DIGIT" to "4",
        "TITLE_TOP" to "KEIN BIER",
        "TITLE_BOTTOM" to "VOR VIER",
        "TITLE_SIZE" to "40",
        "PROVIDER" to "de.irgendwoistimmervier/de.keinbiervorvier.wear.complication.KeinBierVorVierComplicationService",
        "LAUNCH" to "de.irgendwoistimmervier/de.keinbiervorvier.wear.VierActivity",
    ),
    "five" to mapOf(
        "DIGIT" to "5",
        "TITLE_TOP" to "IT'S 5 O'CLOCK",
        "TITLE_BOTTOM" to "SOMEWHERE",
        "TITLE_SIZE" to "34",
        "PROVIDER" to "de.irgendwoistimmervier/de.keinbiervorvier.wear.complication.FiveOClockComplicationService",
        "LAUNCH" to "de.irgendwoistimmervier/de.keinbiervorvier.wear.FiveActivity",
    ),
)

android {
    namespace = "de.keinbiervorvier.watchface"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }

    defaultConfig {
        // Watch Face Format needs Wear OS 4.
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    flavorDimensions += "edition"
    productFlavors {
        create("vier") {
            dimension = "edition"
            applicationId = "de.irgendwoistimmervier.watchface.vier"
        }
        create("five") {
            dimension = "edition"
            applicationId = "de.irgendwoistimmervier.watchface.five"
        }
    }
}

abstract class GenerateWatchFace : DefaultTask() {
    @get:InputFile
    abstract val template: RegularFileProperty

    @get:Input
    abstract val values: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        var xml = template.get().asFile.readText()
        values.get().forEach { (key, value) -> xml = xml.replace("{{$key}}", value) }
        check("{{" !in xml) { "Unfilled placeholder in watch face template" }
        val out = outputDir.file("raw/watchface.xml").get().asFile
        out.parentFile.mkdirs()
        out.writeText(xml)
    }
}

androidComponents {
    onVariants { variant ->
        val edition = variant.flavorName!!
        val task = tasks.register<GenerateWatchFace>("generate${variant.name.replaceFirstChar { it.uppercase() }}WatchFace") {
            template.set(layout.projectDirectory.file("src/main/watchface/watchface.xml"))
            // Debug watch apps carry a ".wear" suffix (see wear/build.gradle.kts).
            val watchApp = if (variant.buildType == "debug") "de.irgendwoistimmervier.wear" else "de.irgendwoistimmervier"
            values.set(editions.getValue(edition).mapValues { it.value.replace("de.irgendwoistimmervier/", "$watchApp/") })
        }
        variant.sources.res?.addGeneratedSourceDirectory(task, GenerateWatchFace::outputDir)
    }
}
