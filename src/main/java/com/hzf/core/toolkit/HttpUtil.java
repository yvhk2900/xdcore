package com.hzf.core.toolkit;

import com.hzf.core.common.GlobalValues;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.RequestBuilder;
import net.dongliu.requests.Requests;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Http请求工具类, 因requests已经足够好用,这里仅包装一层,并提供实例用.
 */
public class HttpUtil extends Requests{

    public static List<String> uploadRequest(HttpServletRequest request) throws Exception {
        List<String> files = new ArrayList<>();
        if (request instanceof StandardMultipartHttpServletRequest) {
            StandardMultipartHttpServletRequest fileRequest = (StandardMultipartHttpServletRequest) request;
            Map<String, MultipartFile> m = fileRequest.getFileMap();
            for (String k : m.keySet()) {
                MultipartFile mf = m.get(k);
                String name = GlobalValues.SaveUploadFile(mf);
                files.add(name);
            }
        }
        return files;
    }

    public static void exportImage(HttpServletResponse response, String filePath) throws Exception {
        File file = new File(GlobalValues.GetFilePath(filePath));
        String name = file.getName();
        response.setHeader("Content-Disposition",
                "attachment; filename=" + new String((name).getBytes("UTF-8"), "ISO_8859_1"));
        String contenType = "img/jpeg";
        if (file.exists() && StrUtil.isNotEmpty(filePath)) {
            if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
                contenType = "img/jpeg";
            } else if (filePath.endsWith(".png")) {
                contenType = "img/png";
            }
            HttpUtil.exportFile(response, file.getPath(), contenType);
        }
    }
    public static void exportFile(HttpServletResponse response, String filePath) throws Exception {
        response.setHeader("Content-Disposition",
                "attachment; filename=" + new String((new File(filePath).getName()).getBytes("UTF-8"), "ISO_8859_1"));
        exportFile(response, filePath, "application/x-msdownload");
    }

    public static void exportFile(HttpServletResponse response, String filePath, String contentType) throws Exception {
        OutputStream out = response.getOutputStream();
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
            response.setContentType(contentType);
            response.setContentLength((int) file.length());
            byte[] buff = new byte[1024];
            if (file.exists()) {
                FileInputStream fs = new FileInputStream(file);
                while (fs.read(buff) >= 0) {
                    out.write(buff);
                }
                fs.close();
            }
            out.flush();
            out.close();
        }catch (Exception ex){

        }finally {

        }
    }

    public static void exportFileByContent(HttpServletResponse response, String content, String fileName)
            throws IOException {
        response.setContentType("application/x-msdownload");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + new String(fileName.getBytes("UTF-8"), "ISO_8859_1"));
        OutputStream out = response.getOutputStream();
        out.write(content.getBytes("utf-8"));
        out.flush();
        out.close();
    }
    public static void downloadFile(String url, String path) {
        RequestBuilder get = Requests.get(url);
        RawResponse response = get.send();
        InputStream in = response.getInput();
        FileOutputStream out = null;
        File file = new File(path);
        try {
            file.getParentFile().mkdirs();
            if (file.exists()) {
                file.delete();
            }
            out = new FileOutputStream(file);
            int buf_size = 1024;
            byte[] buffer = new byte[buf_size];
            int len = 0;
            while (-1 != (len = in.read(buffer, 0, buf_size))) {
                out.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            response.close();
        }
    }

}
