/*
 * Copyright Datadatdat.
 */

package com.datadatdat.remote.datadatdat.server

import com.datadatdat.remote.RemoteOperation
import com.datadatdat.remote.RemoteOperationType
import com.datadatdat.remote.RemoteServerUtil
import com.datadatdat.remote.archive.ArchiveRemote
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The Datadatdat provider integrates with datadatdat-remote-server, a "GitHub for d3" service
 * that provides centralized storage and management of d3 commits via HTTP APIs.
 *
 * This provider handles client-side operations for pushing, pulling, cloning, and listing
 * commits from a datadatdat-remote-server instance.
 *
 * Expected remote properties:
 * - api_base_url (required): Base URL of the datadatdat-remote-server (e.g., "http://localhost:8080")
 * - org (required): Organization name in the remote server
 * - repo (required): Repository name in the remote server
 * - port (optional): Port number if not included in api_base_url
 * - api_token (optional): Authentication token for API access
 *
 * Example remote URL: http://localhost:8080/myorg/myrepo
 */
class DatadadatRemoteServer : ArchiveRemote() {
    private val gson = Gson()
    private val util = RemoteServerUtil()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val octetStreamMediaType = "application/octet-stream".toMediaType()

    // HTTP client with reasonable timeouts
    private val httpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .build()

    override fun getProvider(): String = "datadatdat"

    /**
     * Validate a datadatdat remote. Required fields are api_base_url, org, and repo.
     * Optional fields include port and api_token.
     */
    override fun validateRemote(remote: Map<String, Any>): Map<String, Any> {
        util.validateFields(
            remote,
            listOf("api_base_url", "org", "repo"),
            listOf("port", "api_token"),
        )

        // Validate port if present
        if (remote.containsKey("port")) {
            val port = remote["port"]
            when (port) {
                is Int -> {
                    if (port < 1 || port > 65535) {
                        throw IllegalArgumentException("port must be between 1 and 65535")
                    }
                }
                is Double -> {
                    val portInt = port.toInt()
                    if (portInt < 1 || portInt > 65535) {
                        throw IllegalArgumentException("port must be between 1 and 65535")
                    }
                }
                else -> throw IllegalArgumentException("port must be a number")
            }
        }

        return remote
    }

    /**
     * Validate datadatdat parameters. The only supported parameter is api_token.
     */
    override fun validateParameters(parameters: Map<String, Any>): Map<String, Any> {
        util.validateFields(parameters, emptyList(), listOf("api_token"))
        return parameters
    }

    /**
     * Get the API token from parameters or remote properties.
     */
    private fun getApiToken(
        remote: Map<String, Any>,
        parameters: Map<String, Any>,
    ): String? = (parameters["api_token"] ?: remote["api_token"]) as? String

    /**
     * Build the base API URL from remote properties.
     */
    private fun getBaseUrl(remote: Map<String, Any>): String {
        val baseUrl = remote["api_base_url"] as String
        val org = remote["org"] as String
        val repo = remote["repo"] as String
        return "$baseUrl/api/v1/repos/$org/$repo"
    }

    /**
     * Execute an HTTP request and return the response body as a string.
     */
    private fun executeRequest(request: Request): String {
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP request failed: ${response.code} ${response.message}")
            }
            return response.body?.string() ?: ""
        }
    }

    /**
     * Get metadata for a single commit from the remote server.
     */
    override fun getCommit(
        remote: Map<String, Any>,
        parameters: Map<String, Any>,
        commitId: String,
    ): Map<String, Any>? {
        val baseUrl = getBaseUrl(remote)
        val apiToken = getApiToken(remote, parameters)

        val requestBuilder =
            Request
                .Builder()
                .url("$baseUrl/commits/$commitId")
                .get()

        if (apiToken != null) {
            requestBuilder.addHeader("Authorization", "Bearer $apiToken")
        }

        return try {
            val responseBody = executeRequest(requestBuilder.build())
            val type = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson<Map<String, Any>>(responseBody, type)
        } catch (e: IOException) {
            if (e.message?.contains("404") == true) {
                null
            } else {
                throw e
            }
        }
    }

    /**
     * List all commits in a repository that match the given tags.
     */
    override fun listCommits(
        remote: Map<String, Any>,
        parameters: Map<String, Any>,
        tags: List<Pair<String, String?>>,
    ): List<Pair<String, Map<String, Any>>> {
        val baseUrl = getBaseUrl(remote)
        val apiToken = getApiToken(remote, parameters)

        val requestBuilder =
            Request
                .Builder()
                .url("$baseUrl/commits")
                .get()

        if (apiToken != null) {
            requestBuilder.addHeader("Authorization", "Bearer $apiToken")
        }

        val responseBody = executeRequest(requestBuilder.build())
        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
        val commits: List<Map<String, Any>> = gson.fromJson(responseBody, type)

        // Convert to list of pairs and filter by tags
        val result =
            commits.mapNotNull { commit ->
                val id = commit["id"]
                val properties = commit["properties"]
                if (id != null && properties != null) {
                    id as String
                    @Suppress("UNCHECKED_CAST")
                    properties as Map<String, Any>
                    if (util.matchTags(properties, tags)) {
                        id to properties
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

        return util.sortDescending(result)
    }

    /**
     * Start a sync operation. Returns operation data that will be passed to other sync methods.
     */
    override fun syncDataStart(operation: RemoteOperation): Any? {
        // No special setup needed for HTTP operations
        return null
    }

    /**
     * End a sync operation.
     */
    override fun syncDataEnd(
        operation: RemoteOperation,
        operationData: Any?,
        isSuccessful: Boolean,
    ) {
        // No cleanup needed
    }

    /**
     * Download an archive from the remote server.
     */
    override fun pullArchive(
        operation: RemoteOperation,
        operationData: Any?,
        volume: String,
        archive: File,
    ) {
        val baseUrl = getBaseUrl(operation.remote)
        val apiToken = getApiToken(operation.remote, operation.parameters)
        val commitId = operation.commitId

        val requestBuilder =
            Request
                .Builder()
                .url("$baseUrl/commits/$commitId/volumes/$volume")
                .get()

        if (apiToken != null) {
            requestBuilder.addHeader("Authorization", "Bearer $apiToken")
        }

        httpClient.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to download volume: ${response.code} ${response.message}")
            }

            // Write response body to archive file
            response.body?.byteStream()?.use { input ->
                archive.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    /**
     * Upload an archive to the remote server.
     */
    override fun pushArchive(
        operation: RemoteOperation,
        operationData: Any?,
        volume: String,
        archive: File,
    ) {
        val baseUrl = getBaseUrl(operation.remote)
        val apiToken = getApiToken(operation.remote, operation.parameters)
        val commitId = operation.commitId

        if (!archive.exists()) {
            throw IOException("Archive file not found: ${archive.path}")
        }

        val requestBody = archive.asRequestBody(octetStreamMediaType)
        val requestBuilder =
            Request
                .Builder()
                .url("$baseUrl/commits/$commitId/volumes/$volume")
                .put(requestBody)

        if (apiToken != null) {
            requestBuilder.addHeader("Authorization", "Bearer $apiToken")
        }

        executeRequest(requestBuilder.build())
    }

    /**
     * Push commit metadata to the remote server.
     */
    override fun pushMetadata(
        operation: RemoteOperation,
        commit: Map<String, Any>,
        isUpdate: Boolean,
    ) {
        val baseUrl = getBaseUrl(operation.remote)
        val apiToken = getApiToken(operation.remote, operation.parameters)
        val commitId = operation.commitId

        val requestBody =
            gson
                .toJson(
                    mapOf(
                        "id" to commitId,
                        "properties" to commit,
                    ),
                ).toRequestBody(jsonMediaType)

        val requestBuilder =
            Request
                .Builder()
                .url("$baseUrl/commits")
                .post(requestBody)

        if (apiToken != null) {
            requestBuilder.addHeader("Authorization", "Bearer $apiToken")
        }

        executeRequest(requestBuilder.build())
    }
}
