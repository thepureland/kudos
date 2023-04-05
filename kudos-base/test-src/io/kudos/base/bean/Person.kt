package io.kudos.base.bean

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import io.kudos.base.support.IIdEntity
import io.kudos.base.tree.ITreeNode
import jakarta.xml.bind.annotation.XmlRootElement
import java.time.LocalDate
import java.util.*


/**
 * 人物信息(for test)
 *
 * @author K
 * @since 1.0.0
 */
@XmlRootElement
class Person : IIdEntity<String>, ITreeNode<String?> {

    override var id: String? = null
    var parentId: String? = null
    var children: MutableList<ITreeNode<String?>> = mutableListOf()
    var name: String? = null
    var sex: String? = null
    var age = 0
    var weight = 0.0
    @get:JsonSerialize(using = LocalDateSerializer::class)
    @get:JsonDeserialize(using = LocalDateDeserializer::class)
    var birthday: LocalDate? = null
    var address: Address? = null
    var goods: List<String>? = null
    var contact: Map<String, String>? = null
    var active: Boolean? = null

    constructor() {}

    constructor(name: String?) {
        this.name = name
    }

    constructor(name: String?, sex: String?) {
        this.name = name
        this.sex = sex
    }

    fun sayHello() {
        println("Hello World!")
    }

    fun f(str: String) {
        println("Person.f()...$str")
    }

    override fun toString(): String {
        return "Person.toString()..."
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + if (address == null) 0 else address.hashCode()
        result = prime * result + age
        result = prime * result + if (birthday == null) 0 else birthday.hashCode()
        result = prime * result + if (contact == null) 0 else contact.hashCode()
        result = prime * result + if (goods == null) 0 else goods.hashCode()
        result = prime * result + if (name == null) 0 else name.hashCode()
        result = prime * result + if (sex == null) 0 else sex.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val otherPerson = other as Person
        if (address == null) {
            if (otherPerson.address != null) return false
        } else if (address != otherPerson.address) return false
        if (age != otherPerson.age) return false
        if (birthday == null) {
            if (otherPerson.birthday != null) return false
        } else if (birthday.toString() != otherPerson.birthday.toString()) return false
        if (contact == null) {
            if (otherPerson.contact != null) return false
        } else if (contact.toString() != otherPerson.contact.toString()) return false
        if (goods == null) {
            if (otherPerson.goods != null) return false
        } else if (goods.toString() != otherPerson.goods.toString()) return false
        if (name == null) {
            if (otherPerson.name != null) return false
        } else if (name != otherPerson.name) return false
        if (sex == null) {
            if (otherPerson.sex != null) return false
        } else if (sex != otherPerson.sex) return false
        return true
    }

    companion object {
        private const val serialVersionUID = -4651767804549188044L
    }

    override fun _getId(): String? = id

    override fun _getParentId(): String? = parentId

    override fun _getChildren(): MutableList<ITreeNode<String?>> = children

}