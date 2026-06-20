package ru.levar.api.dto

import com.google.gson.annotations.SerializedName
import ru.levar.domain.Category
import ru.levar.domain.ErrorType

internal data class CategoryListResponse(
    @SerializedName("ListCategory") val listCategory: List<Category>,
    @SerializedName("Error") override val error: ErrorType,
) : ApiEnvelope
