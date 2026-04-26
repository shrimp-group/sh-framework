package com.wkclz.web.helper;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.base.R;
import com.wkclz.core.exception.SystemException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ResponseHelper {

    private static final Logger logger = LoggerFactory.getLogger(ResponseHelper.class);

    public static boolean responseError(HttpServletResponse rep, R r) {
        try (PrintWriter writer = rep.getWriter()) {
            r.setRequestTime(null);
            r.setResponseTime(null);
            r.setCostTime(null);
            String string = JSON.toJSONString(r);
            rep.setHeader("Content-Type", "application/json;charset=UTF-8");
            writer.print(string);
            return true;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }


    public static void responseExcel(HttpServletResponse response, String file) {
        responseExcel(response, new File(file));
    }
    public static void responseExcel(HttpServletResponse response, File file) {
        if (response == null || file == null) {
            throw SystemException.of("response and file can not be null!");
        }

        String fileName = file.getName();

        // 使用 RFC 5987 编码文件名，支持中文
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        logger.info("the excel file is in {}", file.getPath());

        response.setContentType("application/x-excel");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        response.setHeader("Content-Length", String.valueOf(file.length()));

        try (
            InputStream in = Files.newInputStream(file.toPath());
            OutputStream fops = response.getOutputStream();
        ) {
            byte[] bytes = new byte[8192];
            int len;
            while ((len = in.read(bytes)) != -1) {
                fops.write(bytes, 0, len);
            }
            fops.flush();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
