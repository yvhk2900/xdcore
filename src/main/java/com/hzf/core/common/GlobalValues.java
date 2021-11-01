package com.hzf.core.common;

import com.hzf.core.base.BaseApplication;
import com.hzf.core.base.BaseModel;
import com.hzf.core.base.BaseUser;
import com.hzf.core.toolkit.PathUtil;
import com.hzf.core.toolkit.RequestUtil;
import com.hzf.core.toolkit.StrUtil;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalValues {


    public static String CurrentIP = "127.0.0.1";
    public static int CurrentPort = -1;//http监听
//    public static int CurrentHttpsPort = 9091;//https监听
    public static TaskScheduler taskScheduler;
    public static BaseApplication baseAppliction;
    public static boolean isDebug = false;
    static String uploadFilepath = "";

    public static void checkDebug() {
        List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String str : arguments) {
            if (str.startsWith("-agentlib")) {
                isDebug = true;
                break;
            }
        }
    }

    public static String GetUploadFilePath() {
        if (StrUtil.isEmpty(uploadFilepath)) {
            String path = baseAppliction.GetUploadFilePath();
            File f = new File(path);
            if (f.isAbsolute()) {
                uploadFilepath = path;
            } else {
                uploadFilepath = GetRootPath() + "/" + path;
            }
            File ff = new File(uploadFilepath);
            if (!ff.exists()) {
                ff.mkdirs();
            }
        }
        return uploadFilepath;
    }

    static String tempFilepath = "";

    public static String GetTempFilePath() {
        if (StrUtil.isEmpty(tempFilepath)) {
            String path = baseAppliction.GetTempFilePath();
            File f = new File(path);
            if (f.isAbsolute()) {
                tempFilepath = path;
            } else {
                tempFilepath = GetRootPath() + "\\" + path;
            }
            File ff = new File(tempFilepath);
            if (!ff.exists()) {
                ff.mkdirs();
            }
        }
        return tempFilepath;
    }

    public static String SaveUploadFile(MultipartFile mf) throws Exception {
        String originFileName = mf.getOriginalFilename().replaceAll("_", "");
        String[] filepathname = NewHasDateFilePathAndName(originFileName);

        File f = new File(filepathname[0]);
        File fo = new File(f.getParent());
        fo.mkdirs();
        mf.transferTo(f);
        return filepathname[1];
    }

    public static String[] NewHasDateFilePathAndName(String originFileName) {
//        File orf = new File(originFileName);
//        if (orf.isAbsolute()) {
//            originFileName = orf.getName();
//        }
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        String savePath = df.format(new Date());
        String suffix = "";
        if (originFileName.indexOf(".") > 0) {
            suffix = originFileName.substring(originFileName.lastIndexOf(".")).toLowerCase();
        }
//        String uniquename = originFileName;
        String uniquename = BaseModel.GetUniqueId() + suffix;
        String physicalPath = GetUploadFilePath() + "/" + savePath + "/" + uniquename;
        File ff = new File(physicalPath);
        if (ff.exists()) {
            uniquename = BaseModel.GetUniqueId() + originFileName;
            physicalPath = GetUploadFilePath() + "/" + savePath + "/" + uniquename;
        }
        String name = savePath + "_" + uniquename;
        return new String[]{physicalPath, name};
    }

    public static String GetFilePath(String filename) {
        if (StrUtil.isNotEmpty(filename)) {
            if(filename.contains("|")){
                String[] ff = filename.split("\\|");
                filename = ff[1];
            }
            filename = filename.replace("_", "/");
        }
        return GetUploadFilePath() + "/" + filename;
    }

    public static Map<String,Object> GetSessionCache(){
        Map<String,Object>  map = (Map<String,Object>)RequestUtil.GetRequest().getAttribute("sessionCache");
        if(map==null) {
            map = new HashMap<>();
            RequestUtil.GetRequest().setAttribute("sessionCache", map);
        }
        return map;
    }

    public static BaseUser GetSessionUser() {

        if (RequestUtil.HasRequest()) {
            Object user = RequestUtil.GetRequest().getAttribute(BaseUser.F_USER);
            if (user != null) return (BaseUser) user;
        }
        return null;
    }

    public static BaseUser SetSessionUser(BaseUser persistedUser) {
        RequestUtil.GetRequest().setAttribute(BaseUser.F_USER, persistedUser);
        return persistedUser;
    }

    public static String GetJarPath() {
        String path = baseAppliction.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new File(path).getParentFile().getPath();
    }

    //获取系统根目录
    public static String GetRootPath() {
        String path = GetJarPath();
        if (!new File(path).exists()) {
            String key = ".jar";
            int index = path.lastIndexOf(key);
            if (index >= 0) {
                path = new File(path.substring(0, index + key.length())).getParentFile().getPath();
            }
            if (!new File(path).exists()) {
                key = "file:";
                index = path.lastIndexOf(key);
                if (index >= 0) {
                    path = path.substring(index + key.length());
                }
            }
        }
        if(path.endsWith("\\target\\classes")){
            path=path.replace("\\target\\classes","");
        }
        if(path.endsWith("\\target")){
            path=path.replace("\\target","");
        }
        System.out.println("***********GetRootPath****************" + path);
        return path;
    }



    private static String ApplicationPath = null;

    public static String GetFilePath() {
        if (StrUtil.isEmpty(ApplicationPath)) {
            URL url = baseAppliction.getClass().getProtectionDomain().getCodeSource().getLocation();
            File file = new File(url.getPath());
            String path = file.getAbsolutePath();
            try {
                path = java.net.URLDecoder.decode(path, "utf-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (path.contains("/file:")) {
                path = path.substring(0, path.indexOf("/file:"));
            }
            if (path.contains("\\file:")) {
                path = path.substring(0, path.indexOf("\\file:"));
            }
            ApplicationPath = path;
        }
        return ApplicationPath;
    }

    //获取模板文件地址
    public static String GetTemplateFile(String fileName) {
        String path = GetFilePath() + File.separator + "static" + File.separator + "template" + File.separator + fileName;
        if (!new File(path).exists()) {
            path = GetFilePath() + File.separator + "doc" + File.separator + fileName;
        }
        return path;
    }
}
