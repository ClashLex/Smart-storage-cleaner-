plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.smartcleaner.xjpzqd"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    val apiBaseUrl = (project.findProperty("API_BASE_URL") as String?)
      ?: "http://10.0.2.2:3000/"
    val googleWebClientId = (project.findProperty("GOOGLE_WEB_CLIENT_ID") as String?) ?: ""
    buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH")
      val storePasswordValue = System.getenv("STORE_PASSWORD")
      val keyPasswordValue = System.getenv("KEY_PASSWORD")
      val aliasValue = System.getenv("KEY_ALIAS")
      if (!keystorePath.isNullOrBlank() && !storePasswordValue.isNullOrBlank() &&
          !keyPasswordValue.isNullOrBlank() && !aliasValue.isNullOrBlank()) {
        storeFile = file(keystorePath)
        storePassword = storePasswordValue
        keyAlias = aliasValue
        keyPassword = keyPasswordValue
      }
    }
  }

  buildTypes {
    debug {
      // Use Gradle's standard debug signing configuration.
    }
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val signingReady = listOf("KEYSTORE_PATH", "STORE_PASSWORD", "KEY_PASSWORD", "KEY_ALIAS")
        .all { !System.getenv(it).isNullOrBlank() }
      if (signingReady) signingConfig = signingConfigs.getByName("release")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation("com.google.firebase:firebase-auth:23.1.0")
  implementation("androidx.credentials:credentials:1.3.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
  implementation("org.tensorflow:tensorflow-lite:2.5.0")
  implementation("androidx.work:work-runtime-ktx:2.9.1")
  implementation("com.android.billingclient:billing-ktx:6.2.1")
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
