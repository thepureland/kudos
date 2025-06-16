package io.kudos.base.lang.reflect

import io.kudos.base.bean.validation.constraint.annotations.AtLeast
import io.kudos.base.bean.validation.constraint.annotations.NotNullOn
import io.kudos.base.enums.ienums.IDictEnum
import io.kudos.base.enums.impl.SexEnum
import io.kudos.base.support.Consts
import java.io.Serializable
import kotlin.reflect.full.starProjectedType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * XKClass测试用例
 *
 * @author K
 * @since 1.0.0
 */
class XKClassTest {

    @Test
    fun getEmptyConstructor() {
        assert(Dog::class.getEmptyConstructor() != null)
        assert(Parrot::class.getEmptyConstructor() == null)
        assert(Person::class.getEmptyConstructor() == null)
        assert(Student::class.getEmptyConstructor() == null)
    }

    @Test
    fun newInstance() {
        val parrot = Parrot::class.newInstance("name", 3)
        assertEquals(3, parrot.age)

        val dog = Dog::class.newInstance()
        assertEquals(0, dog.age)

        assertFailsWith<IllegalArgumentException> { Animal::class.newInstance() }
    }

    @Test
    fun isEnum() {
        assert(SexEnum::class.isEnum())
        assert(SexEnum.FEMALE::class.isEnum())
        assertFalse(Any::class.isEnum())
        assertFalse(XKClassTest::class.isEnum())
    }

    @Test
    fun isInterface() {
        assert(IDictEnum::class.isInterface())
        assertFalse(NotNullOn::class.isInterface())
        assertFalse(AbstractList::class.isInterface())
        assertFalse(XKClassTest::class.isInterface())
        assertFalse(SexEnum::class.isInterface())
        assertFalse(SexEnum.FEMALE::class.isInterface())
    }

    @Test
    fun isAbstractClass() {
        assert(AbstractList::class.isAbstractClass())
        assertFalse(IDictEnum::class.isAbstractClass())
        assertFalse(NotNullOn::class.isAbstractClass())
        assertFalse(XKClassTest::class.isAbstractClass())
        assertFalse(SexEnum::class.isAbstractClass())
        assertFalse(SexEnum.FEMALE::class.isAbstractClass())
    }

    @Test
    fun isAnnotation() {
        assert(NotNullOn::class.isAnnotation())
        assertFalse(AbstractList::class.isAnnotation())
        assertFalse(IDictEnum::class.isAnnotation())
        assertFalse(XKClassTest::class.isAnnotation())
        assertFalse(SexEnum::class.isAnnotation())
        assertFalse(SexEnum.FEMALE::class.isAnnotation())
    }

    @Test
    fun isAnnotationPresent() {
        assert(Organism::class.isAnnotationPresent(TestAnno::class))
        assert(Animal::class.isAnnotationPresent(TestAnno::class))
        assert(Bird::class.isAnnotationPresent(TestAnno::class))
        assertFalse(Flyable::class.isAnnotationPresent(TestAnno::class))
        assertFalse(Organism::class.isAnnotationPresent(AtLeast::class))
        assertFalse(Organism::class.isAnnotationPresent(SinceKotlin::class)) // 对于像SinceKotlin的注解无效
    }

    @Test
    fun getMemberProperty() {
        Parrot::class.getMemberProperty("age")
        Parrot::class.getMemberProperty("height")
        Parrot::class.getMemberProperty("name")
        assertFailsWith<NoSuchElementException> { Parrot::class.getMemberProperty("weight") } // private
        assertFailsWith<NoSuchElementException> { Parrot::class.getMemberProperty("xxxxx") }
        assertFailsWith<NoSuchElementException> { Parrot::class.getMemberProperty("ageValue") } // 只是参数，未定义为属性
    }

    @Test
    fun getMemberPropertyValue() {
        val parrot = Parrot("polly", 3)
        assertEquals(3, Parrot::class.getMemberPropertyValue(parrot, "age"))
        assertEquals(0.0, Parrot::class.getMemberPropertyValue(parrot, "height"))
        assertEquals("polly", Parrot::class.getMemberPropertyValue(parrot, "name"))
        assertFailsWith<NoSuchElementException> { Parrot::class.getMemberPropertyValue(parrot, "weight") }
        assertFailsWith<NoSuchElementException> { Parrot::class.getMemberPropertyValue(parrot, "xxxxx") }
        assertFailsWith<NoSuchElementException> { Parrot::class.getMemberPropertyValue(parrot, "ageValue") }
    }

    @Test
    fun getMemberFunction() {
        Parrot::class.getMemberFunction("sleep")
        Parrot::class.getMemberFunction("move")
        Parrot::class.getMemberFunction("fly")
        Parrot::class.getMemberFunction("speak", *Parrot::speak.parameters.toTypedArray())
        assertFailsWith<NoSuchElementException> { Parrot::class.getMemberFunction("xxxxx") }
    }

    @Test
    fun getSuperClass() {
        assertEquals(null, Any::class.getSuperClass())
        assertEquals(Bird::class, Parrot::class.getSuperClass())
        assertEquals(Any::class, Animal::class.getSuperClass())
        assertEquals(Any::class, Flyable::class.getSuperClass())
    }

    @Test
    fun getSuperInterfaces() {
        assertEquals(listOf(Speakable::class), Parrot::class.getSuperInterfaces())
        assertEquals(listOf(Flyable::class, Serializable::class), Bird::class.getSuperInterfaces())
        assertEquals(listOf(Organism::class), Speakable::class.getSuperInterfaces())
    }

    @Test
    fun getAllInterfaces() {
        assertEquals(
            listOf(Organism::class, Flyable::class, Serializable::class, Speakable::class),
            Parrot::class.getAllInterfaces()
        )
    }

    @Test
    fun firstMatchTypeOf() {
        assertEquals(
            Parrot::class,
            Parrot::class.firstMatchTypeOf(
                listOf(Animal::class.starProjectedType, Parrot::class.starProjectedType)
            ).classifier
        )
    }

    @Test
    fun getClassUpThatPresentAnnotation() {
        assertEquals(
            setOf(Bird::class, Animal::class, Organism::class),
            Parrot::class.getClassUpThatPresentAnnotation(TestAnno::class)
        )

        // 对于像SinceKotlin的注解无效
        assert(Parrot::class.getClassUpThatPresentAnnotation(SinceKotlin::class).isEmpty())
    }

    @Test
    fun getLocationOnDisk() {
        print(Organism::class.getLocationOnDisk())
    }


    @TestAnno
    @SinceKotlin("1.0.0")
    internal interface Organism {
        fun eat()
    }

    internal interface Flyable {
        fun fly()
    }

    internal interface Speakable : Organism {
        fun speak(content: String)
    }

    @TestAnno
    internal abstract class Animal : Organism {
        abstract val age: Int
        abstract fun move()
    }

    @TestAnno
    internal open class Bird(override val age: Int) : Animal(), Flyable, Serializable {
        private val weight: Double = 0.0
        val height: Double = 0.0
        override fun move() {}
        override fun eat() {}
        override fun fly() {}
        fun sleep() {}
    }

    internal class Parrot(val name: String, ageValue: Int) : Bird(ageValue), Speakable {
        override fun speak(content: String) {}
    }

    internal class Dog : Animal() {
        override val age: Int
            get() = 0

        override fun move() {}

        override fun eat() {}
    }

    internal annotation class TestAnno

    private open class Person { // 没有空构造器，也没有主构造器
        @Suppress(Consts.Suppress.UNUSED_PARAMETER)
        constructor(name:String, age: Int)
    }

    private class Student: Person { // 没有空构造器，也没有主构造器
        constructor(name:String, age: Int): super(name, age)
    }


}