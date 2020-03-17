package sorting.api.user;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import sorting.api.common.Constants;

public class SessionUserUtils {
    public static User getUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (User)attributes.getRequest().getSession().getAttribute(Constants.SESSION_USER_KEY);
    }
}
