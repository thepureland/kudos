package io.kudos.base.net.ftp

import org.soul.base.net.ftp.FtpClientTool


/**
 * FTP客户端工具类
 *
 * @author K
 * @since 1.0.0
 */
class FtpClientKit {

    /**
     * 下载文件
     *
     * @param hostname  FTP服务器地址
     * @param port      FTP服务器端口号
     * @param username  FTP登录帐号
     * @param password  FTP登录密码
     * @param pathname  FTP服务器文件目录
     * @param filename  文件名称
     * @param localpath 下载后的文件路径
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
        localpath: String
    ): Boolean = FtpClientTool.download(hostname, port, username, password, pathname, filename, localpath)

}