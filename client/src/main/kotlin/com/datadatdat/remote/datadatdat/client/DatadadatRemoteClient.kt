/*
 * Copyright Datadatdat.
 */

package com.datadatdat.remote.datadatdat.client

import com.datadatdat.remote.RemoteClient
import com.datadatdat.remote.RemoteClientUtil
import java.net.URI

/**
 * The URI syntax for Datadatdat HTTP remotes is:
 *
 *      http://host[:port]/org/repo
 *      https://host[:port]/org/repo
 *
 * The client will extract the API base URL, organization, and repository name from the URI.
 * The following properties are supported:
 *
 *      api_token   Authentication token for the Datadatdat API. If not specified in the
 *                  remote configuration, the user will be prompted for it when operations
 *                  are performed.
 *
 * Example URIs:
 *      http://localhost:8080/myorg/myrepo
 *      https://data.datadatdat.io/myorg/myrepo
 */
class DatadadatRemoteClient : RemoteClient {
    private val console = System.console()
    private val util = RemoteClientUtil()

    override fun getProvider(): String = "datadatdat"

    override fun parseUri(
        uri: URI,
        additionalProperties: Map<String, String>,
    ): Map<String, Any> {
        // Validate scheme
        if (uri.scheme != "http" && uri.scheme != "https") {
            throw IllegalArgumentException("Datadatdat remote URI must use http or https scheme")
        }

        // Validate host
        val host = uri.host ?: throw IllegalArgumentException("Missing host in Datadatdat remote URI")

        // Build API base URL
        val port = if (uri.port != -1) ":${uri.port}" else ""
        val apiBaseUrl = "${uri.scheme}://${host}$port"

        // Parse path to extract org and repo
        val path = uri.path ?: throw IllegalArgumentException("Missing path in Datadatdat remote URI")
        val pathParts = path.split("/").filter { it.isNotEmpty() }

        if (pathParts.size != 2) {
            throw IllegalArgumentException("Datadatdat remote URI path must be in format '/org/repo', got: ${uri.path}")
        }

        val org = pathParts[0]
        val repo = pathParts[1]

        // Validate additional properties
        var apiToken: String? = null
        for (p in additionalProperties.keys) {
            when (p) {
                "api_token" -> apiToken = additionalProperties[p]
                else -> throw IllegalArgumentException("Invalid Datadatdat remote property '$p'")
            }
        }

        // Build result
        val result =
            mutableMapOf<String, Any>(
                "api_base_url" to apiBaseUrl,
                "org" to org,
                "repo" to repo,
            )

        if (apiToken != null) {
            result["api_token"] = apiToken
        }

        return result
    }

    override fun toUri(properties: Map<String, Any>): Pair<String, Map<String, String>> {
        val apiBaseUrl =
            properties["api_base_url"] as? String
                ?: throw IllegalArgumentException("Missing api_base_url in remote properties")
        val org =
            properties["org"] as? String
                ?: throw IllegalArgumentException("Missing org in remote properties")
        val repo =
            properties["repo"] as? String
                ?: throw IllegalArgumentException("Missing repo in remote properties")

        // Construct URI: http://host:port/org/repo
        val uri = "$apiBaseUrl/$org/$repo"

        // Additional properties (don't include api_token in URI display)
        val props = mutableMapOf<String, String>()
        if (properties["api_token"] != null) {
            props["api_token"] = "*****"
        }

        return Pair(uri, props)
    }

    override fun getParameters(remoteProperties: Map<String, Any>): Map<String, Any> {
        // If api_token is already in remote properties, use it
        var apiToken = remoteProperties["api_token"] as? String

        // If not provided, prompt the user
        if (apiToken == null) {
            val input =
                console?.readPassword("API token: ")
                    ?: throw IllegalArgumentException("API token required but no console available")
            apiToken = String(input)
        }

        return mapOf("api_token" to apiToken)
    }
}
