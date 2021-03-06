package sorting.api.user;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sorting.api.common.Constants;
import sorting.api.common.Result;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequestMapping("/user")
@RestController
public class UserController {
    @Autowired
    private UserRepo userRepo;

    /**
     * 登录
     * @param username 用户名
     * @param branchCode 网点编码
     * @param password 密码
     * @param captcha  验证码
     */
    @PostMapping("/login")
    public Result login(String username, String branchCode, String password, String captcha, HttpSession session) {
        String sessionCaptcha = (String)session.getAttribute("login_captcha");
        if (!(sessionCaptcha != null && sessionCaptcha.equals(captcha))) {
            return Result.fail(2).message("验证码错误");
        }
        Optional<User> userOpt = userRepo.findByPhoneOrCode(username, username);
        if (!userOpt.isPresent()) {
            return Result.fail(3).message("用户名不存在");
        }
        User user = userOpt.get();
        if (!StringUtils.equals(user.getBranchCode(), branchCode)) {
            return Result.fail(3).message("用户名不存在");
        }
        if (!PasswordUtils.isRight(password, user.getPassword())) {
            return Result.fail(4).message("密码错误");
        }

        session.setAttribute(Constants.SESSION_USER_KEY, user);
        session.setMaxInactiveInterval(60 * 60 * 12);

        return Result.ok(user);
    }

    @GetMapping("/session")
    public Map<String, Object> session(HttpSession session) {
        Map<String, Object> ret = new HashMap<>();
        ret.put("user", session.getAttribute(Constants.SESSION_USER_KEY));
        return ret;
    }

    @GetMapping("/login_captcha")
    public void captcha(Integer width, Integer height, HttpServletResponse response, HttpSession session) {
        try {
            width = width != null ? width : 160;
            height = height != null ? height : 50;
            BufferedImage captchaImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            String randomText = CaptchaUtils.drawRandomText(width, height, captchaImage);
            session.setAttribute("login_captcha", randomText);
            response.setContentType("image/png");
            OutputStream out = response.getOutputStream();
            ImageIO.write(captchaImage ,"png", out);
            out.flush();
            out.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @PostMapping("/logout")
    public Result logout(HttpSession session) {
        session.removeAttribute(Constants.SESSION_USER_KEY);
        return Result.ok();
    }

    @GetMapping("/next_code")
    public String genNextCode() {
        return userRepo.findTopByOrderByCreateAtDesc()
            .map(user -> {
                int nextNum = Integer.parseInt(user.getCode()) + 1;
                return StringUtils.leftPad(String.valueOf(nextNum), 4, "0");
            })
            .orElse("0001");
    }

    /**
     * 注册
     * @param user 注册用户信息
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        if (userRepo.existsByPhone(user.getPhone())) {
            return Result.fail().message("手机号已被注册");
        }
        if (userRepo.existsByCode(user.getCode())) {
            return Result.fail().message("编号已存在");
        }
        user.setPassword(PasswordUtils.ciphertext(user.getPassword()));
        user.setCreateAt(new Date());
        user = userRepo.save(user);
        return Result.from(user != null);
    }

    @PutMapping("/password")
    public Result modifyPassword(String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepo.findById(SessionUserUtils.getUser().getId());
        if (!userOpt.isPresent()) {
            return Result.fail();
        }
        User user = userOpt.get();
        if (!PasswordUtils.isRight(oldPassword, user.getPassword())) {
            return Result.fail(2).message("旧密码错误");
        }
        user.setPassword(PasswordUtils.ciphertext(newPassword));
        user = userRepo.save(user);
        return Result.from(user != null);
    }

    @RequestMapping("/not_logged_in")
    @ResponseStatus(code= HttpStatus.UNAUTHORIZED)
    public Result notLoggedIn() {
        return Result.fail().message("未登录，无效的请求");
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
