package com.example.myapplication.recipes.internal

import com.example.myapplication.recipes.model.DeletedRecipe
import com.example.myapplication.recipes.model.Page
import com.example.myapplication.recipes.model.Recipe

/** Converts wire objects into the public model types. */
internal object RecipesMapper {

    fun toDomain(dto: RecipeDto): Recipe = dto.toDomain()

    fun toPage(dto: RecipesPageDto): Page<Recipe> {
        val items = dto.recipes.map { it.toDomain() }
        return Page(
            items = items,
            total = if (dto.total > 0) dto.total else items.size,
            skip = dto.skip,
            limit = if (dto.limit > 0) dto.limit else dto.recipes.size,
        )
    }

    fun toDeletedRecipe(dto: RecipeDto): DeletedRecipe = dto.toDeletedRecipe()
}
