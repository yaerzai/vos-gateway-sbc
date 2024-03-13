package com.ytl.vos.gateway.sbc.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HTTP工具类
 */
public class HTTPClient {
    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url  发送请求的 URL
     * @param json 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String postJsonStream(String url, String json) {
        String result = "";
        OutputStream out = null;
        InputStream in = null;
        try {
            // 创建url资源
            URL urL = new URL(url);
            // 建立http连接
            HttpURLConnection conn = (HttpURLConnection) urL.openConnection();
            // 设置允许输出
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 设置不用缓存
            conn.setUseCaches(false);
            // 设置传递方式
            conn.setRequestMethod("POST");
            // 设置维持长连接
            conn.setRequestProperty("Connection", "Keep-Alive");
            // 设置文件字符集:
            conn.setRequestProperty("Charset", "UTF-8");
            //转换为字节数组
            byte[] data = json.getBytes();
            // 设置文件长度
            String dataLength = String.valueOf(data.length);
            conn.setRequestProperty("Content-Length", dataLength);
            // 设置文件类型:
            conn.addRequestProperty("Content-type", "application/json");
            // 开始连接请求
            conn.connect();
            out = conn.getOutputStream();
            // 写入请求的字符串
            out.write(json.getBytes());
            out.flush();
            out.close();
            System.out.println(conn.getResponseCode());
            // 请求返回的状态
            if (conn.getResponseCode() == 200) {
                // 请求返回的数据
                in = conn.getInputStream();
                try {
                    byte[] data1 = new byte[in.available()];
                    in.read(data1);
                    // 转成字符串
                    result = new String(data1);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } else {
                System.out.println("no++");
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url    发送请求的 URL
     * @param params 请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     */
    public static String postParam(String url, String params) {
        String result = "";
        OutputStream out = null;
        InputStream in = null;
        try {
            // 创建url资源
            URL urL = new URL(url);
            // 建立http连接
            HttpURLConnection conn = (HttpURLConnection) urL.openConnection();
            // 设置允许输出
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 设置不用缓存
            conn.setUseCaches(false);
            // 设置传递方式
            conn.setRequestMethod("POST");
            // 设置维持长连接
            conn.setRequestProperty("Connection", "Keep-Alive");
            // 设置文件字符集:
            conn.setRequestProperty("Charset", "UTF-8");
            //转换为字节数组
            byte[] data = params.getBytes();
            // 设置文件长度
            String dataLength = String.valueOf(data.length);
            conn.setRequestProperty("Content-Length", dataLength);
            // 设置文件类型:
            conn.addRequestProperty("Content-type", "text/plain");
            // 开始连接请求
            conn.connect();
            out = conn.getOutputStream();
            // 写入请求的字符串
            out.write(params.getBytes());
            out.flush();
            out.close();
            // 请求返回的状态
            if (conn.getResponseCode() == 200) {
                // 请求返回的数据
                in = conn.getInputStream();
                try {
                    byte[] data1 = new byte[in.available()];
                    in.read(data1);
                    // 转成字符串
                    result = new String(data1);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } else {
                System.out.println("no++");
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }
}
