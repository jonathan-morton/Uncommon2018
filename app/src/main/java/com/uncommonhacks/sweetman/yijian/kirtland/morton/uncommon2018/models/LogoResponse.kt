package com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.models

import android.util.Log
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type
import javax.annotation.Generated

@Generated("com.robohorse.robopojogenerator")
data class LogoResponse(

        @field:SerializedName("responses")
        val responses: List<ResponsesItem?>? = null
)

@Generated("com.robohorse.robopojogenerator")
data class ResponsesItem(

        @field:SerializedName("logoAnnotations")
        val logoAnnotations: List<LogoAnnotationsItem?>? = null
)

@Generated("com.robohorse.robopojogenerator")
data class LogoAnnotationsItem(

        @field:SerializedName("score")
        val score: Double? = null,

        @field:SerializedName("mid")
        val mid: String? = null,

        @field:SerializedName("description")
        val description: String? = null,

        @field:SerializedName("boundingPoly")
        val boundingPoly: BoundingPoly? = null
)

@Generated("com.robohorse.robopojogenerator")
data class BoundingPoly(

        @field:SerializedName("vertices")
        val vertices: List<VerticesItem?>? = null
)

@Generated("com.robohorse.robopojogenerator")
data class VerticesItem(

        @field:SerializedName("x")
        val X: Int? = null,

        @field:SerializedName("y")
        val Y: Int? = null
)

class ResponseDeserializer : JsonDeserializer<LogoResponse> {
    private val TAG = ResponseDeserializer::class.java.simpleName

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LogoResponse {
        val list = mutableListOf<ResponsesItem?>()

        val jsonArray = json.asJsonArray
        val gson = Gson()

        for (i in 0 until jsonArray.size()) {
            // for some reason this is always one object even tho there are many potential results, so
            // just to future proof, gonna treat is as an array and hope for the best
            val responses = jsonArray.get(i)

            val logos = gson.fromJson(responses, ResponsesItem::class.java)
            if (logos != null) {
                Log.d(TAG, "add logos: " + logos)
                list.add(logos)
            }
        }

        return LogoResponse(list)
    }
}