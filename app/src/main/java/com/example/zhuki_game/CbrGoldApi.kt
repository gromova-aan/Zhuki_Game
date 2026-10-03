package com.example.zhuki_game

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CbrGoldApi {

    @GET("scripts/xml_metall.asp")
    suspend fun getMetals(
        @Query("date_req1") from: String,
        @Query("date_req2") to: String
    ): Response<String>
}
