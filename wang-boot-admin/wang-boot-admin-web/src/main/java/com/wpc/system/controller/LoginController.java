package com.wpc.system.controller;

import com.wpc.SessionUtil;
import com.wpc.base.controller.BaseController;
import com.wpc.common.utils.image.CaptchaUtils;
import com.wpc.common.utils.image.vcode.Captcha;
import com.wpc.common.utils.image.vcode.GifCaptcha;
import com.wpc.common.utils.net.IpUtils;
import com.wpc.log.LogManager;
import com.wpc.log.factory.LogTaskFactory;
import com.wpc.shiro.JCaptchaValidateFilter;
import com.wpc.shiro.MyFormAuthenticationFilter;
import com.wpc.system.model.User;
import com.wpc.system.model.node.MenuNode;
import com.wpc.system.service.IMenuService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;

@Controller
public class LoginController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private MyFormAuthenticationFilter formAuthenticationFilter = new MyFormAuthenticationFilter();

    @Autowired
    private SessionDAO sessionDAO;

    @Autowired
    private IMenuService menuService;

    /**
     * 跳转到主页
     */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Model model) {
        //获取菜单列表
        List<Long> roleList = SessionUtil.getPrincipal().getRoleIds();
        if (roleList == null || roleList.size() == 0) {
            SessionUtil.getSubject().logout();
            model.addAttribute(formAuthenticationFilter.getMessageParam(), "该用户没有角色，无法登陆");
            return "/login";
        }
        List<MenuNode> menus = menuService.getMenusByRoleIds(roleList);
        List<MenuNode> titles = MenuNode.buildTitle(menus);

        model.addAttribute("titles", titles);

        //获取用户头像
        User user = SessionUtil.getUser();
        String avatar = user.getAvatar();
        model.addAttribute("avatar", avatar);

        return "index";
    }

    @RequestMapping(value="/register", method = RequestMethod.GET)
    public String register() {
        return "register";
    }

    /**
     * 跳转到登录页面
     */
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String login() {
        if (SessionUtil.isAuthenticated() || SessionUtil.getUser() != null) {
            return REDIRECT + "/";
        } else {
            return "/login";
        }
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String loginValidation(HttpServletRequest request, Model model) {

//        Principal principal = SessionUtil.getPrincipal();
//        if (principal != null) {
//            SavedRequest savedRequest = WebUtils.getSavedRequest(request);
//            // 获取保存的URL
//            if (savedRequest == null || savedRequest.getRequestUrl() == null) {
//                return "redirect:/";
//            } else {
//                //String url = savedRequest.getRequestUrl().substring(12, savedRequest.getRequestUrl().length());
//            	return "redirect:" + savedRequest.getRequestUrl();
//            }
//        }

        String username = WebUtils.getCleanParam(request, formAuthenticationFilter.getUsernameParam());
        String password = WebUtils.getCleanParam(request, formAuthenticationFilter.getPasswordParam());
        boolean rememberMe = WebUtils.isTrue(request, formAuthenticationFilter.getRememberMeParam());
        String message = (String)request.getAttribute(formAuthenticationFilter.getMessageParam());
        String exception = (String)request.getAttribute(MyFormAuthenticationFilter.DEFAULT_ERROR_KEY_ATTRIBUTE_NAME);

        if (StringUtils.isBlank(message) || StringUtils.equals(message, "null")){
            message = exception;
        }

        model.addAttribute(formAuthenticationFilter.getUsernameParam(), username);
//        model.addAttribute(formAuthenticationFilter.getPasswordParam(), password);
//        model.addAttribute(formAuthenticationFilter.getRememberMeParam(), rememberMe);
        model.addAttribute(formAuthenticationFilter.getMessageParam(), message);
//        model.addAttribute(MyFormAuthenticationFilter.DEFAULT_ERROR_KEY_ATTRIBUTE_NAME, exception);

        if (logger.isDebugEnabled()){
            logger.debug("login fail, active session size: {}, message: {}, exception: {}",
                    sessionDAO.getActiveSessions().size(), message, exception);
        }
        
        return "login";
    }

    @RequestMapping(value = "/logout")
    public String doLogout(HttpServletRequest request) {
        LogManager.me().executeLog(LogTaskFactory.exitLog(SessionUtil.getUser().getId(), IpUtils.getIpAddress(request)));
        SecurityUtils.getSubject().logout();
        return "login";
    }

    /**
     * 获取验证码（Gif版本）
     * @param response
     */
    @RequestMapping(value="getGifCode",method= RequestMethod.GET)
    public void getGifCode(HttpServletResponse response){
        try {
            response.setHeader("Pragma", "No-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setContentType("image/gif");
            /**
             * gif格式动画验证码
             * 宽，高，位数。
             */
            Captcha captcha = new GifCaptcha(146,50,4);
            //输出
            ServletOutputStream out = response.getOutputStream();
            captcha.out(out);
            out.flush();
            //存入Shiro会话session
            SessionUtil.getSession().setAttribute(JCaptchaValidateFilter.DEFAULT_CAPTCHA_PARAM, captcha.text().toLowerCase());
        } catch (Exception e) {
            logger.debug("获取验证码异常：{}", e.getMessage());
        }
    }
    /**
     * 获取验证码（jpg版本）
     * @param response
     */
    @RequestMapping(value="getJPGCode",method= RequestMethod.GET)
    public void getJPGCode(HttpServletResponse response, HttpServletRequest request){
        CaptchaUtils.captcha(100, 40, 4, CaptchaUtils.CAPTCHA_TYPE.ALL, request, response);
        /*try {
            response.setHeader("Pragma", "No-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setContentType("image/jpg");
            *//**
             * jgp格式验证码
             * 宽，高，位数。
             *//*
            Captcha captcha = new SpecCaptcha(146,33,4);
            //输出
            captcha.out(response.getOutputStream());

            //存入Session
//            SessionUtil.getSession().setAttribute("_code",captcha.text().toLowerCase());
        } catch (Exception e) {
//            LoggerUtils.fmtError(getClass(),e, "获取验证码异常：%s",e.getMessage());
        }*/
    }
    
}
