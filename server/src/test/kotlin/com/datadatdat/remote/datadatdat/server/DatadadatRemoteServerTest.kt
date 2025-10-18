/*
 * Copyright Datadatdat.
 */

package com.datadatdat.remote.datadatdat.server

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class DatadadatRemoteServerTest : StringSpec() {
    private val server = DatadadatRemoteServer()

    init {
        "getProvider returns datadatdat" {
            server.getProvider() shouldBe "datadatdat"
        }

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
    }
}
