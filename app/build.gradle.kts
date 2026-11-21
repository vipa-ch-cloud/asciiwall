import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.asciiwall.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.asciiwall.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.2"
    }
    val ksProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    fun sign(key: String, env: String): String? = ksProps.getProperty(key) ?: System.getenv(env)
    val ksFile = sign("storeFile", "ASCIIWALL_STORE_FILE")
    val ksStorePass = sign("storePassword", "ASCIIWALL_STORE_PASSWORD")
    val ksAlias = sign("keyAlias", "ASCIIWALL_KEY_ALIAS")
    val ksKeyPass = sign("keyPassword", "ASCIIWALL_KEY_PASSWORD")
    val canSign = ksFile != null && ksStorePass != null && ksAlias != null && ksKeyPass != null &&
        rootProject.file(ksFile).exists()
    signingConfigs {
        if (canSign) {
            create("personal") {
                storeFile = rootProject.file(ksFile!!)
                storePassword = ksStorePass
                keyAlias = ksAlias
                keyPassword = ksKeyPass
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (canSign) signingConfig = signingConfigs.getByName("personal")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true }
    lint { abortOnError = false }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-core")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
