/*
 * Copyright Datadatdat.
 */

package com.datadatdat.remote.datadatdat.client

import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.OverrideMockKs
import java.io.Console
import java.net.URI

class DatadadatRemoteClientTest : StringSpec() {
    @MockK
    lateinit var console: Console

    @InjectMockKs
    @OverrideMockKs
    var client = DatadadatRemoteClient()

    override fun beforeTest(testCase: TestCase) = MockKAnnotations.init(this)

    override fun afterTest(
        testCase: TestCase,
        result: TestResult,
    ) {
        clearAllMocks()
    }

    init {
        "get provider returns datadatdat" {
            client.getProvider() shouldBe "datadatdat"
        }

        "parsing HTTP URI with port succeeds" {
            val result = client.parseUri(URI("http://localhost:8080/myorg/myrepo"), emptyMap())
            result["api_base_url"] shouldBe "http://localhost:8080"
            result["org"] shouldBe "myorg"
            result["repo"] shouldBe "myrepo"
            result["api_token"] shouldBe null
        }

        "parsing HTTPS URI without port succeeds" {
            val result = client.parseUri(URI("https://data.datadatdat.io/myorg/myrepo"), emptyMap())
            result["api_base_url"] shouldBe "https://data.datadatdat.io"
            result["org"] shouldBe "myorg"
            result["repo"] shouldBe "myrepo"
            result["api_token"] shouldBe null
        }

        "parsing URI with api_token property succeeds" {
            val result =
                client.parseUri(
                    URI("http://localhost:8080/myorg/myrepo"),
                    mapOf("api_token" to "secret123"),
                )
            result["api_base_url"] shouldBe "http://localhost:8080"
            result["org"] shouldBe "myorg"
            result["repo"] shouldBe "myrepo"
            result["api_token"] shouldBe "secret123"
        }

        "parsing URI with invalid scheme fails" {
            shouldThrow<IllegalArgumentException> {
                client.parseUri(URI("ftp://localhost:8080/myorg/myrepo"), emptyMap())
            }
        }

        "parsing URI without host fails" {
            shouldThrow<IllegalArgumentException> {
                client.parseUri(URI("http:///myorg/myrepo"), emptyMap())
            }
        }

        "parsing URI without path fails" {
            shouldThrow<IllegalArgumentException> {
                client.parseUri(URI("http://localhost:8080"), emptyMap())
            }
        }

        "parsing URI with wrong path format fails - too few parts" {
            shouldThrow<IllegalArgumentException> {
                client.parseUri(URI("http://localhost:8080/myorg"), emptyMap())
            }
        }

        "parsing URI with wrong path format fails - too many parts" {
            shouldThrow<IllegalArgumentException> {
                client.parseUri(URI("http://localhost:8080/myorg/myrepo/extra"), emptyMap())
            }
        }

        "parsing URI with invalid property fails" {
            shouldThrow<IllegalArgumentException> {
                client.parseUri(
                    URI("http://localhost:8080/myorg/myrepo"),
                    mapOf("invalid_prop" to "value"),
                )
            }
        }

        "toUri with all properties succeeds" {
            val (uri, params) =
                client.toUri(
                    mapOf(
                        "api_base_url" to "http://localhost:8080",
                        "org" to "myorg",
                        "repo" to "myrepo",
                        "api_token" to "secret123",
                    ),
                )
            uri shouldBe "http://localhost:8080/myorg/myrepo"
            params.size shouldBe 1
            params["api_token"] shouldBe "*****"
        }

        "toUri without api_token succeeds" {
            val (uri, params) =
                client.toUri(
                    mapOf(
                        "api_base_url" to "https://data.datadatdat.io",
                        "org" to "myorg",
                        "repo" to "myrepo",
                    ),
                )
            uri shouldBe "https://data.datadatdat.io/myorg/myrepo"
            params.size shouldBe 0
        }

        "toUri with missing api_base_url fails" {
            shouldThrow<IllegalArgumentException> {
                client.toUri(
                    mapOf(
                        "org" to "myorg",
                        "repo" to "myrepo",
                    ),
                )
            }
        }

        "toUri with missing org fails" {
            shouldThrow<IllegalArgumentException> {
                client.toUri(
                    mapOf(
                        "api_base_url" to "http://localhost:8080",
                        "repo" to "myrepo",
                    ),
                )
            }
        }

        "toUri with missing repo fails" {
            shouldThrow<IllegalArgumentException> {
                client.toUri(
                    mapOf(
                        "api_base_url" to "http://localhost:8080",
                        "org" to "myorg",
                    ),
                )
            }
        }

        "getParameters with api_token in properties succeeds" {
            val params =
                client.getParameters(
                    mapOf(
                        "api_base_url" to "http://localhost:8080",
                        "org" to "myorg",
                        "repo" to "myrepo",
                        "api_token" to "secret123",
                    ),
                )
            params["api_token"] shouldBe "secret123"
        }

        "getParameters prompts for api_token when not in properties" {
            every { console.readPassword(any()) } returns "prompted_token".toCharArray()
            val params =
                client.getParameters(
                    mapOf(
                        "api_base_url" to "http://localhost:8080",
                        "org" to "myorg",
                        "repo" to "myrepo",
                    ),
                )
            params["api_token"] shouldBe "prompted_token"
        }

        "parsing URI handles trailing slash" {
            val result = client.parseUri(URI("http://localhost:8080/myorg/myrepo/"), emptyMap())
            result["api_base_url"] shouldBe "http://localhost:8080"
            result["org"] shouldBe "myorg"
            result["repo"] shouldBe "myrepo"
        }

        "parsing URI with default HTTP port (80)" {
            val result = client.parseUri(URI("http://data.datadatdat.io:80/myorg/myrepo"), emptyMap())
            result["api_base_url"] shouldBe "http://data.datadatdat.io:80"
            result["org"] shouldBe "myorg"
            result["repo"] shouldBe "myrepo"
        }

        "parsing URI with default HTTPS port (443)" {
            val result = client.parseUri(URI("https://data.datadatdat.io:443/myorg/myrepo"), emptyMap())
            result["api_base_url"] shouldBe "https://data.datadatdat.io:443"
            result["org"] shouldBe "myorg"
            result["repo"] shouldBe "myrepo"
        }
    }
}
