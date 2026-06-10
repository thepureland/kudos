package io.kudos.base.data.xml

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.Marshaller
import jakarta.xml.bind.Unmarshaller
import jakarta.xml.bind.annotation.XmlAnyElement
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import java.util.concurrent.ConcurrentHashMap
import javax.xml.XMLConstants
import javax.xml.namespace.QName
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.sax.SAXSource
import kotlin.reflect.KClass

/**
 * XML utility class.
 *
 * Performs XML <-> Java object conversion based on JAXB (Java Architecture for XML Binding).
 * JAXB uses OXM (Object XML Mapping) and is backed by StAX (JSR 173) for XML document processing.
 *
 * Core features:
 * 1. Object serialization: converts Java objects to XML strings (marshalling).
 * 2. Object deserialization: converts XML strings to Java objects (unmarshalling).
 * 3. Collection support: serializes XML whose root element is a Collection.
 * 4. Encoding control: supports specifying the XML encoding (default UTF-8).
 * 5. Namespaces: supports namespace-agnostic parsing.
 *
 * Class requirements:
 * - Supports data classes and plain classes.
 * - Must have a no-argument constructor.
 * - Mapped properties must be readable and writable (var).
 * - JAXB annotations can be used to control serialization behavior.
 *
 * Performance optimizations:
 * - Caches JAXBContext in a ConcurrentHashMap to avoid repeated creation.
 * - Marshaller / Unmarshaller are created on each call (they are not thread-safe).
 *
 * Use cases:
 * - Web-service data exchange (SOAP, REST).
 * - Configuration parsing.
 * - Data persistence.
 * - Inter-system data transfer.
 *
 * Notes:
 * - XML encoding must be UTF-8 (the default).
 * - When a Collection is the root element, wrap it with CollectionWrapper.
 * - Namespace handling must be configured according to the actual scenario.
 * - Thread safety: the JAXBContext cache is thread-safe, but Marshaller / Unmarshaller are not.
 *
 * @author K
 * @since 1.0.0
 */
object XmlKit {

    /** [JAXBContext] cache: reused by target [KClass] to avoid repeated construction ([JAXBContext] itself is thread-safe). */
    private val jaxbContexts = ConcurrentHashMap<KClass<*>, JAXBContext>()

    /**
     * Serialization (marshalling): converts the bean to XML with the specified encoding.
     *
     * @param root root object to serialize
     * @param encoding encoding name; defaults to UTF-8
     * @return the serialized XML string
     * @author K
     * @since 1.0.0
     */
    fun toXml(root: Any, encoding: String = "UTF-8"): String {
        val writer = StringWriter()
        createMarshaller(root::class, encoding).marshal(root, writer)
        return writer.toString()
    }

    /**
     * Serialization (marshalling), specifically for the case where the root element is a Collection.
     *
     * @param T element type of the collection
     * @param root root container object to serialize
     * @param rootName root element name
     * @param clazz class
     * @param encoding encoding name; defaults to UTF-8
     * @return the serialized XML string
     * @author K
     * @since 1.0.0
     */
    fun <T: Any> toXml(root: Collection<T>, rootName: String, clazz: KClass<T>, encoding: String = "UTF-8"): String {
        val wrapper = CollectionWrapper(root)
        val wrapperElement = JAXBElement(QName(rootName), CollectionWrapper::class.java, wrapper)
        val writer = StringWriter()
        createMarshaller(clazz, encoding).marshal(wrapperElement, writer)
        return writer.toString()
    }

    /**
     * Deserialization (unmarshalling): converts the XML string into an instance of the specified class.
     *
     * Security: external entity resolution (XXE) and external DTD loading are disabled on the underlying
     * SAX parser, so crafted payloads cannot read local files or trigger SSRF via entity expansion.
     *
     * @param T target type
     * @param xml XML string
     * @param clazz target class
     * @param ignoreNameSpace whether to ignore namespaces
     * @return an instance of the specified class
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> fromXml(xml: String, clazz: KClass<T>, ignoreNameSpace: Boolean = false): T {
        val reader = StringReader(xml)
        val sax = SAXParserFactory.newInstance()
        sax.isNamespaceAware = ignoreNameSpace
        // Harden against XXE: no external general/parameter entities, no external DTD fetching
        sax.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        sax.setFeature("http://xml.org/sax/features/external-general-entities", false)
        sax.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        sax.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        val xmlReader = sax.newSAXParser().xmlReader
        val source = SAXSource(xmlReader, InputSource(reader))
        val result = createUnmarshaller(clazz).unmarshal(source)
        return clazz.java.cast(result)
    }

    /**
     * Creates a Marshaller and sets the encoding. Not thread-safe; create one per call or use pooling.
     *
     * @param clazz class
     * @param encoding encoding name; defaults to UTF-8
     * @return the Marshaller
     * @author K
     * @since 1.0.0
     */
    private fun createMarshaller(clazz: KClass<*>, encoding: String = "UTF-8"): Marshaller {
        val jaxbContext: JAXBContext = getJaxbContext(clazz)
        val marshaller: Marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true) // Formatted output with automatic newlines per element; otherwise everything is on a single line.
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, false) // Whether to omit the XML header; defaults to false (do not omit).
//        marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "")
        if (encoding.isNotBlank()) {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding)
        }
        return marshaller
    }

    /**
     * Creates an Unmarshaller. Not thread-safe; create one per call or use pooling.
     *
     * @param clazz class
     * @return the Unmarshaller
     * @author K
     * @since 1.0.0
     */
    private fun createUnmarshaller(clazz: KClass<*>): Unmarshaller = getJaxbContext(clazz).createUnmarshaller()

    /**
     * Gets or creates a [JAXBContext] for the specified class.
     * Also registers [CollectionWrapper] so that Collection-as-root cases can be handled.
     *
     * The context is built at most once per class via `computeIfAbsent` and then served from the cache
     * ([JAXBContext] itself is thread-safe and safe to share).
     *
     * @param clazz target class
     * @return the corresponding [JAXBContext]
     * @author K
     * @since 1.0.0
     */
    private fun getJaxbContext(clazz: KClass<*>): JAXBContext =
        jaxbContexts.computeIfAbsent(clazz) {
            JAXBContext.newInstance(clazz.java, CollectionWrapper::class.java)
        }

    /**
     * Wrapper for the case where the root element is a Collection.
     *
     * @author K
     * @since 1.0.0
     */
    class CollectionWrapper(
        /** Wrapped collection contents; JAXB automatically expands each element by type via `@XmlAnyElement`. */
        @set:XmlAnyElement
        var item: Collection<*>
    )

}