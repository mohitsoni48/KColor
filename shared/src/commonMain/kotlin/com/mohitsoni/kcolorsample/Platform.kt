package com.mohitsoni.kcolorsample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform