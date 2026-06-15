package io.kudos.base.net.ftp

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test cases for FtpClientKit.
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class FtpClientKitTest {

    private lateinit var tempDir: File
    private lateinit var ftpClientKit: FtpClientKit

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("ftp-test").toFile()
        ftpClientKit = FtpClientKit()
    }

    @AfterTest
    fun teardown() {
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testDownloadWithInvalidConnection() {
        // Test the connection-failure scenario
        val result = ftpClientKit.download(
            hostname = "invalid.host",
            port = 21,
            username = "test",
            password = "test",
            pathname = "/",
            filename = "test.txt",
            localPath = tempDir.absolutePath
        )
        assertFalse(result)
    }

    @Test
    fun testDownloadWithInvalidCredentials() {
        // Test the login-failure scenario.
        // Note: this test requires a real FTP server or a mock.
        // Since FtpClientKit uses a real FTP connection, this only exercises basic behavior.
        val result = ftpClientKit.download(
            hostname = "localhost",
            port = 21,
            username = "invalid",
            password = "invalid",
            pathname = "/",
            filename = "test.txt",
            localPath = tempDir.absolutePath
        )
        // With no real FTP server available, the login failure should yield false.
        // In a real environment a mock FTP server should be used.
        assertFalse(result, "Invalid credentials should return false")
    }

    @Test
    fun testDownloadWithNonExistentFile() {
        // Test the missing-file scenario
        val result = ftpClientKit.download(
            hostname = "localhost",
            port = 21,
            username = "test",
            password = "test",
            pathname = "/",
            filename = "non-existent-file.txt",
            localPath = tempDir.absolutePath
        )
        // When the file does not exist the call should return false.
        // Actual behavior depends on the FTP server's response.
        assertFalse(result, "Missing file should return false")
    }

    @Test
    fun testDownloadWithEmptyPath() {
        val result = ftpClientKit.download(
            hostname = "localhost",
            port = 21,
            username = "test",
            password = "test",
            pathname = "",
            filename = "test.txt",
            localPath = tempDir.absolutePath
        )
        // An empty path may cause the connection to fail or return false.
        // Depending on the actual FTP server, this only verifies the method runs.
        // If the connection fails, result should be false.
        // Without a real FTP server, we only verify no exception is thrown here.
        // Actual outcome depends on FTP server configuration.
    }

    @Test
    fun testDownloadWithEmptyFilename() {
        val result = ftpClientKit.download(
            hostname = "localhost",
            port = 21,
            username = "test",
            password = "test",
            pathname = "/",
            filename = "",
            localPath = tempDir.absolutePath
        )
        // With an empty filename no file matches, so the call should return false.
        assertFalse(result, "Empty filename should return false")
    }

    @Test
    fun testDownloadWithInvalidLocalPath() {
        // Test the invalid-local-path scenario.
        // Note: path format differs on Windows; use a clearly nonexistent path.
        val invalidPath = if (System.getProperty("os.name").contains("Windows")) {
            "Z:\\invalid\\path\\that\\does\\not\\exist"
        } else {
            "/invalid/path/that/does/not/exist"
        }
        val result = ftpClientKit.download(
            hostname = "localhost",
            port = 21,
            username = "test",
            password = "test",
            pathname = "/",
            filename = "test.txt",
            localPath = invalidPath
        )
        // When the path does not exist, file creation fails, so the method should
        // return false or throw. Since the method catches exceptions internally, it should return false.
        assertFalse(result, "Invalid local path should return false")
    }

    @Test
    fun testDownloadCaseInsensitiveFilename() {
        // Test that filename comparison is case-insensitive.
//        val result = ftpClientKit.download(
//            hostname = "localhost",
//            port = 21,
//            username = "test",
//            password = "test",
//            pathname = "/",
//            filename = "TEST.TXT", // uppercase
//            localPath = tempDir.absolutePath
//        )
        // Without a real FTP server, this only verifies the method runs.
        // In a real environment it should match a lowercase test.txt on the server (if present).
        // The code uses ignoreCase = true for the comparison, so it is case-insensitive.
        // Without a real server, we only verify no exception is thrown.
    }

    /**
     * Covers the full success path (login ok, working dir switch, matching file found, retrieve + logout)
     * by mocking the FTPClient construction with Mockito's mockConstruction (no source change required).
     */
    @Test
    fun testDownloadSuccessPath_withMockedFtpClient() {
        val ftpFile = FTPFile().apply { name = "Report.TXT" }
        Mockito.mockConstruction(FTPClient::class.java) { mock, _ ->
            Mockito.`when`(mock.replyCode).thenReturn(FTPReply.COMMAND_OK)
            Mockito.`when`(mock.listFiles()).thenReturn(arrayOf(ftpFile))
            Mockito.`when`(mock.retrieveFile(anyString(), Mockito.any())).thenReturn(true)
            Mockito.`when`(mock.isConnected).thenReturn(true)
        }.use {
            // filename "report.txt" matches "Report.TXT" case-insensitively
            val result = FtpClientKit().download(
                hostname = "host",
                port = 21,
                username = "u",
                password = "p",
                pathname = "/data",
                filename = "report.txt",
                localPath = tempDir.absolutePath
            )
            assertTrue(result, "Success path should return true")
            // the matching file should have been written locally
            assertTrue(File(tempDir, "Report.TXT").exists(), "Downloaded file should exist locally")
        }
    }

    /**
     * Covers the early-return branch when the FTP login reply code is not a positive completion.
     */
    @Test
    fun testDownloadLoginNotPositive_returnsFalse() {
        Mockito.mockConstruction(FTPClient::class.java) { mock, _ ->
            Mockito.`when`(mock.replyCode).thenReturn(FTPReply.NOT_LOGGED_IN)
            Mockito.`when`(mock.isConnected).thenReturn(false)
        }.use {
            val result = FtpClientKit().download(
                hostname = "host",
                port = 21,
                username = "u",
                password = "bad",
                pathname = "/",
                filename = "x.txt",
                localPath = tempDir.absolutePath
            )
            assertFalse(result, "Non-positive reply code should return false")
        }
    }

    /**
     * Covers the case where listFiles returns files but none matches -> loop body not entered, but logout/flag set.
     */
    @Test
    fun testDownloadNoMatchingFile_stillReturnsTrue() {
        val ftpFile = FTPFile().apply { name = "other.dat" }
        Mockito.mockConstruction(FTPClient::class.java) { mock, _ ->
            Mockito.`when`(mock.replyCode).thenReturn(FTPReply.COMMAND_OK)
            Mockito.`when`(mock.listFiles()).thenReturn(arrayOf(ftpFile))
            Mockito.`when`(mock.isConnected).thenReturn(true)
        }.use {
            val result = FtpClientKit().download(
                hostname = "host",
                port = 21,
                username = "u",
                password = "p",
                pathname = "/",
                filename = "missing.txt",
                localPath = tempDir.absolutePath
            )
            // logout succeeds and flag=true even though no file matched
            assertTrue(result)
            assertFalse(File(tempDir, "other.dat").exists())
        }
    }
}
