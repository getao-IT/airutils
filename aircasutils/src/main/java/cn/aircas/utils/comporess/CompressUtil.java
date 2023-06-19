package cn.aircas.utils.comporess;

import cn.aircas.utils.file.FileUtils;
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 解压缩工具类
 */
@Slf4j
public class CompressUtil {
    private final static byte[] RAR_HEADER = {82,97,114,33}; //RAR格式的二进制头

    private final static byte[] ZIP_HEADER = {80,75,3,4}; //

    private final static byte[] ZIP_OLD_HEADER = {80,75,3,4};

    private final static byte[] TAR_HEADER = {117,115,116,97,114};

    private enum CompressFileType{
        ZIP,RAR,TAR
    }

    /**
     * 判断文件的压缩格式
     * @param filePath
     * @return
     */
    private static CompressFileType getCompressFileType(String filePath){
        byte[] fileHeader = new byte[4];
        byte[] tarFileHeader = new byte[5];
        RandomAccessFile randomAccessFile = null;
        try{
            randomAccessFile = new RandomAccessFile(filePath,"r");
            randomAccessFile.read(fileHeader);
            randomAccessFile.seek(257);
            randomAccessFile.read(tarFileHeader);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                assert randomAccessFile != null;
                randomAccessFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        if (Arrays.equals(fileHeader,RAR_HEADER))
            return CompressFileType.RAR;

        if (Arrays.equals(fileHeader,ZIP_HEADER) || Arrays.equals(fileHeader, ZIP_OLD_HEADER))
            return CompressFileType.ZIP;

        if (Arrays.equals(tarFileHeader,TAR_HEADER))
            return CompressFileType.TAR;

        return null;
    }


    /**
     * 解压缩zip文件夹
     * @param srcFile
     * @param destFile
     * @return
     * @throws Exception
     */
    /**
     * 解压缩zip文件
     * @param srcFile
     * @param destFile
     */
    private static void unZipFile(File srcFile, File destFile) throws IOException {
        log.info("开始解压缩文件：{}",srcFile.getAbsolutePath());
        net.lingala.zip4j.ZipFile zipFile = new net.lingala.zip4j.ZipFile(srcFile);
        ProgressMonitor processMonitor = zipFile.getProgressMonitor();
        zipFile.setRunInThread(true);
        zipFile.setCharset(Charset.forName("GBK"));


        String fileName = null;
        try {
            zipFile.extractAll(destFile.getAbsolutePath());
            while(processMonitor.getState().equals(ProgressMonitor.State.BUSY)){
                String currentFileName = processMonitor.getFileName();
                if (currentFileName!=null && !currentFileName.equals(fileName)){
                    fileName = currentFileName;
                    log.info("正在解压缩文件：{}",currentFileName);
                }
                Thread.sleep(100);
            }
        }catch (ZipException | InterruptedException e) {
            log.error("解压缩文件：{} 错误",fileName);
        }
        log.info("解压缩文件：{}完成",srcFile.getAbsolutePath());
    }

/*
    private static void unZipFile(File srcFile, File destFile) throws Exception {
        log.info("开始解压缩文件：{}",srcFile.getAbsolutePath());
        System.setProperty("sun.zip.encoding",System.getProperty("sun.jnu.encoding"));
        ZipFile srcZipFile = new ZipFile(srcFile.getAbsolutePath(), Charset.forName("GBK"));
        Enumeration<?> enumeration = srcZipFile.entries();
        String middlePath = null;

        while(enumeration.hasMoreElements()){
            ZipEntry zipEntry = (ZipEntry) enumeration.nextElement();
            String zipEntryName = FilenameUtils.normalize(zipEntry.getName());
            try {
                middlePath = zipEntryName.substring(0,zipEntryName.lastIndexOf(File.separator));
            }
            catch (Exception e){
                middlePath="";
            }
            try {
                zipEntryName = zipEntryName.substring(zipEntryName.lastIndexOf
                        (File.separator)+1);
            }catch (Exception e){
                zipEntryName = "";
            }

            File entryFile = FileUtils.getFile(destFile.getAbsolutePath()+File.separator
                    + middlePath,zipEntryName);

            if (zipEntry.isDirectory()){
                entryFile.mkdirs();
            }else {
                long begin = System.currentTimeMillis();
                if (!entryFile.getParentFile().exists())
                    entryFile.mkdirs();
                int length=0;
                InputStream inputStream = srcZipFile.getInputStream(zipEntry);
                BufferedInputStream bis = new BufferedInputStream(inputStream);

                FileOutputStream fileOutputStream = new FileOutputStream(entryFile);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

                byte[] buffer = new byte[4096];
                while( (length = bis.read(buffer))!=-1){
                    bufferedOutputStream.write(buffer,0,length);
                    bufferedOutputStream.flush();
                }
                log.info("解压缩文件{}：耗时:{}",zipEntry.getName(),System.currentTimeMillis() - begin);
                bufferedOutputStream.close();
                fileOutputStream.close();

            }
        }
        srcZipFile.close();
        log.info("解压缩文件：{}完成",srcFile.getAbsolutePath());
    }
*/

    /**
     * 解压缩rar文件
     * @param srcFile
     * @param destFile
     * @return
     * @throws Exception
     */
    private static void unRarFile(File srcFile, File destFile) throws Exception {
        log.info("开始解压缩文件：{}",srcFile.getAbsolutePath());
        Archive archive = new Archive(new FileInputStream(srcFile));
        FileHeader fileHeader = archive.nextFileHeader();
        while(fileHeader!=null){
            String fileHeaderName = fileHeader.getFileNameW();
            String middlePath = null;
            if (!existZH(fileHeaderName)) {
                fileHeaderName = FilenameUtils.normalize(fileHeader.getFileNameString());
                try{
                    middlePath = fileHeaderName.substring(0,fileHeaderName.lastIndexOf(File.separator));
                }catch (Exception e){
                    middlePath="";
                }
                try {
                    fileHeaderName = fileHeaderName.substring(fileHeaderName.lastIndexOf
                            (File.separator)+1);
                }catch (Exception e){
                    fileHeaderName = "";
                }

            }
            File fileHeaderFile = FileUtils.getFile(destFile.getAbsolutePath()+File.separator
                    + middlePath,fileHeaderName);
            if (fileHeader.isDirectory()){
                fileHeaderFile.mkdirs();
            }else {
                if (!fileHeaderFile.exists()){
                    fileHeaderFile.getParentFile().mkdirs();
                    fileHeaderFile.createNewFile();
                }

                FileOutputStream fileOutputStream = new FileOutputStream(fileHeaderFile);
                archive.extractFile(fileHeader,fileOutputStream);
                fileOutputStream.close();
            }
            fileHeader = archive.nextFileHeader();
        }
        archive.close();
        log.info("解压缩文件：{}完成",srcFile.getAbsolutePath());
    }

    private static void unTarFile(File srcFile, File destFile) throws Exception {
        log.info("开始解压缩文件：{}",srcFile.getAbsolutePath());
        System.setProperty("sun.tar.encoding",System.getProperty("sun.jnu.encoding"));
        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(srcFile));
        TarArchiveEntry tarArchiveEntry = tarArchiveInputStream.getNextTarEntry();
        while(tarArchiveEntry!=null){

            String fileHeaderName = FilenameUtils.normalize(tarArchiveEntry.getName());
            String middlePath = null;
            try{
                middlePath = fileHeaderName.substring(0,fileHeaderName.lastIndexOf(File.separator));
            }catch (Exception e){
                middlePath="";
            }
            try {
                fileHeaderName = fileHeaderName.substring(fileHeaderName.lastIndexOf
                        (File.separator)+1);
            }catch (Exception e){
                fileHeaderName = "";
            }
            File archiveEntryFile = FileUtils.getFile(destFile.getAbsolutePath()+File.separator
                    + middlePath,fileHeaderName);
            if (tarArchiveEntry.isDirectory())
                archiveEntryFile.mkdirs();
            else {
                if (!archiveEntryFile.getParentFile().exists())
                    archiveEntryFile.mkdirs();

                byte[] buffer = new byte[1024];
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(archiveEntryFile));
                while(tarArchiveInputStream.read(buffer)!=-1){
                    bufferedOutputStream.write(buffer);
                    bufferedOutputStream.flush();
                }
                bufferedOutputStream.close();
            }
            tarArchiveEntry = tarArchiveInputStream.getNextTarEntry();
        }

        tarArchiveInputStream.close();
        log.info("解压缩文件：{}完成",srcFile.getAbsolutePath());
    }


    /*
    * 解压缩文件
    * */
    public static void decompress(String srcPath, String destPath, boolean delete) throws Exception
    {
        File srcFile = new File(srcPath);
        File destFile = new File(destPath);
        if (!srcFile.exists() || srcFile.length() ==0)
            throw new Exception("文件：" + srcFile.getName() + " 不存在");
        if (!destFile.exists())
            destFile.mkdirs();

        CompressFileType fileType = getCompressFileType(srcPath);
        if (fileType == null)
            throw new Exception("文件：" + srcFile.getName() + " 不是压缩格式");

        switch (fileType){
            case RAR:unRarFile(srcFile,destFile); break;
            case TAR:unTarFile(srcFile,destFile); break;
            case ZIP:unZipFile(srcFile,destFile); break;
        }

        if(delete)
            srcFile.delete();
    }



    /**
     * 压缩文件，指定输出流
     * @param srcDir 源文件(夹)路径
     * @param outputStream 输出流
     * @throws IOException
     */
    public static void toZip(String srcDir, OutputStream outputStream) throws IOException {
        File srcDirFile = new File(srcDir);
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        compress(srcDirFile,zipOutputStream);
        zipOutputStream.close();
    }

//    /**
//     * 递归压缩文件
//     * @param srcFile 源文件
//     * @param zipOutputStream zip输出流
//     * @param inZipName 文件对应的压缩包里的名称
//     * @throws IOException
//     */
    public static void compress(File srcFile, ZipOutputStream zipOutputStream) throws IOException {
        File[] childFiles = srcFile.listFiles();
        for (File childFile : childFiles) {
            compressToZip(childFile,zipOutputStream,childFile.getName());
        }
    }

    public static void compressToZip(File srcFile, ZipOutputStream zipOutputStream, String inZipName) throws IOException {
        byte[] buffer = new byte[1024];
        if (srcFile.isFile()){
            int length = 0;
            zipOutputStream.putNextEntry(new ZipEntry(inZipName));
            FileInputStream fileInputStream = new FileInputStream(srcFile);

            while((length = fileInputStream.read(buffer))!=-1){
                zipOutputStream.write(buffer,0,length);
            }
            fileInputStream.close();
            zipOutputStream.closeEntry();
        }else {
            zipOutputStream.putNextEntry(new ZipEntry(inZipName + "/"));
            zipOutputStream.closeEntry();

            File[] childFiles = srcFile.listFiles();
            for (File childFile : childFiles) {
                compressToZip(childFile,zipOutputStream,inZipName + "/" + childFile.getName());
            }
        }
    }
    /**
     * 判断文件名是否存在中文字符
     */
    public static boolean existZH(String str){
        String regEx = "[\\u4e00-\\u9fa5]";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()){
            return true;
        }
        return false;
    }

}