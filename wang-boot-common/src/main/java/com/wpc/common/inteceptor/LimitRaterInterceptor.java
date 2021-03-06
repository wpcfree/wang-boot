package com.wpc.common.inteceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpc.common.Global;
import com.wpc.common.annotation.RateLimiter;
import com.wpc.common.bean.ResponseResult;
import com.wpc.common.limit.RedisRaterLimiter;
import com.wpc.common.utils.net.IpUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;

//@Component
public class LimitRaterInterceptor extends HandlerInterceptorAdapter {

    @Value("${wpc.rateLimit.enable}")
    private boolean rateLimitEnable;

    @Value("${wpc.rateLimit.limit}")
    private Integer limit;

    @Value("${wpc.rateLimit.timeout}")
    private Integer timeout;

    @Autowired
    private RedisRaterLimiter redisRaterLimiter;

    /**
     * 预处理回调方法，实现处理器的预处理（如登录检查）
     * 第三个参数为响应的处理器，即controller
     * 返回true，表示继续流程，调用下一个拦截器或者处理器
     * 返回false，表示流程中断，通过response产生响应
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        // IP限流 在线Demo所需 一秒限5个请求
        String token1 = redisRaterLimiter.acquireTokenFromBucket(IpUtils.getIpAddress(request), 5, 1000);
        if (StringUtils.isBlank(token1)) {
//            throw new RuntimeException("你手速怎么这么快，请点慢一点");
            try{
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                ResponseResult result = ResponseResult.errorByMsg("你手速怎么这么快，请点慢一点");
                PrintWriter writer =  response.getWriter();
                writer.write(new ObjectMapper().writeValueAsString(result));
                return false;
            }catch (IOException e){
                throw new RuntimeException(e);
            }
        }

        if(rateLimitEnable){
            String token2 = redisRaterLimiter.acquireTokenFromBucket(Global.LIMIT_ALL, limit, timeout);
            if (StringUtils.isBlank(token2)) {
//                throw new RuntimeException("当前访问总人数太多啦，请稍后再试");
                try{
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    ResponseResult result = ResponseResult.errorByMsg("当前访问总人数太多啦，请稍后再试");
                    PrintWriter writer =  response.getWriter();
                    writer.write(new ObjectMapper().writeValueAsString(result));
                    return false;
                }catch (IOException e){
                    throw new RuntimeException(e);
                }
            }
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);

        if (rateLimiter != null) {
            int limit = rateLimiter.limit();
            int timeout = rateLimiter.timeout();
            String token3 = redisRaterLimiter.acquireTokenFromBucket(method.getName(), limit, timeout);
            if (StringUtils.isBlank(token3)) {
//                throw new RuntimeException("当前访问人数太多啦，请稍后再试");
                try{
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    ResponseResult result = ResponseResult.errorByMsg("当前访问人数太多啦，请稍后再试");
                    PrintWriter writer =  response.getWriter();
                    writer.write(new ObjectMapper().writeValueAsString(result));
                    return false;
                }catch (IOException e){
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }

    /**
     * 当前请求进行处理之后，也就是Controller方法调用之后执行，
     * 但是它会在DispatcherServlet 进行视图返回渲染之前被调用。
     * 此时我们可以通过modelAndView对模型数据进行处理或对视图进行处理。
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {

    }

    /**
     * 方法将在整个请求结束之后，也就是在DispatcherServlet渲染了对应的视图之后执行。
     * 这个方法的主要作用是用于进行资源清理工作的。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
    }

}
