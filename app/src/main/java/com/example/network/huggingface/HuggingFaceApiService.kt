package com.example.network.huggingface

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Retrofit REST API interface for Hugging Face Hub.
 * Docs: https://huggingface.co/docs/hub/api
 */
interface HuggingFaceApiService {

    /**
     * Search models on Hugging Face Hub.
     */
    @GET("api/models")
    suspend fun searchModels(
        @Query("search") search: String? = null,
        @Query("author") author: String? = null,
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = "downloads",
        @Query("direction") direction: String? = "-1",
        @Query("limit") limit: Int = 20,
        @Query("full") full: Boolean = true
    ): List<HfModelItem>

    /**
     * Fetch comprehensive metadata for a specific model repository.
     */
    @GET("api/models/{repoId}")
    suspend fun getModelDetails(
        @Path(value = "repoId", encoded = true) repoId: String
    ): HfModelDetail

    /**
     * Fetch tree of files inside a model repository.
     */
    @GET("api/models/{repoId}/tree/{revision}")
    suspend fun listRepoFiles(
        @Path(value = "repoId", encoded = true) repoId: String,
        @Path("revision") revision: String = "main"
    ): List<HfSibling>

    /**
     * Stream binary download of any file in repository (e.g. pytorch_model.bin, model.safetensors, model.gguf, config.json).
     * Uses @Streaming to stream directly to disk without loading entire payload into heap RAM.
     */
    @Streaming
    @GET("{repoId}/resolve/{revision}/{filename}")
    suspend fun downloadModelFile(
        @Path(value = "repoId", encoded = true) repoId: String,
        @Path("revision") revision: String = "main",
        @Path(value = "filename", encoded = true) filename: String,
        @Header("Range") rangeHeader: String? = null,
        @Header("Authorization") authHeader: String? = null
    ): Response<ResponseBody>

    /**
     * Download binary directly from an absolute URL (e.g. Hugging Face CDN / LFS redirect URL).
     */
    @Streaming
    @GET
    suspend fun downloadFromUrl(
        @Url url: String,
        @Header("Range") rangeHeader: String? = null,
        @Header("Authorization") authHeader: String? = null
    ): Response<ResponseBody>
}
