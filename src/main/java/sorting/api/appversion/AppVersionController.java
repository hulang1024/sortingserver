package sorting.api.appversion;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sorting.api.common.Result;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/app_version")
public class AppVersionController {
    @GetMapping("/latest_info")
    public Result getLatestVersionInfo(HttpServletRequest request) {
        File releaseDir = new File("E:\\work\\sortingserver\\target\\classes\\release");
        File[] files = releaseDir.listFiles();
        if (files == null || files.length == 0) {
            return Result.fail(1);
        }
        String fileName = files[0].getName();
        Map<String, Object> versionInfo = new HashMap<>();
        versionInfo.put("version", fileName.substring(0, fileName.length() - 4));
        versionInfo.put("url", "app_version/latest");

        return Result.ok(versionInfo);
    }

    @GetMapping("/latest")
    public void latest(HttpServletResponse response) {
        File releaseDir = new File("E:\\work\\sortingserver\\target\\classes\\release");
        File[] files = releaseDir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        try {
            InputStream in = null;
            in = new BufferedInputStream(new FileInputStream(files[0]));
            response.setContentType("application/octet-stream");
            response.setContentLength(in.available());
            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[1024];
            for (int len = 0; (len = in.read(buf)) != -1; ) {
                out.write(buf, 0, len);
            }
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
