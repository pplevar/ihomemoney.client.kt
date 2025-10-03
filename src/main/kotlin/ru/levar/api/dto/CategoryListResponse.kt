package ru.levar.api.dto

import com.google.gson.annotations.SerializedName
import ru.levar.domain.Category
import ru.levar.domain.ErrorType

data class CategoryListResponse(
    @SerializedName("ListCategory") val listCategory: List<Category>,
    @SerializedName("Error") val error: ErrorType,
)
