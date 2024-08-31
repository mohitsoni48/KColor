package com.mohitsoni.kcolorsample

import kcolorsample.shared.generated.resources.Res
import kcolorsample.shared.generated.resources.title
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class Greeting {
    private val platform: Platform = getPlatform()

    fun greet(): String {
        val string = runBlocking {
            getString(Res.string.title)
        }
        return "Hellog, ${platform.name}!"
    }
}