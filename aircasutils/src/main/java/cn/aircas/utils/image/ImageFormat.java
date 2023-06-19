package cn.aircas.utils.image;

import cn.aircas.utils.file.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

@Slf4j
public class ImageFormat {

    /**
     * 使用gdal进行图像格式转换
     * @param inputPath
     * @param outputPath
     * @param format
     * @return 存储path
     */
    public static String formatConvertor(String inputPath, String outputPath, String format){
        gdal.AllRegister();

        gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
        Dataset ds = gdal.Open(inputPath, gdalconstConstants.GA_ReadOnly);
        if(ds == null){
            log.error("GDALOpen failed-"+gdal.VSIGetLastErrorNo());
            log.error(gdal.GetLastErrorMsg());
            return null;
        }

        Driver hDriver = gdal.GetDriverByName(format);
        log.info("Driver:"+hDriver.getShortName()+"/"+hDriver.getLongName());
        String[] split = inputPath.split("/");
        String baseName = split[split.length-1].split("\\.")[0];
        String extension;
        if(format.equals("JPEG")){
            extension = "jpg";
        }else if(format.equals("GTiff")){
            extension = "tif";
        }else {
            extension = format.toLowerCase();
        }
        String path = FileUtils.getStringPath(outputPath,baseName)+"."+extension;
        hDriver.CreateCopy(path,ds);

        ds.delete();
        hDriver.delete();
        log.info("格式转换成功");
        return path;
    }

}
