package com.example.network.huggingface

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data models for the Hugging Face Hub REST API.
 */
@JsonClass(generateAdapter = true)
data class HfModelItem(
    @Json(name = "_id") val internalId: String? = null,
    @Json(name = "id") val id: String,
    @Json(name = "author") val author: String? = null,
    @Json(name = "sha") val sha: String? = null,
    @Json(name = "downloads") val downloads: Int = 0,
    @Json(name = "likes") val likes: Int = 0,
    @Json(name = "private") val isPrivate: Boolean = false,
    @Json(name = "pipeline_tag") val pipelineTag: String? = null,
    @Json(name = "library_name") val libraryName: String? = null,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "lastModified") val lastModified: String? = null,
    @Json(name = "siblings") val siblings: List<HfSibling>? = null
)

@JsonClass(generateAdapter = true)
data class HfModelDetail(
    @Json(name = "_id") val internalId: String? = null,
    @Json(name = "id") val id: String,
    @Json(name = "author") val author: String? = null,
    @Json(name = "sha") val sha: String? = null,
    @Json(name = "downloads") val downloads: Int = 0,
    @Json(name = "likes") val likes: Int = 0,
    @Json(name = "private") val isPrivate: Boolean = false,
    @Json(name = "pipeline_tag") val pipelineTag: String? = null,
    @Json(name = "library_name") val libraryName: String? = null,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "siblings") val siblings: List<HfSibling> = emptyList(),
    @Json(name = "cardData") val cardData: HfCardData? = null,
    @Json(name = "config") val config: Map<String, Any?>? = null,
    @Json(name = "lastModified") val lastModified: String? = null
)

@JsonClass(generateAdapter = true)
data class HfSibling(
    @Json(name = "rfilename") val rfilename: String,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "blobId") val blobId: String? = null,
    @Json(name = "lfs") val lfs: HfLfsInfo? = null
)

@JsonClass(generateAdapter = true)
data class HfLfsInfo(
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "sha256") val sha256: String? = null,
    @Json(name = "pointerSize") val pointerSize: Long? = null
)

@JsonClass(generateAdapter = true)
data class HfCardData(
    @Json(name = "language") val language: Any? = null,
    @Json(name = "license") val license: String? = null,
    @Json(name = "tags") val tags: List<String>? = null
)

/**
 * Real-time progress update for binary/model downloads.
 */
data class HfDownloadProgress(
    val modelId: String,
    val fileName: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progressPercent: Int,
    val speedBytesPerSec: Long,
    val etaSeconds: Int,
    val isFinished: Boolean = false,
    val isResumed: Boolean = false,
    val destinationPath: String? = null,
    val error: String? = null,
    val sha256Checksum: String? = null
)
