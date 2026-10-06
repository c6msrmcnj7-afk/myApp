package com.example.myapplication.recipes

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js

internal actual fun createPlatformHttpClientEngine(): HttpClientEngine = Js.create()
