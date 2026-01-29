import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension // Возможно, потребуется

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
}


android {
    namespace = "com.example.monetto"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.monetto"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // --- ИСПРАВЛЕНИЕ ОШИБКИ API 26: Core Library Desugaring ---
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // ЭТО ВАЖНО: Включаем поддержку Java 8+ API (java.time) для старых версий Android (minSdk 24)
        isCoreLibraryDesugaringEnabled = true
    }
    // -----------------------------------------------------------
        kotlinOptions {
            jvmTarget = "17"
        }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"

    }
}

dependencies {
    // --- ИСПРАВЛЕНИЕ ОШИБКИ API 26: Core Library Desugaring ---
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    // ----------------------------------------------------------------------------------------

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.activity:activity-compose:1.11.0")

    // Compose BOM - устанавливает рекомендуемые версии
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))

    // Compose Material3 - ВАЖНО для DropdownMenuItemDefaults
    // Это должно быть включено, если вы используете компоненты Material3, такие как Dropdown, Card, TopAppBar и т.д.
    implementation("androidx.compose.material3:material3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // DataStore и ViewModel Lifecycle
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("androidx.compose.material:material-icons-extended")

    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Инструменты для запуска тестов на Android
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
