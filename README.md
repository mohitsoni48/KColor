# KColor

![Maven Central](https://img.shields.io/badge/Maven_Central-1.0.2-blue)

This is a Kotlin multiplatform library for sharing color between android and iOS.

## How to Install

In project level build.gradle

```
plugins {
    //other plugins
    id("io.github.mohitsoni48.KColor") version "<Version>" apply false
}
```

In shared module build.gradle

```
plugins {
    //other plugins
    id("io.github.mohitsoni48.KColor")
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

kColor {
    packageName = "com.mohitsoni.kcolorsample"
    sharedModule = "shared" //optional, default is shared
    iosAppName = "iosApp"   //optional, default is iosApp
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
Verify generated colors file at shared>build>generated>colors>KColor.kt
You can then simply use this color in your composable like this

```
@Composable
fun GreetingView(text: String) {
    Text(text = text, color = KColor.primaryThree)
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

If you want to set color from your ViewModels you can use generated KColorRes to set colors like this

In shared module build.gradle

```
plugins {
    //other plugins
    id("io.github.mohitsoni48.KColor")
}

kotlin {

    sourceSets.commonMain {
        kotlin.srcDir("build/generated/colors")
    }
    sourceSets.androidMain {
        kotlin.srcDir("build/generated/android/kcolors")
    }
    sourceSets.iosMain {
        kotlin.srcDir("build/generated/ios/kcolors")
    }

    sourceSets {
        commonMain.dependencies {
            //...
        }
    }
}

kColor {
    packageName = "com.mohitsoni.kcolorsample"
    sharedModule = "shared" //optional, default is shared
    iosAppName = "iosApp"   //optional, default is iosApp
}
```

In shared ViewModel

```
class ColorStateViewModel: ViewModel() {
    val colorState: MutableStateFlow<KColorRes> = MutableStateFlow(KColorRes.primary)
    //.....
    fun setColor() {
        colorState.value = KColorRes.primaryTwo
    }
}
```

and to use in Android and iOS you can use ```getColor()``` method which return ```Color``` from Jertpack compose in AndroidMain and ```UIColor``` in iOSMain

### In Android
```
Text(text = text, color = getColor(colorState.value))
```

### In iOS
Since in iOS kotlin has interop with Objective-C and not Swift, we get UIColor which you have to convert to ```Color``` in swift so you can create an extention func in swift

```
func getColor(kColorRes: KColorRes) -> Color {
    return Color(GetColorKt.getColor(kColorRes: kColorRes))
}
```

and use it like
```
Text(greet)
    .foregroundColor(getColor(kColorRes: colorState.value))
```

See full instruction with illustrations here: https://medium.com/@mohitsoni48/kcolor-a-library-to-share-color-between-android-and-ios-in-kmm-aca162411dc2


## To support this library

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/L3L612MBTA)
