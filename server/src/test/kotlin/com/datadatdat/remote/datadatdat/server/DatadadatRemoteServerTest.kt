/*
 * Copyright Datadatdat.
 */

package com.datadatdat.remote.datadatdat.server

import com.datadatdat.remote.RemoteOperation
import com.datadatdat.remote.RemoteOperationType
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File
import java.io.IOException

class DatadadatRemoteServerTest : StringSpec() {
    private val server = DatadadatRemoteServer()

    // Server with mocked HTTP client for testing HTTP-dependent methods
    private val mockClient = mockk<OkHttpClient>()
    private val mockedServer = DatadadatRemoteServer().also { s ->
        val field = DatadadatRemoteServer::class.java.getDeclaredField("httpClient")
        field.isAccessible = true
        field.set(s, mockClient)
    }

    private val baseRemote = mapOf(
        "api_base_url" to "http://localhost:8080",
        "org" to "myorg",
        "repo" to "myrepo",
    )

    private val baseParams = emptyMap<String, Any>()

    private fun makeOperation(
        remote: Map<String, Any> = baseRemote,
        parameters: Map<String, Any> = baseParams,
        commitId: String = "commit-123",
        type: RemoteOperationType = RemoteOperationType.PUSH,
    ): RemoteOperation = RemoteOperation(
        updateProgress = { _, _, _ -> },
        remote = remote,
        parameters = parameters,
        operationId = "op-1",
        commitId = commitId,
        commit = mapOf("key" to "value"),
        type = type,
    )

    /**
     * Build a mock OkHttp response with the given code and body.
     */
    private fun mockResponse(code: Int, body: String, request: Request? = null): Response {
        val req = request ?: Request.Builder().url("http://localhost").build()
        return Response.Builder()
            .request(req)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(body.toResponseBody(null))
            .build()
    }

    /**
     * Set up the mock client to return a given response for any call.
     */
    private fun setupMockCall(response: Response) {
        val mockCall = mockk<Call>()
        every { mockCall.execute() } returns response
        every { mockClient.newCall(any()) } returns mockCall
    }

    /**
     * Set up the mock client to throw an exception.
     */
    private fun setupMockCallException(exception: Exception) {
        val mockCall = mockk<Call>()
        every { mockCall.execute() } throws exception
        every { mockClient.newCall(any()) } returns mockCall
    }

    init {
        // =========================================
        // getProvider tests
        // =========================================

        "getProvider returns datadatdat" {
            server.getProvider() shouldBe "datadatdat"
        }

        // =========================================
        // validateRemote tests
        // =========================================

        "validateRemote succeeds with required fields" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                )
            val result = server.validateRemote(remote)
            result shouldNotBe null
            result["api_base_url"] shouldBe "http://localhost:8080"
            result["org"] shouldBe "myorg"
            result["repo"] shouldBe "myrepo"
        }

        "validateRemote succeeds with optional port" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to 8080,
                )
            val result = server.validateRemote(remote)
            result["port"] shouldBe 8080
        }

        "validateRemote succeeds with optional api_token" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "api_token" to "secret123",
                )
            val result = server.validateRemote(remote)
            result["api_token"] shouldBe "secret123"
        }

        "validateRemote fails with missing api_base_url" {
            val remote =
                mapOf(
                    "org" to "myorg",
                    "repo" to "myrepo",
                )
            shouldThrow<IllegalArgumentException> {
                server.validateRemote(remote)
            }
        }

        "validateRemote fails with missing org" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "repo" to "myrepo",
                )
            shouldThrow<IllegalArgumentException> {
                server.validateRemote(remote)
            }
        }

        "validateRemote fails with missing repo" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                )
            shouldThrow<IllegalArgumentException> {
                server.validateRemote(remote)
            }
        }

        "validateRemote fails with invalid port (negative)" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to -1,
                )
            shouldThrow<IllegalArgumentException> {
                server.validateRemote(remote)
            }
        }

        "validateRemote fails with invalid port (too large)" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to 70000,
                )
            shouldThrow<IllegalArgumentException> {
                server.validateRemote(remote)
            }
        }

        "validateRemote fails with unknown property" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "unknown" to "value",
                )
            shouldThrow<IllegalArgumentException> {
                server.validateRemote(remote)
            }
        }

        "validateRemote succeeds with port as Double (JSON deserialization)" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to 8080.0,
                )
            val result = server.validateRemote(remote)
            result shouldNotBe null
        }

        "validateRemote fails with port as Double out of range" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to 70000.0,
                )
            shouldThrow<IllegalArgumentException> {
                server.validateRemote(remote)
            }
        }

        "validateRemote fails with port as Double negative" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to -1.0,
                )
            shouldThrow<IllegalArgumentException> {
                server.validateRemote(remote)
            }
        }

        "validateRemote fails with port as non-number type" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to "not-a-number",
                )
            shouldThrow<IllegalArgumentException> {
                server.validateRemote(remote)
            }
        }

        "validateRemote succeeds with valid port boundary (1)" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to 1,
                )
            val result = server.validateRemote(remote)
            result["port"] shouldBe 1
        }

        "validateRemote succeeds with valid port boundary (65535)" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to 65535,
                )
            val result = server.validateRemote(remote)
            result["port"] shouldBe 65535
        }

        "validateRemote fails with port 0" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to 0,
                )
            shouldThrow<IllegalArgumentException> {
                server.validateRemote(remote)
            }
        }

        "validateRemote fails with port 65536" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to 65536,
                )
            shouldThrow<IllegalArgumentException> {
                server.validateRemote(remote)
            }
        }

        // =========================================
        // validateParameters tests
        // =========================================

        "validateParameters succeeds with empty map" {
            val params = emptyMap<String, Any>()
            val result = server.validateParameters(params)
            result shouldBe emptyMap()
        }

        "validateParameters succeeds with api_token" {
            val params = mapOf("api_token" to "secret123")
            val result = server.validateParameters(params)
            result["api_token"] shouldBe "secret123"
        }

        "validateParameters fails with unknown parameter" {
            val params = mapOf("unknown" to "value")
            shouldThrow<IllegalArgumentException> {
                server.validateParameters(params)
            }
        }

        "validateParameters with null input returns empty map" {
            val result = server.validateParameters(null)
            result shouldBe emptyMap()
        }

        "validateParameters succeeds with all optional fields" {
            val params = mapOf(
                "api_base_url" to "http://localhost:8080",
                "org" to "myorg",
                "repo" to "myrepo",
                "port" to 8080,
                "api_token" to "secret123",
            )
            val result = server.validateParameters(params)
            result["api_token"] shouldBe "secret123"
        }

        // =========================================
        // getCommit tests (exercises getApiToken, getBaseUrl, executeRequest)
        // =========================================

        "getCommit returns parsed commit on success" {
            val responseJson = """{"commitId":"commit-123","timestamp":"2024-01-01T00:00:00Z"}"""
            setupMockCall(mockResponse(200, responseJson))

            val result = mockedServer.getCommit(baseRemote, baseParams, "commit-123")
            result shouldNotBe null
            result!!["commitId"] shouldBe "commit-123"

            verify { mockClient.newCall(match { it.url.toString() == "http://localhost:8080/api/v1/repos/myorg/myrepo/commits/commit-123" }) }
        }

        "getCommit returns null on 404" {
            setupMockCallException(IOException("HTTP request failed: 404 Not Found"))

            val result = mockedServer.getCommit(baseRemote, baseParams, "nonexistent")
            result shouldBe null
        }

        "getCommit throws on server error (non-404)" {
            setupMockCallException(IOException("HTTP request failed: 500 Internal Server Error"))

            shouldThrow<IOException> {
                mockedServer.getCommit(baseRemote, baseParams, "commit-123")
            }
        }

        "getCommit includes auth header when token in parameters" {
            val responseJson = """{"commitId":"commit-123"}"""
            setupMockCall(mockResponse(200, responseJson))
            val params = mapOf<String, Any>("api_token" to "my-secret-token")

            mockedServer.getCommit(baseRemote, params, "commit-123")

            verify {
                mockClient.newCall(match { request ->
                    request.header("Authorization") == "Bearer my-secret-token"
                })
            }
        }

        "getCommit includes auth header when token in remote" {
            val responseJson = """{"commitId":"commit-123"}"""
            setupMockCall(mockResponse(200, responseJson))
            val remoteWithToken = baseRemote + ("api_token" to "remote-token")

            mockedServer.getCommit(remoteWithToken, baseParams, "commit-123")

            verify {
                mockClient.newCall(match { request ->
                    request.header("Authorization") == "Bearer remote-token"
                })
            }
        }

        "getCommit prefers token from parameters over remote" {
            val responseJson = """{"commitId":"commit-123"}"""
            setupMockCall(mockResponse(200, responseJson))
            val remoteWithToken = baseRemote + ("api_token" to "remote-token")
            val paramsWithToken = mapOf<String, Any>("api_token" to "param-token")

            mockedServer.getCommit(remoteWithToken, paramsWithToken, "commit-123")

            verify {
                mockClient.newCall(match { request ->
                    request.header("Authorization") == "Bearer param-token"
                })
            }
        }

        "getCommit sends no auth header when no token" {
            val responseJson = """{"commitId":"commit-123"}"""
            setupMockCall(mockResponse(200, responseJson))

            mockedServer.getCommit(baseRemote, baseParams, "commit-123")

            verify {
                mockClient.newCall(match { request ->
                    request.header("Authorization") == null
                })
            }
        }

        // =========================================
        // listCommits tests
        // =========================================

        "listCommits returns parsed commits" {
            val responseJson = """{
                "commits": [
                    {"commitId":"c1","metadata":{"timestamp":"2024-01-02T00:00:00Z","tags":{}}},
                    {"commitId":"c2","metadata":{"timestamp":"2024-01-01T00:00:00Z","tags":{}}}
                ]
            }"""
            setupMockCall(mockResponse(200, responseJson))

            val result = mockedServer.listCommits(baseRemote, baseParams, emptyList())
            result.size shouldBe 2
            result[0].first shouldBe "c1"
            result[1].first shouldBe "c2"
        }

        "listCommits returns empty list when no commits" {
            val responseJson = """{"commits":[]}"""
            setupMockCall(mockResponse(200, responseJson))

            val result = mockedServer.listCommits(baseRemote, baseParams, emptyList())
            result.size shouldBe 0
        }

        "listCommits returns empty list when commits key is null" {
            val responseJson = """{}"""
            setupMockCall(mockResponse(200, responseJson))

            val result = mockedServer.listCommits(baseRemote, baseParams, emptyList())
            result.size shouldBe 0
        }

        "listCommits filters by tags" {
            val responseJson = """{
                "commits": [
                    {"commitId":"c1","metadata":{"timestamp":"2024-01-02T00:00:00Z","tags":{"env":"prod"}}},
                    {"commitId":"c2","metadata":{"timestamp":"2024-01-01T00:00:00Z","tags":{"env":"dev"}}}
                ]
            }"""
            setupMockCall(mockResponse(200, responseJson))

            val result = mockedServer.listCommits(
                baseRemote,
                baseParams,
                listOf("env" to "prod"),
            )
            result.size shouldBe 1
            result[0].first shouldBe "c1"
        }

        "listCommits skips commits with null commitId" {
            val responseJson = """{
                "commits": [
                    {"metadata":{"timestamp":"2024-01-01T00:00:00Z","tags":{}}},
                    {"commitId":"c1","metadata":{"timestamp":"2024-01-01T00:00:00Z","tags":{}}}
                ]
            }"""
            setupMockCall(mockResponse(200, responseJson))

            val result = mockedServer.listCommits(baseRemote, baseParams, emptyList())
            result.size shouldBe 1
            result[0].first shouldBe "c1"
        }

        "listCommits skips commits with null metadata" {
            val responseJson = """{
                "commits": [
                    {"commitId":"c1"},
                    {"commitId":"c2","metadata":{"timestamp":"2024-01-01T00:00:00Z","tags":{}}}
                ]
            }"""
            setupMockCall(mockResponse(200, responseJson))

            val result = mockedServer.listCommits(baseRemote, baseParams, emptyList())
            result.size shouldBe 1
            result[0].first shouldBe "c2"
        }

        "listCommits throws on server error" {
            setupMockCall(mockResponse(500, "Internal Server Error"))

            shouldThrow<IOException> {
                mockedServer.listCommits(baseRemote, baseParams, emptyList())
            }
        }

        "listCommits includes auth header when token provided" {
            val responseJson = """{"commits":[]}"""
            setupMockCall(mockResponse(200, responseJson))
            val params = mapOf<String, Any>("api_token" to "my-token")

            mockedServer.listCommits(baseRemote, params, emptyList())

            verify {
                mockClient.newCall(match { request ->
                    request.header("Authorization") == "Bearer my-token" &&
                        request.url.toString() == "http://localhost:8080/api/v1/repos/myorg/myrepo/commits"
                })
            }
        }

        // =========================================
        // syncDataStart / syncDataEnd tests
        // =========================================

        "syncDataStart returns null" {
            val op = makeOperation()
            val result = server.syncDataStart(op)
            result shouldBe null
        }

        "syncDataEnd completes without error on success" {
            val op = makeOperation()
            server.syncDataEnd(op, null, true)
            // No exception thrown = success
        }

        "syncDataEnd completes without error on failure" {
            val op = makeOperation()
            server.syncDataEnd(op, null, false)
            // No exception thrown = success
        }

        // =========================================
        // pullArchive tests
        // =========================================

        "pullArchive downloads content to file" {
            val archiveContent = "fake-archive-content"
            val response = mockResponse(200, archiveContent)
            setupMockCall(response)

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                val op = makeOperation(commitId = "commit-456", type = RemoteOperationType.PULL)
                mockedServer.pullArchive(op, null, "data", tempFile)

                tempFile.readText() shouldBe archiveContent

                verify {
                    mockClient.newCall(match { request ->
                        request.url.toString() == "http://localhost:8080/api/v1/repos/myorg/myrepo/commits/commit-456/volumes/data" &&
                            request.method == "GET"
                    })
                }
            } finally {
                tempFile.delete()
            }
        }

        "pullArchive throws on server error" {
            setupMockCall(mockResponse(500, "Error"))

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                val op = makeOperation(type = RemoteOperationType.PULL)
                shouldThrow<IOException> {
                    mockedServer.pullArchive(op, null, "data", tempFile)
                }
            } finally {
                tempFile.delete()
            }
        }

        "pullArchive includes auth header when token provided" {
            val response = mockResponse(200, "data")
            setupMockCall(response)

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                val params = mapOf<String, Any>("api_token" to "pull-token")
                val op = makeOperation(parameters = params, type = RemoteOperationType.PULL)
                mockedServer.pullArchive(op, null, "vol1", tempFile)

                verify {
                    mockClient.newCall(match { request ->
                        request.header("Authorization") == "Bearer pull-token"
                    })
                }
            } finally {
                tempFile.delete()
            }
        }

        // =========================================
        // pushArchive tests
        // =========================================

        "pushArchive uploads file content" {
            setupMockCall(mockResponse(200, ""))

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                tempFile.writeText("archive-data")
                val op = makeOperation(commitId = "commit-789")
                mockedServer.pushArchive(op, null, "data", tempFile)

                verify {
                    mockClient.newCall(match { request ->
                        request.url.toString() == "http://localhost:8080/api/v1/repos/myorg/myrepo/commits/commit-789/volumes/data" &&
                            request.method == "PUT"
                    })
                }
            } finally {
                tempFile.delete()
            }
        }

        "pushArchive throws when file does not exist" {
            val nonExistentFile = File("/nonexistent/path/archive.tar.gz")
            val op = makeOperation()

            shouldThrow<IOException> {
                mockedServer.pushArchive(op, null, "data", nonExistentFile)
            }
        }

        "pushArchive throws on server error" {
            setupMockCall(mockResponse(500, "Error"))

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                tempFile.writeText("archive-data")
                val op = makeOperation()
                shouldThrow<IOException> {
                    mockedServer.pushArchive(op, null, "data", tempFile)
                }
            } finally {
                tempFile.delete()
            }
        }

        "pushArchive includes auth header when token provided" {
            setupMockCall(mockResponse(200, ""))

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                tempFile.writeText("archive-data")
                val params = mapOf<String, Any>("api_token" to "push-token")
                val op = makeOperation(parameters = params)
                mockedServer.pushArchive(op, null, "vol1", tempFile)

                verify {
                    mockClient.newCall(match { request ->
                        request.header("Authorization") == "Bearer push-token"
                    })
                }
            } finally {
                tempFile.delete()
            }
        }

        // =========================================
        // pushMetadata tests
        // =========================================

        "pushMetadata sends POST with commit data" {
            setupMockCall(mockResponse(200, ""))

            val op = makeOperation(commitId = "commit-abc")
            val commit = mapOf<String, Any>("timestamp" to "2024-01-01T00:00:00Z")
            mockedServer.pushMetadata(op, commit, false)

            verify {
                mockClient.newCall(match { request ->
                    request.url.toString() == "http://localhost:8080/api/v1/repos/myorg/myrepo/commits" &&
                        request.method == "POST"
                })
            }
        }

        "pushMetadata sends POST with isUpdate true" {
            setupMockCall(mockResponse(200, ""))

            val op = makeOperation(commitId = "commit-abc")
            val commit = mapOf<String, Any>("timestamp" to "2024-01-01T00:00:00Z")
            mockedServer.pushMetadata(op, commit, true)

            verify {
                mockClient.newCall(match { request ->
                    request.method == "POST"
                })
            }
        }

        "pushMetadata throws on server error" {
            setupMockCall(mockResponse(500, "Error"))

            val op = makeOperation()
            val commit = mapOf<String, Any>("timestamp" to "2024-01-01T00:00:00Z")
            shouldThrow<IOException> {
                mockedServer.pushMetadata(op, commit, false)
            }
        }

        "pushMetadata includes auth header when token provided" {
            setupMockCall(mockResponse(200, ""))

            val params = mapOf<String, Any>("api_token" to "meta-token")
            val op = makeOperation(parameters = params)
            val commit = mapOf<String, Any>("timestamp" to "2024-01-01T00:00:00Z")
            mockedServer.pushMetadata(op, commit, false)

            verify {
                mockClient.newCall(match { request ->
                    request.header("Authorization") == "Bearer meta-token"
                })
            }
        }

        "pushMetadata sends no auth header when no token" {
            setupMockCall(mockResponse(200, ""))

            val op = makeOperation()
            val commit = mapOf<String, Any>("timestamp" to "2024-01-01T00:00:00Z")
            mockedServer.pushMetadata(op, commit, false)

            verify {
                mockClient.newCall(match { request ->
                    request.header("Authorization") == null
                })
            }
        }

        // =========================================
        // executeRequest error path tests (via getCommit/listCommits)
        // =========================================

        "executeRequest handles empty response body" {
            setupMockCall(mockResponse(200, ""))

            // getCommit calls executeRequest internally; with empty body it returns ""
            // Gson.fromJson("", ...) returns null, so getCommit returns null
            val result = mockedServer.getCommit(baseRemote, baseParams, "commit-123")
            result shouldBe null
        }
    }
}
