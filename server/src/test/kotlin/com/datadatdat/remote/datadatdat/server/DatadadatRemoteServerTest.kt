/*
 * Copyright Datadatdat.
 */

package com.datadatdat.remote.datadatdat.server

import com.datadatdat.remote.RemoteOperation
import com.datadatdat.remote.RemoteOperationType
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class DatadadatRemoteServerTest : StringSpec() {
    private val server = DatadadatRemoteServer()

    // Real OkHttp client + real local HTTP server. No reflection, no mock framework
    // for HTTP. MockWebServer responds with whatever we enqueue and records every
    // request so we can assert on headers, paths, and bodies. A fresh instance per
    // test guarantees isolation - MockWebServer is not restartable once closed.
    private lateinit var mockWebServer: MockWebServer

    override fun beforeTest(testCase: TestCase) {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    override fun afterTest(
        testCase: TestCase,
        result: TestResult,
    ) {
        mockWebServer.close()
    }

    private fun baseRemote(): Map<String, Any> =
        mapOf(
            "api_base_url" to mockWebServer.url("").toString().trimEnd('/'),
            "org" to "myorg",
            "repo" to "myrepo",
        )

    private val baseParams = emptyMap<String, Any>()

    private fun makeOperation(
        remote: Map<String, Any> = baseRemote(),
        parameters: Map<String, Any> = baseParams,
        commitId: String = "commit-123",
        type: RemoteOperationType = RemoteOperationType.PUSH,
    ): RemoteOperation =
        RemoteOperation(
            updateProgress = { _, _, _ -> },
            remote = remote,
            parameters = parameters,
            operationId = "op-1",
            commitId = commitId,
            commit = mapOf("key" to "value"),
            type = type,
        )

    private fun enqueue(
        code: Int,
        body: String = "",
    ) {
        mockWebServer.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .body(body)
                .build(),
        )
    }

    /**
     * Take the next recorded request, with a short timeout so a missing request
     * fails fast rather than hanging the test.
     */
    private fun nextRequest(): RecordedRequest =
        mockWebServer.takeRequest(5L, TimeUnit.SECONDS)
            ?: throw AssertionError("Expected an HTTP request but none was received")

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

        "validateRemote succeeds with port as Double boundary (1.0)" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to 1.0,
                )
            val result = server.validateRemote(remote)
            result shouldNotBe null
        }

        "validateRemote fails with port as Double 0.0" {
            val remote =
                mapOf(
                    "api_base_url" to "http://localhost:8080",
                    "org" to "myorg",
                    "repo" to "myrepo",
                    "port" to 0.0,
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
            val params =
                mapOf(
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
        // User-Agent interceptor (#1 in the architecture review)
        // =========================================

        "all outgoing requests carry the datadatdat User-Agent header" {
            // Enqueue a benign 200 so getCommit returns; we only care that the
            // recorded request shows our User-Agent.
            enqueue(200, """{"commitId":"c1"}""")

            server.getCommit(baseRemote(), baseParams, "c1")

            val recorded = nextRequest()
            recorded.headers["User-Agent"] shouldBe DatadadatRemoteServer.USER_AGENT
            // Match the documented pattern, not just our own constant value.
            (recorded.headers["User-Agent"] ?: "") shouldNotBe ""
            Regex("^datadatdat-remote-kotlin/\\S+$")
                .matches(recorded.headers["User-Agent"] ?: "") shouldBe true
        }

        // =========================================
        // getCommit tests
        // =========================================

        "getCommit returns parsed commit on success with properties format" {
            val responseJson =
                """{"id":"commit-123","properties":{"timestamp":"2024-01-01T00:00:00Z","v2":{"image":{"image":"postgres","tag":"latest"}}}}"""
            enqueue(200, responseJson)

            val result = server.getCommit(baseRemote(), baseParams, "commit-123")
            result.shouldNotBeNullAndAssert { commit ->
                commit["timestamp"] shouldBe "2024-01-01T00:00:00Z"
                commit.containsKey("id") shouldBe false
                commit.containsKey("commitId") shouldBe false
                commit.containsKey("v2") shouldBe true
            }

            val recorded = nextRequest()
            recorded.url.encodedPath shouldBe "/api/v1/repos/myorg/myrepo/commits/commit-123"
        }

        "getCommit handles flat commit format (fallback)" {
            val responseJson =
                """{"commitId":"commit-123","timestamp":"2024-01-01T00:00:00Z","metadata":{"v2":{"image":{"image":"postgres","tag":"latest"}}}}"""
            enqueue(200, responseJson)

            val result = server.getCommit(baseRemote(), baseParams, "commit-123")
            result.shouldNotBeNullAndAssert { commit ->
                commit["timestamp"] shouldBe "2024-01-01T00:00:00Z"
                commit.containsKey("commitId") shouldBe false
                commit.containsKey("v2") shouldBe true
            }
        }

        // Covers the inner-metadata merge branch inside getCommit's properties path
        // (the `metadata?.forEach` loop at lines 211-215).
        "getCommit merges nested metadata inside properties format" {
            val responseJson =
                """{"id":"commit-123","properties":{"timestamp":"2024-01-01T00:00:00Z","metadata":{"extra":"value","timestamp":"OVERRIDDEN"}}}"""
            enqueue(200, responseJson)

            val result = server.getCommit(baseRemote(), baseParams, "commit-123")
            result.shouldNotBeNullAndAssert { commit ->
                commit["extra"] shouldBe "value"
                // Top-level timestamp wins over the same key inside metadata.
                commit["timestamp"] shouldBe "2024-01-01T00:00:00Z"
                commit.containsKey("metadata") shouldBe false
            }
        }

        // Covers the fallback branch in getCommit where a top-level key collides
        // with one inside `metadata` (the `if (!properties.containsKey(k))` false branch).
        "getCommit fallback keeps top-level fields over metadata duplicates" {
            val responseJson =
                """{"commitId":"commit-123","author":"top-level","metadata":{"author":"nested","extra":"only-in-nested"}}"""
            enqueue(200, responseJson)

            val result = server.getCommit(baseRemote(), baseParams, "commit-123")
            result.shouldNotBeNullAndAssert { commit ->
                commit["author"] shouldBe "top-level"
                commit["extra"] shouldBe "only-in-nested"
            }
        }

        // Covers extractMap's else-branch: when the key exists but is not a Map,
        // the helper must return null (line 151). We exercise this via getCommit
        // by sending a response whose "properties" field is a string, not an object.
        "getCommit handles response whose properties field is not a map" {
            val responseJson = """{"commitId":"commit-123","properties":"not-a-map","timestamp":"2024-01-01T00:00:00Z"}"""
            enqueue(200, responseJson)

            // properties is a string, so extractMap returns null and we fall through
            // to the flat-format path. The result should include the timestamp.
            val result = server.getCommit(baseRemote(), baseParams, "commit-123")
            result.shouldNotBeNullAndAssert { commit ->
                commit["timestamp"] shouldBe "2024-01-01T00:00:00Z"
                commit.containsKey("commitId") shouldBe false
            }
        }

        // #2 in the architecture review: 404 must be detected via response.code,
        // not by string-matching the exception message.
        "getCommit returns null on real 404 response" {
            enqueue(404, "Not Found")

            val result = server.getCommit(baseRemote(), baseParams, "nonexistent")
            result shouldBe null
        }

        "getCommit returns null on 404 even when body looks like an error envelope" {
            // Realistic API response - we should not depend on the body shape at all.
            enqueue(404, """{"error":"commit not found"}""")

            val result = server.getCommit(baseRemote(), baseParams, "nonexistent")
            result shouldBe null
        }

        "getCommit throws on 500 response" {
            enqueue(500, "Internal Server Error")

            shouldThrow<IOException> {
                server.getCommit(baseRemote(), baseParams, "commit-123")
            }
        }

        "getCommit throws on other 4xx (not 404) response" {
            enqueue(403, "Forbidden")

            shouldThrow<IOException> {
                server.getCommit(baseRemote(), baseParams, "commit-123")
            }
        }

        "getCommit includes auth header when token in parameters" {
            enqueue(200, """{"commitId":"commit-123"}""")
            val params = mapOf<String, Any>("api_token" to "my-secret-token")

            server.getCommit(baseRemote(), params, "commit-123")

            nextRequest().headers["Authorization"] shouldBe "Bearer my-secret-token"
        }

        "getCommit includes auth header when token in remote" {
            enqueue(200, """{"commitId":"commit-123"}""")
            val remoteWithToken = baseRemote() + ("api_token" to "remote-token")

            server.getCommit(remoteWithToken, baseParams, "commit-123")

            nextRequest().headers["Authorization"] shouldBe "Bearer remote-token"
        }

        "getCommit prefers token from parameters over remote" {
            enqueue(200, """{"commitId":"commit-123"}""")
            val remoteWithToken = baseRemote() + ("api_token" to "remote-token")
            val paramsWithToken = mapOf<String, Any>("api_token" to "param-token")

            server.getCommit(remoteWithToken, paramsWithToken, "commit-123")

            nextRequest().headers["Authorization"] shouldBe "Bearer param-token"
        }

        "getCommit sends no auth header when no token" {
            enqueue(200, """{"commitId":"commit-123"}""")

            server.getCommit(baseRemote(), baseParams, "commit-123")

            nextRequest().headers["Authorization"] shouldBe null
        }

        // =========================================
        // listCommits tests
        // =========================================

        "listCommits returns parsed commits" {
            val responseJson =
                """{
                "commits": [
                    {"commitId":"c1","metadata":{"timestamp":"2024-01-02T00:00:00Z","tags":{}}},
                    {"commitId":"c2","metadata":{"timestamp":"2024-01-01T00:00:00Z","tags":{}}}
                ]
            }"""
            enqueue(200, responseJson)

            val result = server.listCommits(baseRemote(), baseParams, emptyList())
            result.size shouldBe 2
            result[0].first shouldBe "c1"
            result[1].first shouldBe "c2"
        }

        "listCommits returns empty list when no commits" {
            enqueue(200, """{"commits":[]}""")

            val result = server.listCommits(baseRemote(), baseParams, emptyList())
            result.size shouldBe 0
        }

        "listCommits returns empty list when commits key is null" {
            enqueue(200, """{}""")

            val result = server.listCommits(baseRemote(), baseParams, emptyList())
            result.size shouldBe 0
        }

        "listCommits filters by tags" {
            val responseJson =
                """{
                "commits": [
                    {"commitId":"c1","metadata":{"timestamp":"2024-01-02T00:00:00Z","tags":{"env":"prod"}}},
                    {"commitId":"c2","metadata":{"timestamp":"2024-01-01T00:00:00Z","tags":{"env":"dev"}}}
                ]
            }"""
            enqueue(200, responseJson)

            val result =
                server.listCommits(
                    baseRemote(),
                    baseParams,
                    listOf("env" to "prod"),
                )
            result.size shouldBe 1
            result[0].first shouldBe "c1"
        }

        "listCommits skips commits with null commitId" {
            val responseJson =
                """{
                "commits": [
                    {"metadata":{"timestamp":"2024-01-01T00:00:00Z","tags":{}}},
                    {"commitId":"c1","metadata":{"timestamp":"2024-01-01T00:00:00Z","tags":{}}}
                ]
            }"""
            enqueue(200, responseJson)

            val result = server.listCommits(baseRemote(), baseParams, emptyList())
            result.size shouldBe 1
            result[0].first shouldBe "c1"
        }

        "listCommits includes commits without metadata field" {
            val responseJson =
                """{
                "commits": [
                    {"commitId":"c1"},
                    {"commitId":"c2","metadata":{"timestamp":"2024-01-01T00:00:00Z","tags":{}}}
                ]
            }"""
            enqueue(200, responseJson)

            val result = server.listCommits(baseRemote(), baseParams, emptyList())
            result.size shouldBe 2
            result.map { it.first }.toSet() shouldBe setOf("c1", "c2")
        }

        "listCommits handles production API format with top-level fields" {
            val responseJson =
                """{
                "commits": [
                    {"commitId":"c1","repo":"myorg/myrepo","timestamp":"2024-01-02T00:00:00Z","size":1024,"author":"user@test.com","message":"test commit","tags":{"env":"prod"}},
                    {"commitId":"c2","repo":"myorg/myrepo","timestamp":"2024-01-01T00:00:00Z","size":512}
                ]
            }"""
            enqueue(200, responseJson)

            val result = server.listCommits(baseRemote(), baseParams, emptyList())
            result.size shouldBe 2
            result[0].first shouldBe "c1"
            result[0].second["timestamp"] shouldBe "2024-01-02T00:00:00Z"
            result[0].second["author"] shouldBe "user@test.com"
            result[0].second.containsKey("commitId") shouldBe false
            result[1].first shouldBe "c2"
        }

        // Covers the false branch of `if (!properties.containsKey(k))` inside
        // listCommits's nested-merge loop (line ~286): when a metadata key collides
        // with an existing top-level key, the top-level value must win.
        "listCommits keeps top-level fields when metadata has the same key" {
            val responseJson =
                """{
                "commits": [
                    {"commitId":"c1","author":"top-level","metadata":{"author":"nested","extra":"only-in-nested"}}
                ]
            }"""
            enqueue(200, responseJson)

            val result = server.listCommits(baseRemote(), baseParams, emptyList())
            result.size shouldBe 1
            result[0].second["author"] shouldBe "top-level"
            result[0].second["extra"] shouldBe "only-in-nested"
        }

        "listCommits merges properties sub-map (PROD format) into top level" {
            val responseJson =
                """{
                "commits": [
                    {"commitId":"c1","repo":"myorg/myrepo","timestamp":"2024-01-02T00:00:00Z","author":"user","properties":{"v2":{"image":{"image":"postgres","tag":"latest"}},"user":"Test User"}}
                ]
            }"""
            enqueue(200, responseJson)

            val result = server.listCommits(baseRemote(), baseParams, emptyList())
            result.size shouldBe 1
            result[0].first shouldBe "c1"
            result[0].second.containsKey("v2") shouldBe true
            result[0].second["author"] shouldBe "user"
            result[0].second.containsKey("properties") shouldBe false
        }

        "listCommits throws on server error" {
            enqueue(500, "Internal Server Error")

            shouldThrow<IOException> {
                server.listCommits(baseRemote(), baseParams, emptyList())
            }
        }

        "listCommits includes auth header when token provided" {
            enqueue(200, """{"commits":[]}""")
            val params = mapOf<String, Any>("api_token" to "my-token")

            server.listCommits(baseRemote(), params, emptyList())

            val recorded = nextRequest()
            recorded.headers["Authorization"] shouldBe "Bearer my-token"
            recorded.url.encodedPath shouldBe "/api/v1/repos/myorg/myrepo/commits"
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
        }

        "syncDataEnd completes without error on failure" {
            val op = makeOperation()
            server.syncDataEnd(op, null, false)
        }

        // =========================================
        // pullArchive tests
        // =========================================

        "pullArchive downloads content to file" {
            val archiveContent = "fake-archive-content"
            enqueue(200, archiveContent)

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                val op = makeOperation(commitId = "commit-456", type = RemoteOperationType.PULL)
                server.pullArchive(op, null, "data", tempFile)

                tempFile.readText() shouldBe archiveContent

                val recorded = nextRequest()
                recorded.url.encodedPath shouldBe "/api/v1/repos/myorg/myrepo/commits/commit-456/volumes/data"
                recorded.method shouldBe "GET"
            } finally {
                tempFile.delete()
            }
        }

        "pullArchive throws on server error" {
            enqueue(500, "Error")

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                val op = makeOperation(type = RemoteOperationType.PULL)
                shouldThrow<IOException> {
                    server.pullArchive(op, null, "data", tempFile)
                }
            } finally {
                tempFile.delete()
            }
        }

        // Covers the synthetic exception-handling path inside the outer `use {}`
        // block of pullArchive (line 349) - when archive.outputStream() throws,
        // the response body stream's `use` must still close cleanly.
        "pullArchive propagates IOException when output stream cannot be opened" {
            enqueue(200, "archive-content")

            // Pointing the archive at a non-existent directory makes
            // archive.outputStream() fail with FileNotFoundException.
            val unwritable = File("/nonexistent-directory/that/does/not/exist/file.tar.gz")
            val op = makeOperation(type = RemoteOperationType.PULL)

            shouldThrow<IOException> {
                server.pullArchive(op, null, "data", unwritable)
            }
        }

        // Covers the synthetic exception path of the inner `use {}` (line 350):
        // after both streams are open, force the copy itself to fail by having
        // MockWebServer close the socket mid-response.
        "pullArchive propagates IOException when response body is truncated" {
            mockWebServer.enqueue(
                MockResponse
                    .Builder()
                    .code(200)
                    // Promise more bytes than we will send, then close the socket
                    // during the response body - input.copyTo(output) throws.
                    .addHeader("Content-Length", "1000000")
                    .body("partial")
                    .onResponseBody(SocketEffect.CloseSocket())
                    .build(),
            )

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                val op = makeOperation(type = RemoteOperationType.PULL)
                shouldThrow<IOException> {
                    server.pullArchive(op, null, "data", tempFile)
                }
            } finally {
                tempFile.delete()
            }
        }

        "pullArchive includes auth header when token provided" {
            enqueue(200, "data")

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                val params = mapOf<String, Any>("api_token" to "pull-token")
                val op = makeOperation(parameters = params, type = RemoteOperationType.PULL)
                server.pullArchive(op, null, "vol1", tempFile)

                nextRequest().headers["Authorization"] shouldBe "Bearer pull-token"
            } finally {
                tempFile.delete()
            }
        }

        // =========================================
        // pushArchive tests
        // =========================================

        "pushArchive uploads file content" {
            enqueue(200, "")

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                tempFile.writeText("archive-data")
                val op = makeOperation(commitId = "commit-789")
                server.pushArchive(op, null, "data", tempFile)

                val recorded = nextRequest()
                recorded.url.encodedPath shouldBe "/api/v1/repos/myorg/myrepo/commits/commit-789/volumes/data"
                recorded.method shouldBe "PUT"

                // Read body from MockWebServer's recorded request (ByteString)
                recorded.body?.utf8() shouldBe "archive-data"
            } finally {
                tempFile.delete()
            }
        }

        "pushArchive throws when file does not exist" {
            val nonExistentFile = File("/nonexistent/path/archive.tar.gz")
            val op = makeOperation()

            shouldThrow<IOException> {
                server.pushArchive(op, null, "data", nonExistentFile)
            }
        }

        "pushArchive throws on server error" {
            enqueue(500, "Error")

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                tempFile.writeText("archive-data")
                val op = makeOperation()
                shouldThrow<IOException> {
                    server.pushArchive(op, null, "data", tempFile)
                }
            } finally {
                tempFile.delete()
            }
        }

        "pushArchive includes auth header when token provided" {
            enqueue(200, "")

            val tempFile = File.createTempFile("test-archive", ".tar.gz")
            try {
                tempFile.writeText("archive-data")
                val params = mapOf<String, Any>("api_token" to "push-token")
                val op = makeOperation(parameters = params)
                server.pushArchive(op, null, "vol1", tempFile)

                nextRequest().headers["Authorization"] shouldBe "Bearer push-token"
            } finally {
                tempFile.delete()
            }
        }

        // =========================================
        // pushMetadata tests
        // =========================================

        "pushMetadata sends POST with commit data" {
            enqueue(200, "")

            val op = makeOperation(commitId = "commit-abc")
            val commit = mapOf<String, Any>("timestamp" to "2024-01-01T00:00:00Z")
            server.pushMetadata(op, commit, false)

            val recorded = nextRequest()
            recorded.url.encodedPath shouldBe "/api/v1/repos/myorg/myrepo/commits"
            recorded.method shouldBe "POST"
        }

        "pushMetadata sends POST with isUpdate true" {
            enqueue(200, "")

            val op = makeOperation(commitId = "commit-abc")
            val commit = mapOf<String, Any>("timestamp" to "2024-01-01T00:00:00Z")
            server.pushMetadata(op, commit, true)

            nextRequest().method shouldBe "POST"
        }

        "pushMetadata throws on server error" {
            enqueue(500, "Error")

            val op = makeOperation()
            val commit = mapOf<String, Any>("timestamp" to "2024-01-01T00:00:00Z")
            shouldThrow<IOException> {
                server.pushMetadata(op, commit, false)
            }
        }

        "pushMetadata includes auth header when token provided" {
            enqueue(200, "")

            val params = mapOf<String, Any>("api_token" to "meta-token")
            val op = makeOperation(parameters = params)
            val commit = mapOf<String, Any>("timestamp" to "2024-01-01T00:00:00Z")
            server.pushMetadata(op, commit, false)

            nextRequest().headers["Authorization"] shouldBe "Bearer meta-token"
        }

        "pushMetadata sends no auth header when no token" {
            enqueue(200, "")

            val op = makeOperation()
            val commit = mapOf<String, Any>("timestamp" to "2024-01-01T00:00:00Z")
            server.pushMetadata(op, commit, false)

            nextRequest().headers["Authorization"] shouldBe null
        }

        // =========================================
        // executeRequest error path tests (via getCommit/listCommits)
        // =========================================

        "executeRequest handles empty response body" {
            enqueue(200, "")

            // getCommit calls executeRequest internally; with empty body it returns ""
            // Gson.fromJson("", ...) returns null, so getCommit returns null
            val result = server.getCommit(baseRemote(), baseParams, "commit-123")
            result shouldBe null
        }
    }
}

/**
 * Helper that replaces the previous `result!!` non-null assertion with a guarded
 * check that fails the test (rather than throwing NPE) when `result` is null.
 */
private inline fun <T : Any> T?.shouldNotBeNullAndAssert(block: (T) -> Unit) {
    if (this == null) {
        throw AssertionError("expected non-null result")
    }
    block(this)
}
