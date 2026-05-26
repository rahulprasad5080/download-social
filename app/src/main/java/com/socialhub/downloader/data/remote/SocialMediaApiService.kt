package com.socialhub.downloader.data.remote

import com.socialhub.downloader.BuildConfig
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface SocialMediaApiService {
    @Headers("Content-Type: application/json")
    @POST(BuildConfig.SOCIAL_DOWNLOADER_ENDPOINT)
    suspend fun resolveMedia(@Body request: VideoRequest): Response<VideoResponse>
}
