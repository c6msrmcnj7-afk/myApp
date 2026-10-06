package com.example.myapplication.recipes

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createPlatformHttpClientEngine(): HttpClientEngine = OkHttp.create()
