package cn.aircas.utils.file;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.Dom4JDriver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.dom4j.*;
import org.json.XML;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 文件md5值
 */
@Slf4j
public class FileUtils {

    private static final double KB = 1024D;
    private static final double MB = 1024 * 1024D;
    private static final double GB = 1024 * 1024 * 1024D;



    /**
     * 封装paths方法，使任意类型均可转换为string
     * @param first 第一个值
     * @param more 其它值
     * @return
     */
    public static Path getPath(Object first, Object... more){
        String[] strMore = Arrays.stream(more).map(String::valueOf).toArray(String[]::new);
        return Paths.get(String.valueOf(first),strMore);
    }

    /**
     * 封装paths方法，使任意类型均可转换为string
     * @param first 第一个值
     * @param more 其它值
     * @return
     */
    public static String getStringPath(Object first, Object... more){
        String[] strMore = Arrays.stream(more).map(String::valueOf).toArray(String[]::new);
        return Paths.get(String.valueOf(first),strMore).toString();
    }

    /**
     * 封装paths方法，使任意类型均可转换为string
     * @param first 第一个值
     * @param more 其它值
     * @return
     */
    public static File getFile(Object first, Object... more){
        String[] strMore = Arrays.stream(more).map(String::valueOf).toArray(String[]::new);
        return Paths.get(String.valueOf(first),strMore).toFile();
    }

    /**
     * 读取文件
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String readFile(String filePath){
        StringBuilder content = new StringBuilder();
        try(RandomAccessFile randomAccessFile = new RandomAccessFile(filePath,"r")){
            String line;
            while((line = randomAccessFile.readLine())!=null)
                content.append(new String(line.getBytes("ISO-8859-1"),"utf-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }

    /**
     * 拷贝文件
     * @param srcFile
     * @param destFile
     * @throws IOException
     */
    public static void copyFile(File srcFile, File destFile) throws IOException {
        org.apache.commons.io.FileUtils.copyFile(srcFile,destFile);
    }

    /**
     * 移动文件到文件夹
     * @param srcFile
     * @param destFile
     * @throws IOException
     */
    public static void moveFileToDirectory(File srcFile, File destFile, boolean replace) throws IOException {
        File destSubFile = getFile(destFile.getAbsoluteFile(),srcFile.getName());
        if (destSubFile.exists()){
            if (replace){
                org.apache.commons.io.FileUtils.forceDelete(destSubFile);
            }else {
                log.error("文件夹：{}下已经存在文件：{}",destFile.getAbsolutePath(),srcFile.getName());
                return;
            }
        }
        org.apache.commons.io.FileUtils.moveFileToDirectory(srcFile,destFile,true);
    }

    /**
     * 拷贝文件到某一文件夹
     * @param srcFile
     * @param destDir
     * @throws IOException
     */
    public static void copyFileToDirectory(File srcFile, File destDir) throws IOException {
        org.apache.commons.io.FileUtils.copyFileToDirectory(srcFile,destDir);
    }

    /**
     * 将java对象保存为xml文件
     * @param clazz 对象类型
     * @param data 对象
     * @param destFile 输出文件
     * @throws IOException
     */
    public static void objectToXML(Class clazz, Object data, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        FileOutputStream fileOutputStream = new FileOutputStream(destFile);
        XStream xStream = new XStream(new Dom4JDriver());
        xStream.processAnnotations(clazz);
        xStream.toXML(data,fileOutputStream);
        fileOutputStream.close();
    }

    /**
     * xml转json
     * @param filePath
     * @return
     */
    public static JSONObject XMLToJSON(String filePath){
        String content = FileUtils.readFile(filePath);
        org.json.JSONObject xmlJSONObj = XML.toJSONObject(content);
        return JSON.parseObject(xmlJSONObj.toString());
    }

    /**
     * 移动文件夹
     */
    public static void moveDirectoryToDirectory(File srcFile, File destFile,boolean merge) throws IOException {
        File destSubFile = getFile(destFile.getAbsoluteFile(),srcFile.getName());
        if (destSubFile.exists() && !merge){
            log.error("{}路径下已经存在名为:{}的文件夹",destFile.getAbsolutePath(),srcFile.getName());
            return;
        }
        if (!destSubFile.exists()) {
            destSubFile.mkdirs();
        }
        for (File file : srcFile.listFiles()) {
            if (!file.isFile()) {
                moveDirectoryToDirectory(file,destSubFile,true);
            } else {
                moveFileToDirectory(file,destSubFile,merge);
            }
        }
        org.apache.commons.io.FileUtils.forceDelete(srcFile);
    }

    public static void writeListToFile(Set<String> list, File destFile) throws IOException {
        if (!destFile.exists())
            destFile.createNewFile();
        if (list.size()==0)
            return;

        FileWriter fileWriter = new FileWriter(destFile,true);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        for (String value : list) {
            bufferedWriter.write(String.valueOf(value));
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();
        fileWriter.close();
    }



    /**
     * 删除文件夹
     * @param directory
     * @throws IOException
     */
    public static void deleteDirectory(File directory) throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(directory);
    }

    /**
     * 强制删除文件
     * @param fileName
     */
    public static void forceDelete(String fileName){
        try {
            org.apache.commons.io.FileUtils.forceDelete(new File(fileName));
        } catch (IOException e) {
            log.error("forceDelete删除文件：{} 出错",fileName);
            e.printStackTrace();
        }
    }


    /**
     * 读取缩略图到字节数组
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public static byte[] getImageByteArray(String filePath) throws IOException {
        File thumbnailFile = new File(filePath);
        String extionsion = FilenameUtils.getExtension(filePath);
        if (!thumbnailFile.exists())
            return null;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedImage bufferedImage = ImageIO.read(thumbnailFile);
        if ("png".equalsIgnoreCase(extionsion))
            ImageIO.write(bufferedImage,"PNG",byteArrayOutputStream);
        if ("jpg".equalsIgnoreCase(extionsion) || "jpeg".equalsIgnoreCase(extionsion))
            ImageIO.write(bufferedImage,"jpg",byteArrayOutputStream);
        byte[] data =  byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        return data;
    }

    /**
     * 计算文件夹的大小，并返回string值
     * @param filePath
     * @return
     */
    public static String getDirectorySize(String filePath){
        File file = new File(filePath);
        long size = org.apache.commons.io.FileUtils.sizeOfDirectory(file);
        if (size == 0)
            return "0";
        return fileSizeToString(Double.valueOf(size));
    }

    public static String fileSizeToString(double size){
        if (size < KB){
            size = Double.valueOf(String.format("%.2f",size));
            return size + "B";
        }
        if (size >= KB && size < MB ){
            size = Double.valueOf(String.format("%.2f",size/KB));
            return size + "KB";

        }


        if (size >=MB && size < GB){
            size = Double.valueOf(String.format("%.2f",size/MB));
            return size + "MB";
        }

        size = Double.valueOf(String.format("%.2f",size/GB));
        return size + "GB";
    }


    public static boolean isEmpty(String str){
        if (str == null ||str.trim().isEmpty() || "null".equals(str)){
            return true;
        }
        return false;
    }

    /**
     * String转File
     */
    public static void stringToFile(String filePath , String content) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        byte [] bytes = content.getBytes();
        fileOutputStream.write(bytes);

    }


    /**
     * 获取文件夹下指定格式的所有文件
     * @param dirPath
     * @param allFilePaths
     * @return
     */
    public static List<String> folderFiles(String dirPath,List<String> allFilePaths, String[] extensions){
        File dirFile = new File(dirPath);
        File[] files = dirFile.listFiles();
        if (files ==null){
            return allFilePaths;
        }
        for (int i=0;i< files.length;i++){
            File file = files[i];
            if (file.isDirectory()){
                folderFiles(file.getAbsolutePath(),allFilePaths,extensions);
            }else {
                if (FilenameUtils.isExtension(file.getName(),extensions))
                    allFilePaths.add(file.getAbsolutePath());
            }
        }
        return allFilePaths;
    }


    /**
     * 获取文件夹下的所有文件
     * @param dirPath
     * @param AllFilePaths
     * @return
     */
    public static List<String> folderFiles(String dirPath,List<String> AllFilePaths){
        File dirFile = new File(dirPath);
        File[] files = dirFile.listFiles();
        if (files ==null){
            return AllFilePaths;
        }
        for (int i=0;i< files.length;i++){
            File file = files[i];
            if (file.isDirectory()){
                folderFiles(file.getAbsolutePath(),AllFilePaths);
            }else {
                AllFilePaths.add(file.getPath());
            }
        }
        return AllFilePaths;
    }



}
