package io.kudos.ability.web.springmvc.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.servlet.HandlerInterceptor


/**
 * 跨域资源共享处理器的拦截器
 *
 * 一个基于 Spring MVC 的拦截器，用来在响应里塞 CORS 相关的响应头
 *
 * @author K
 * @since 1.0.0
 */
open class CorsHandlerInterceptor : HandlerInterceptor {

    /**
     * 在控制器方法执行前被调用
     * @return true 表示继续后续处理（包括调用控制器）；false 表示到此为止（通常用于处理预检 OPTIONS 直接返回）
     */
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {

        // Access-Control-Allow-Origin：告诉浏览器“允许哪个源访问”
        // 这里直接把请求头里的 Origin 原样回显（reflect）
        // 注意：如果请求没有 Origin（同源请求或非浏览器），这里可能是 null
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"))

        // Access-Control-Allow-Methods：允许的跨域方法
        // 这里设置为 "*"，表示所有方法都允许
        // 但规范中更推荐列出具体方法（如 GET,POST,PUT,DELETE,OPTIONS）
        response.setHeader("Access-Control-Allow-Methods", "*")

        // Access-Control-Allow-Credentials：是否允许携带 Cookie/Authorization 等凭证
        // 设为 true 表示允许
        // **规范要点**：当 allow-credentials = true 时，Allow-Origin **不能是"*"**，必须是具体域
        response.setHeader("Access-Control-Allow-Credentials", "true")

        // Access-Control-Allow-Headers：预检请求里客户端想发送的自定义请求头列表
        // 这里手工列了一些常见的头；如果客户端额外发送了别的自定义头，可能被拦截
        // 也可以用 "*"（在较新规范/浏览器里支持），或按 Access-Control-Request-Headers 回显
        response.setHeader(
            "Access-Control-Allow-Headers",
            "Authorization,Origin, X-Requested-With, Content-Type, Accept,Access-Token"
        )

        // 返回 true：继续执行后续拦截器/控制器
        // 如果想在预检请求（OPTIONS）时直接结束链路并返回 200，可在这里根据 request.method == "OPTIONS" 返回 false
        return true
    }
}