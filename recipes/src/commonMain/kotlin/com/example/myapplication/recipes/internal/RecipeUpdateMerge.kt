package com.example.myapplication.recipes.internal

import com.example.myapplication.recipes.model.RecipeUpdate

/** Serializes an update to the request-body shape expected by the API. */
internal fun RecipeUpdate.toDto(): RecipeUpdateDto = RecipeUpdateDto.from(this)
