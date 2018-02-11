package com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.models

data class RequestLogos(
        val requests: List<RequestsItem?>? = null
)

data class RequestsItem(
        val image: Image? = null,
        val features: List<FeaturesItem?>? = null
)

data class Image(
        val content: String? = null
)

data class FeaturesItem(
        val type: String = "LOGO_DETECTION",
        val maxResults: Int = 10
)

