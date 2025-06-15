package io.kudos.base.bean

import java.io.Serializable

/**
 * 地址信息(for test)
 *
 * @author K
 * @since 1.0.0
 */
@kotlinx.serialization.Serializable
class Address : Serializable {

    var province: String? = null
    var city: String? = null
    var street: String? = null
    var zipcode: String?  = null

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (city?.hashCode() ?: 0)
        result = prime * result + (province?.hashCode() ?: 0)
        result = prime * result + (street?.hashCode() ?: 0)
        result = prime * result + (zipcode?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val otherAddr: Address = other as Address
        if (city == null) {
            if (otherAddr.city != null) return false
        } else if (city != otherAddr.city) return false
        if (province == null) {
            if (otherAddr.province != null) return false
        } else if (province != otherAddr.province) return false
        if (street == null) {
            if (otherAddr.street != null) return false
        } else if (street != otherAddr.street) return false
        if (zipcode == null) {
            if (otherAddr.zipcode != null) return false
        } else if (zipcode != otherAddr.zipcode) return false
        return true
    }

    companion object {
        private const val serialVersionUID = -7450588278095526959L
    }
}