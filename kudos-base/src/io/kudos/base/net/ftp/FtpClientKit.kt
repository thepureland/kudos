package io.kudos.base.net.ftp

import io.kudos.base.logger.LogFactory
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


/**
 * FTP客户端工具类
 *
 * @author K
 * @since 1.0.0
 */
class FtpClientKit {

    private val log = LogFactory.getLog(this)

    /**
     * 下载文件
     *
     * @param hostname  FTP服务器地址
     * @param port      FTP服务器端口号
     * @param username  FTP登录帐号
     * @param password  FTP登录密码
     * @param pathname  FTP服务器文件目录
     * @param filename  文件名称
     * @param localPath 下载后的文件路径
     * @return 是否下载成功
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
            //连接FTP服务器
            ftpClient.connect(hostname, port)
            //登录FTP服务器
            ftpClient.login(username, password)
            //验证FTP服务器是否登录成功
            val replyCode: Int = ftpClient.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                return false
            }
            //切换FTP目录
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