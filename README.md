# KColor

![Maven Central](https://img.shields.io/badge/Maven_Central-1.0.0.alpha2-blue)

This is a Kotlin multiplatform library for sharing color between android and iOS.

## How to Install

To add KSP (Kotlin Symbol Processing) to your project, include the following in your dependencies:

```
google-devtools-ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }

```
In project level build.gradle

```
plugins {
    //other plugins
    alias(libs.plugins.google.devtools.ksp).apply(false)
}
```

In shared module build.gradle

```
plugins {
    //other plugins
    alias(libs.plugins.google.devtools.ksp)
}

kotlin {

    sourceSets.commonMain {
        kotlin.srcDir("build/generated/colors")
    }

    sourceSets {
        commonMain.dependencies {
            //...
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata" ) {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

ksp {
    arg("packageName", "<YOUR_PACKAGE_NAME>")
    arg("sharedModuleName", "<YOUR_SHARED_MODULE NAME>") //optional, default is shared
    arg("iosAppName", "<YOUR IOS APP NAME>") //optional, default is iosApp
}

dependencies {
    add("kspCommonMainMetadata", "io.github.mohitsoni48:KColor:<Version>")
}
```

## Add Colors
In your shared module: shared>src>commonMain>resources>colors add “colors.xml”
```
<resources>
    <color name="primary">#FF9900</color>
    <color name="primary_two">#AAFF9900</color>
    <color name="primary_three">#FFF00000</color>
</resources>
```

## How to use in Android
After adding color you can run your android app or run
```./gradlew build```
Verify generated colors file at shared>build>generated>colors>generatedcolors.kt
You can then simply use this color in your composable like this

```
@Composable
fun GreetingView(text: String) {
    Text(text = text, color = primaryThree)
}
```

## How to use in iOS

Once you have already run your Android app, open iosApp.xcodeproj. On your “iosApp”, right click and click on “Add Files to “iosApp” and add “Colors.xcassets”. You should already start seeing your colors in XCode.

## Enable Asset Symbol Extensions

select iosApp Target> Build Setting, Change the value of “Generate Swift Asset Symbol Extensions” to “yes”
And use it in your swift

```
struct ContentView: View {
 let greet = Greeting().greet()

 var body: some View {
  Text(greet)
    .foregroundColor(.primaryThree)
 }
}
```

All set

See full instruction with illustrations here: https://medium.com/@mohitsoni48/kcolor-a-library-to-share-color-between-android-and-ios-in-kmm-aca162411dc2


## Liked my work?

Did this library make your work easy? Tipe me here

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/L3L612MBTA)
