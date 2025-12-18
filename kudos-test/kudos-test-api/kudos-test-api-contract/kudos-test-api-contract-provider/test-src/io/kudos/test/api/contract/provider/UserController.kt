package io.kudos.test.api.contract.provider

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/users")
class UserController {

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long): ResponseEntity<UserDto> {
        val user = UserDto(id = id, name = "Tom")
        return ResponseEntity.ok(user)
    }
    
}

data class UserDto(
    val id: Long,
    val name: String
)