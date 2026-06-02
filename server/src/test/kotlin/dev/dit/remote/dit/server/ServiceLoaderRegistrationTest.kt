/*
 * Copyright Dit.
 */

package dev.dit.remote.dit.server

import dev.dit.remote.RemoteServer
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.util.ServiceLoader

class ServiceLoaderRegistrationTest :
    StringSpec({
        "DitRemoteServer is discoverable as a dev.dit.remote.RemoteServer ServiceLoader provider" {
            ServiceLoader.load(RemoteServer::class.java).any { it is DitRemoteServer } shouldBe true
        }
    })
