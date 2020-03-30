package sorting.config;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import sorting.api.common.Constants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 验证请求是否有效的拦截器
 */
public class RequestValidateInterceptor implements HandlerInterceptor {
    private final String[] WHITE_LIST = new String[]{
        "/error",
        "/user/next_code", "/user/login", "/user/logout", "/user/register",
        "/user/login_captcha", "/user/not_logged_in", "/user/ping",
        "/app_version/latest_info", "/app_version/latest",
        "/scheme/all"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) throws Exception {
        // 会话是否有效

        if (ArrayUtils.contains(WHITE_LIST, request.getRequestURI())) {
            return true;
        }

        Object user = request.getSession().getAttribute(Constants.SESSION_USER_KEY);
        if(user == null) {
            request.getRequestDispatcher("/user/not_logged_in").forward(request, response);
            return false;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }
}
