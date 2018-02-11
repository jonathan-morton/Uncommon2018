package com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.networking

import android.content.Context
import com.google.gson.GsonBuilder
import com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.BuildConfig
import com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.R
import com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.models.LogoResponse
import com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.models.RequestLogos
import com.uncommonhacks.sweetman.yijian.kirtland.morton.uncommon2018.models.ResponseDeserializer
import io.reactivex.Observable
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Created by Jonathan Morton on 2/10/18.
 */
interface GoogleVisionApi {

    companion object RestClient {
        fun create(context: Context): GoogleVisionApi {
            val httpClient = OkHttpClient.Builder()
                    .addInterceptor(LoggingInterceptor())
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

            val gson = GsonBuilder()
                    .registerTypeAdapter(LogoResponse::class.java, ResponseDeserializer())
                    .create()

            val ApiKey = BuildConfig.VISION_API_KEY
            val baseUrl = context.resources.getString(R.string.api_base_url)
            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .baseUrl(baseUrl)
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
            return retrofit.create(GoogleVisionApi::class.java)
        }

        private class LoggingInterceptor : Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request()

                val t1 = System.nanoTime()
                Timber.d(String.format("Sending request \n%s on %s%n%s",
                        request.url(), chain.connection(), request.headers()))

                val response = chain.proceed(request)

                val t2 = System.nanoTime()
                Timber.d(String.format("Received response for \n%s in %.1fms%n%s",
                        response.request().url(), (t2 - t1) / 1e6, response.headers()))


                val responseString = String(response.body()!!.bytes())

                Timber.d("Response:\n" + responseString)

                return response.newBuilder()
                        .body(ResponseBody.create(response.body()!!.contentType(), responseString))
                        .build()
            }
        }
    }

    /*@Headers("Content-type: application/json")
    @POST("v1/images:annotate")
    fun locateLogos(@Query("key") apiKey: String, @Body body: RequestLogos): Observable<LogoResponse>*/
    @Headers("Content-type: application/json")
    @POST("v1/images:annotate")
    fun locateLogos(@Query("key") apiKey: String, @Body body: RequestLogos): Observable<ResponseBody>
}