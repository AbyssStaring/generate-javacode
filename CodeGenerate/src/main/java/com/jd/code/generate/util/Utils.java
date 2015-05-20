package com.jd.code.generate.util;

import com.jd.code.generate.domain.FileConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * User: yangkuan@jd.com
 * Date: 15-3-16
 * Time: 下午3:48
 */
public class Utils {
    //读取properties的全部信息
    public static void readProperties(String filePath,Map<String,String> map)   {
        try {

            Properties props = new Properties();
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
           // InputStream in  =  Object.class.getResourceAsStream(filePath);
            InputStream in = loader.getResourceAsStream(filePath);
            props.load(in);
            Enumeration en = props.propertyNames();
            while (en.hasMoreElements()) {
                String key = (String) en.nextElement();
                String Property = props.getProperty (key);
                // System.out.println(key+Property);
                map.put(key, (String)props.get(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
           // throw new Exception("读取配置文件失败");
        }
    }

    public static  void readProperties(File file,FileConfig fileConfig) throws Exception {
        InputStream in  = new FileInputStream(file);
        Properties props = new Properties();
        props.load(in);
        fileConfig.setPackagePath(props.getProperty("packagePath"));
        fileConfig.setFileExtendName(props.getProperty("fileExtendName"));
        fileConfig.setFileType(props.getProperty("fileType"));
        fileConfig.setVelocityName(props.getProperty("velocityName"));
        fileConfig.setSrcTargetChildPath(props.getProperty("srcTargetChildPath"));
    }
}
