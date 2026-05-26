package io.kudos.base.net.ftp

import io.kudos.base.logger.LogFactory
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


/**
 * FTP client utility class.
 *
 * @author K
 * @since 1.0.0
 */
class FtpClientKit {

    private val log = LogFactory.getLog(this::class)

    /**
     * Download a file.
     *
     * @param hostname  FTP server address
     * @param port      FTP server port
     * @param username  FTP login user
     * @param password  FTP login password
     * @param pathname  directory on the FTP server
     * @param filename  file name
     * @param localPath path of the downloaded file on the local machine
     * @return whether the download succeeded
     * @author K
     * @since 1.0.0
     */
    fun download(
        hostname: String,
        port: Int,
        username: String,
        password: String,
        pathname: String,
        filename: String,
        localPath: String
    ): Boolean {
        var flag = false
        val ftpClient = FTPClient()
        try {
            // Connect to the FTP server
            ftpClient.connect(hostname, port)
            // Log in to the FTP server
            ftpClient.login(username, password)
            // Verify whether the FTP login succeeded
            val replyCode: Int = ftpClient.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                return false
            }
            // Switch the working directory on the FTP server
            ftpClient.changeWorkingDirectory(pathname)
            val ftpFiles = ftpClient.listFiles()
            for (file in ftpFiles) {
                if (filename.equals(file.name, ignoreCase = true)) {
                    val localFile = File("$localPath/${file.name}")
                    val os: OutputStream = FileOutputStream(localFile)
                    ftpClient.retrieveFile(file.name, os)
                    os.close()
                }
            }
            ftpClient.logout()
            flag = true
        } catch (e: IOException) {
            log.error(e)
        } finally {
            if (ftpClient.isConnected) {
                try {
                    ftpClient.logout()
                } catch (_: IOException) {
                }
            }
        }
        return flag
    }

}