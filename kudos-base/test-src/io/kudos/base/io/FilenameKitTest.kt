package io.kudos.base.io

import io.kudos.base.enums.impl.OsEnum
import io.kudos.base.lang.SystemKit
import java.io.File
import kotlin.test.*

/**
 * test for FileKit
 *
 * @author ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class FilenameKitTest {

    // ----------- normalize --------------

    @Test
    fun normalizeNullReturnsNull() {
        assertNull(FilenameKit.normalize(null))
    }

    @Test
    fun normalizeSimplePaths() {
        if (SystemKit.currentOs() == OsEnum.WINDOWS) {
            // “/foo//” → “\foo/”
            assertEquals("\\foo\\", FilenameKit.normalize("/foo//"))

            // “/foo/./” → “\foo/”
            assertEquals("\\foo\\", FilenameKit.normalize("/foo/./"))

            // “/foo/../bar” → “\bar”
            assertEquals("\\bar", FilenameKit.normalize("/foo/../bar"))

            // “foo/bar/..” → “foo\”
            assertEquals("foo\\", FilenameKit.normalize("foo/bar/.."))
        } else {
            // “/foo//” → “/foo/”
            assertEquals("/foo/", FilenameKit.normalize("/foo//"))

            // “/foo/./” → “/foo/”
            assertEquals("/foo/", FilenameKit.normalize("/foo/./"))

            // “/foo/../bar” → “/bar”
            assertEquals("/bar", FilenameKit.normalize("/foo/../bar"))

            // “foo/bar/..” → “foo/”
            assertEquals("foo/", FilenameKit.normalize("foo/bar/.."))
        }
        // “../foo” → null (no parent)
        assertNull(FilenameKit.normalize("../foo"))
    }

    @Test
    fun normalizeWithUnixSeparatorFlag() {
        // Even if we supply windows-style separators, result uses Unix or Windows based on flag
        val raw = "C:\\foo\\..\\bar\\"
        // use Unix separators
        assertEquals("C:/bar/", FilenameKit.normalize(raw, true))
        // use Windows separators
        assertEquals("C:\\bar\\", FilenameKit.normalize(raw, false))
    }

    @Test
    fun normalizeNoEndSeparatorNullReturnsNull() {
        assertNull(FilenameKit.normalizeNoEndSeparator(null))
    }

    @Test
    fun normalizeNoEndSeparatorSimplePaths() {
        if (SystemKit.currentOs() == OsEnum.WINDOWS) {
            // “/foo//” → “\foo”
            assertEquals("\\foo", FilenameKit.normalizeNoEndSeparator("/foo//"))
            // “/foo/./” → “\foo”
            assertEquals("\\foo", FilenameKit.normalizeNoEndSeparator("/foo/./"))
            // “/foo/../bar/” → “\bar”
            assertEquals("\\bar", FilenameKit.normalizeNoEndSeparator("/foo/../bar/"))
        } else {
            // “/foo//” → “/foo”
            assertEquals("/foo", FilenameKit.normalizeNoEndSeparator("/foo//"))
            // “/foo/./” → “/foo”
            assertEquals("/foo", FilenameKit.normalizeNoEndSeparator("/foo/./"))
            // “/foo/../bar/” → “/bar”
            assertEquals("/bar", FilenameKit.normalizeNoEndSeparator("/foo/../bar/"))
        }
        // “foo/bar/..” → “foo”
        assertEquals("foo", FilenameKit.normalizeNoEndSeparator("foo/bar/.."))
        // “../foo” → null
        assertNull(FilenameKit.normalizeNoEndSeparator("../foo"))
    }

    @Test
    fun normalizeNoEndSeparatorWithUnixSeparatorFlag() {
        val raw = "C:\\foo\\..\\bar\\"
        // no end separator + Unix style
        assertEquals("C:/bar", FilenameKit.normalizeNoEndSeparator(raw, true))
        // no end separator + Windows style
        assertEquals("C:\\bar", FilenameKit.normalizeNoEndSeparator(raw, false))
    }

    // ----------- concat --------------

    @Test
    fun concatSimplePaths() {
        if (SystemKit.currentOs() == OsEnum.WINDOWS) {
            assertEquals("\\foo\\bar", FilenameKit.concat("/foo", "bar"))
            assertEquals("\\bar", FilenameKit.concat("/foo", "/bar"))
            assertEquals("C:\\bar", FilenameKit.concat("C:/foo", "C:/bar"))
            // when base ends with filename
            assertEquals("\\foo\\c.txt\\bar", FilenameKit.concat("/foo/c.txt", "bar"))
        } else {
            assertEquals("/foo/bar", FilenameKit.concat("/foo", "bar"))
            assertEquals("/bar", FilenameKit.concat("/foo", "/bar"))
            assertEquals("C:/bar", FilenameKit.concat("C:/foo", "C:/bar"))
            // when base ends with filename
            assertEquals("/foo/c.txt/bar", FilenameKit.concat("/foo/c.txt", "bar"))
        }
        // invalid → parent “/foo” + “../../baz” has no valid parent
        assertNull(FilenameKit.concat("/foo", "../../baz"))
    }

    @Test
    fun concatWithNulls() {
        assertNull(FilenameKit.concat(null, "bar"))
        assertNull(FilenameKit.concat("/foo", null))
    }

    // ----------- directoryContains --------------

    @Test
    fun directoryContainsBasicChecks() {
        // valid containment
        assertTrue(FilenameKit.directoryContains("/a/b", "/a/b/c.txt"))
        // not contains when same
        assertFalse(FilenameKit.directoryContains("/a/b", "/a/b"))
        // null child → false
        assertFalse(FilenameKit.directoryContains("/a/b", null))
        // invalid parent → 返回 false（不抛异常）
        assertFalse(FilenameKit.directoryContains("/not/exist/path", "/a"))
    }

    // ----------- separatorsTo --------------

    @Test
    fun separatorsToUnixWindowsSystem() {
        val mixed = "C:\\foo\\bar/baz"
        // to Unix
        assertEquals("C:/foo/bar/baz", FilenameKit.separatorsToUnix(mixed))
        // to Windows
        assertEquals("C:\\foo\\bar\\baz", FilenameKit.separatorsToWindows(mixed))
        // to System (assuming current OS uses '/' as separator)
        val expectedSys = mixed.replace('\\', File.separatorChar).replace('/', File.separatorChar)
        assertEquals(expectedSys, FilenameKit.separatorsToSystem(mixed))
    }

    @Test
    fun separatorsToNullReturnsNull() {
        assertNull(FilenameKit.separatorsToUnix(null))
        assertNull(FilenameKit.separatorsToWindows(null))
        assertNull(FilenameKit.separatorsToSystem(null))
    }

    // ----------- getPrefixLength / getPrefix --------------

    @Test
    fun getPrefixLengthVariousInputs() {
        // 空输入应返回 -1
        assertEquals(-1, FilenameKit.getPrefixLength(null))

        // “C:folder/file.txt” 前缀应为 "C:"（drive-relative）
        run {
            val input = "C:folder/file.txt"
            val prefixLen = FilenameKit.getPrefixLength(input)
            assertEquals(2, prefixLen)
            assertEquals("C:", input.substring(0, prefixLen))
        }

        // “C:/folder” 前缀应为 "C:/"
        run {
            val input = "C:/folder"
            val prefixLen = FilenameKit.getPrefixLength(input)
            assertEquals(3, prefixLen)
            assertEquals("C:/", input.substring(0, prefixLen))
        }

        // "/usr/bin" 前缀应为 "/"
        run {
            val input = "/usr/bin"
            val prefixLen = FilenameKit.getPrefixLength(input)
            assertEquals(1, prefixLen)
            assertEquals("/", input.substring(0, prefixLen))
        }

        // "//server/share/path" 前缀应为 "//server/"
        run {
            val input = "//server/share/path"
            val prefixLen = FilenameKit.getPrefixLength(input)
            assertEquals("//server/", input.substring(0, prefixLen))
        }

        // "~/folder" 前缀应为 "~/"
        run {
            val input = "~/folder"
            val prefixLen = FilenameKit.getPrefixLength(input)
            assertEquals(2, prefixLen)
            assertEquals("~/", input.substring(0, prefixLen))
        }

        // “relative/path” 无前缀，应返回 0
        assertEquals(0, FilenameKit.getPrefixLength("relative/path"))

        // 空字符串也视为无前缀
        assertEquals(0, FilenameKit.getPrefixLength(""))
    }

    @Test
    fun getPrefixReturnsCorrectPrefix() {
        assertEquals("", FilenameKit.getPrefix("relative/file.txt"))
        assertEquals("/", FilenameKit.getPrefix("/foo/bar"))
        assertEquals("C:/", FilenameKit.getPrefix("C:/foo/bar"))
        assertEquals("C:", FilenameKit.getPrefix("C:foo/bar"))
        assertEquals("~/", FilenameKit.getPrefix("~/docs/readme"))
        assertEquals("~user/", FilenameKit.getPrefix("~user/docs"))
        assertEquals("\\\\server\\", FilenameKit.getPrefix("\\\\server\\share\\file"))
    }

    // ----------- indexOfLastSeparator / indexOfExtension --------------

    @Test
    fun indexOfLastSeparatorAndExtension() {
        assertEquals(-1, FilenameKit.indexOfLastSeparator(null))
        assertEquals(-1, FilenameKit.indexOfLastSeparator("no-sep"))
        assertEquals(3, FilenameKit.indexOfLastSeparator("a/b/c"))
        assertEquals(3, FilenameKit.indexOfLastSeparator("a\\b\\c"))
        assertEquals(-1, FilenameKit.indexOfExtension(null))
        assertEquals(-1, FilenameKit.indexOfExtension("file"))
        assertEquals(4, FilenameKit.indexOfExtension("file.txt"))
        // if dot after last separator, ignore
        assertEquals(-1, FilenameKit.indexOfExtension("a/b.txt/c"))
    }

    // ----------- getPath / getPathNoEndSeparator / getFullPath / getFullPathNoEndSeparator --------------

    @Test
    fun getPathAndVariants() {
        assertNull(FilenameKit.getPath(null))
        assertNull(FilenameKit.getPathNoEndSeparator(null))
        assertNull(FilenameKit.getFullPath(null))
        assertNull(FilenameKit.getFullPathNoEndSeparator(null))

        // “C:/a/b/c.txt” → path “a/b/”
        assertEquals("a/b/", FilenameKit.getPath("C:/a/b/c.txt"))
        // no end separator → “a/b”
        assertEquals("a/b", FilenameKit.getPathNoEndSeparator("C:/a/b/c.txt"))
        // full path → “C:/a/b/”
        assertEquals("C:/a/b/", FilenameKit.getFullPath("C:/a/b/c.txt"))
        // full path no end sep → “C:/a/b”
        assertEquals("C:/a/b", FilenameKit.getFullPathNoEndSeparator("C:/a/b/c.txt"))

        // relative without separators
        assertEquals("", FilenameKit.getPath("file.txt"))
        assertEquals("", FilenameKit.getPathNoEndSeparator("file.txt"))
        assertEquals("", FilenameKit.getFullPath("file.txt"))
        assertEquals("", FilenameKit.getFullPathNoEndSeparator("file.txt"))
    }

    // ----------- getName / getBaseName --------------

    @Test
    fun getNameAndBaseName() {
        assertNull(FilenameKit.getName(null))
        assertNull(FilenameKit.getBaseName(null))
        assertEquals("file.txt", FilenameKit.getName("a/b/file.txt"))
        assertEquals("file", FilenameKit.getBaseName("a/b/file.txt"))
        // no extension
        assertEquals("file", FilenameKit.getName("a/b/file"))
        assertEquals("file", FilenameKit.getBaseName("a/b/file"))
        // trailing slash
        assertEquals("", FilenameKit.getName("a/b/"))
        assertEquals("", FilenameKit.getBaseName("a/b/"))
    }

    // ----------- getExtension / removeExtension --------------

    @Test
    fun getExtensionAndRemoveExtension() {
        assertNull(FilenameKit.removeExtension(null))
        assertEquals("txt", FilenameKit.getExtension("foo.txt"))
        assertEquals("foo", FilenameKit.removeExtension("foo.txt"))
        // no extension
        assertEquals("", FilenameKit.getExtension("foo"))
        assertEquals("foo", FilenameKit.removeExtension("foo"))
        // dot in path
        assertEquals("txt", FilenameKit.getExtension("a.b/foo.txt"))
        assertEquals("a.b/foo", FilenameKit.removeExtension("a.b/foo.txt"))
    }

    // ----------- equals / equalsOnSystem / equalsNormalized / equalsNormalizedOnSystem / equals with flags --------------

    @Test
    fun equalsVariantsCaseSensitivity() {
        // both null → equal
        assertTrue(FilenameKit.equals(null, null))
        // one null → false
        assertFalse(FilenameKit.equals("a", null))
        assertFalse(FilenameKit.equals(null, "a"))
        // case-sensitive compare
        assertTrue(FilenameKit.equals("a.txt", "a.txt"))
        assertFalse(FilenameKit.equals("A.txt", "a.txt"))

        // equalsOnSystem 应与 equals(..., normalized=false, caseSensitivity=null) 等价
        val a = "A.TXT"
        val b = "a.txt"
        val viaFlags = FilenameKit.equals(a, b, normalized = false, caseSensitivity = null)
        val viaOnSystem = FilenameKit.equalsOnSystem(a, b)
        assertEquals(viaFlags, viaOnSystem)

        // equalsNormalized: normalize then case-sensitive compare
        assertTrue(FilenameKit.equalsNormalized("/foo/../bar", "/bar"))

        // equalsNormalizedOnSystem 应与 equals(..., normalized=true, caseSensitivity=null) 等价
        val x = "/FOO/../bar"
        val y = "/bar"
        val viaNormFlags = FilenameKit.equals(x, y, normalized = true, caseSensitivity = null)
        val viaNormOnSystem = FilenameKit.equalsNormalizedOnSystem(x, y)
        assertEquals(viaNormFlags, viaNormOnSystem)

        // equals with explicit flags
        // normalized=true, caseSensitivity=true → case-sensitive
        assertTrue(FilenameKit.equals("/foo/../baz", "/baz", normalized = true, caseSensitivity = true))
        assertFalse(FilenameKit.equals("/foo/../Baz", "/baz", normalized = true, caseSensitivity = true))
        // normalized=false, case-insensitive
        assertTrue(FilenameKit.equals("Foo.txt", "foo.txt", normalized = false, caseSensitivity = false))
    }

    // ----------- isExtension --------------

    @Test
    fun isExtensionSingleAndArray() {
        // null filename → false
        assertFalse(FilenameKit.isExtension(null, "txt"))
        // 没有扩展名 → false（对于指定的非空扩展列表）
        assertFalse(FilenameKit.isExtension("foo", "txt"))
        // 单个扩展名
        assertTrue(FilenameKit.isExtension("file.txt", "txt"))
        assertFalse(FilenameKit.isExtension("file.TXT", "txt"))

        // 数组变体
        val exts: Array<String?> = arrayOf("txt", "jpg")
        assertTrue(FilenameKit.isExtension("a.jpg", exts))
        assertFalse(FilenameKit.isExtension("a.png", exts))

        // 集合变体
        val extList = listOf("mp3", "wav")
        assertTrue(FilenameKit.isExtension("song.wav", extList))
        assertFalse(FilenameKit.isExtension("song.txt", extList))

        // 空数组或空集合意味着“只有没有扩展名的文件会匹配”，因此对没有扩展名的文件返回 true
        assertTrue(FilenameKit.isExtension("file", arrayOf()))
        assertTrue(FilenameKit.isExtension("file", emptyList()))

        // 单个 null 元素的数组也表示“只有没有扩展名的文件会匹配”，对没有扩展名的文件返回 true
        assertFalse(FilenameKit.isExtension(null, arrayOf("a.jpg")))
    }


    // ----------- wildcardMatch / wildcardMatchOnSystem / wildcardMatch with caseSensitivity --------------

    @Test
    fun wildcardMatchBasic() {
        // simple patterns
        assertTrue(FilenameKit.wildcardMatch("c.txt", "*.txt"))
        assertFalse(FilenameKit.wildcardMatch("c.txt", "*.jpg"))
        assertTrue(FilenameKit.wildcardMatch("a/b/c.txt", "a/b/*"))
        assertTrue(FilenameKit.wildcardMatch("c.txt", "*.???"))
        assertFalse(FilenameKit.wildcardMatch("c.txt", "*.????"))

        // null comparisons
        assertTrue(FilenameKit.wildcardMatch(null, null))
        assertFalse(FilenameKit.wildcardMatch("a", null))
        assertFalse(FilenameKit.wildcardMatch(null, "a"))

        // wildcardMatchOnSystem 应与调用 wildcardMatch(..., null)（即使用 IOCase.SYSTEM）结果一致
        val onSystem = FilenameKit.wildcardMatchOnSystem("ABC.txt", "*.txt")
        val viaWildcard = FilenameKit.wildcardMatch("ABC.txt", "*.txt", null)
        assertEquals(viaWildcard, onSystem)

        // wildcardMatch with explicit caseSensitivity
        assertTrue(FilenameKit.wildcardMatch("ABC.TXT", "*.txt", false))  // 忽略大小写
        assertFalse(FilenameKit.wildcardMatch("ABC.TXT", "*.txt", true))   // 区分大小写
    }
}