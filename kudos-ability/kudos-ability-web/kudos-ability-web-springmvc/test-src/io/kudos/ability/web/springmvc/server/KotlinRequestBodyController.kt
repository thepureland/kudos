package io.kudos.ability.web.springmvc.server

import io.kudos.base.annotations.IgnoreApiResponseWrap
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import java.time.LocalDateTime

/**
 * A request body shaped exactly like the ones the microservice admin APIs use: a Kotlin data class,
 * some parameters defaulted, no no-arg constructor.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class KotlinBodyRequest(

    /** Required, no default. */
    val name: String,

    /** Defaulted **non-nullable** — the shape Jackson cannot fabricate without the Kotlin module. */
    val effect: String = "ALLOW",

    /** Defaulted nullable. */
    val note: String? = null,

    /** A java.time value, to confirm the time module is discovered as well. */
    val at: LocalDateTime? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Exists to hold the HTTP boundary still.
 *
 * The bug this guards against was invisible to every unit test in the repository, because unit tests
 * call services directly and never serialize anything: a Kotlin data class has no no-arg
 * constructor, so without the Jackson Kotlin module *every* `@RequestBody` taking one fails at
 * runtime with "no Creators, like default constructor, exist". It was found by the first real JSON
 * POST ever made against the admin API — roughly two dozen endpoints had never worked.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@RestController
@RequestMapping("/test")
class KotlinRequestBodyController {

    @IgnoreApiResponseWrap
    @PostMapping("/kotlinBody")
    fun echo(@RequestBody request: KotlinBodyRequest): String =
        "${request.name}|${request.effect}|${request.note ?: "-"}|${request.at ?: "-"}"
}
