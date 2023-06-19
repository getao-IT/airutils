package cn.aircas.utils.image;

import cn.aircas.utils.image.emun.CoordinateSystemType;
import cn.aircas.utils.image.geo.GeoUtils;
import lombok.extern.slf4j.Slf4j;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import java.text.DecimalFormat;

@Slf4j
public class ParseImageInfo {

    static {
        gdal.AllRegister();
        gdal.SetConfigOption("GDAL_PAM_ENABLED","FALSE");

    }
    /**
     * 读取影像的地理信息
     * @param imagePath
     * @return
     */
    public static ImageInfo parseInfo(String imagePath){
        log.info("开始解析文件：{} 的地理信息",imagePath);

        long begin = System.currentTimeMillis();
        ImageInfo imageInfo = new ImageInfo();
        Dataset dataset = gdal.Open(imagePath,gdalconst.GA_ReadOnly);
        if (dataset==null){
            log.error("没有找到需要parseInfo的影像路径");
            return imageInfo;
        }

        double[] lonLatCoordinateRange = new double[]{0,0,0,0};
        double[] projectionCoordinateRange = new double[]{0,0,0,0};
        CoordinateSystemType coordinateSystemType = CoordinateSystemType.GEOGCS;
        String dateType = gdal.GetDataTypeName(dataset.GetRasterBand(1).getDataType());
        String imageSizeSpec = calculateImageSizeSpec(dataset.getRasterXSize(),dataset.getRasterYSize());
        double test = dataset.GetGeoTransform()[1] * 111194.872221777;
        String resolutioStr = String.format("%.2f",test);
        double resolution = Double.parseDouble(resolutioStr);

        //如果是像素坐标
        if (!GeoUtils.hasGeoInfo(dataset)){
            resolution = 0;
            coordinateSystemType = CoordinateSystemType.PIXELCS;
        }else {
            //如果是投影坐标
            if (dataset.GetGeoTransform()[0]> 180){
                resolution = 1;
                coordinateSystemType = CoordinateSystemType.PROJCS;
            }
            lonLatCoordinateRange = GeoUtils.getCoordinateRange(dataset,GeoUtils.COORDINATE_LONLAT);
            projectionCoordinateRange = GeoUtils.getCoordinateRange(dataset,GeoUtils.COORDINATE_PROJECTION);
        }
        int lonRangeIndex = calculateLonRangeIndex(lonLatCoordinateRange[1],lonLatCoordinateRange[3]);

        if (dataset.getRasterCount()>=1){
            imageInfo.setBit(gdal.GetDataTypeName(dataset.GetRasterBand(1).getDataType()));
        }


        imageInfo.setBit(dateType);
        imageInfo.setResolution(resolution);
        imageInfo.setLonRangeIndex(lonRangeIndex);
        imageInfo.setImageSizeSpec(imageSizeSpec);
        imageInfo.setRange(lonLatCoordinateRange);
        imageInfo.setBands(dataset.getRasterCount());
        imageInfo.setWidth(dataset.getRasterXSize());
        imageInfo.setMinLon(lonLatCoordinateRange[0]);
        imageInfo.setMinLat(lonLatCoordinateRange[1]);
        imageInfo.setMaxLon(lonLatCoordinateRange[2]);
        imageInfo.setMaxLat(lonLatCoordinateRange[3]);
        imageInfo.setHeight(dataset.getRasterYSize());
        imageInfo.setProjection(dataset.GetProjectionRef());
        imageInfo.setProjectionRange(projectionCoordinateRange);
        imageInfo.setCoordinateSystemType(coordinateSystemType);
        log.info("文件：{} 的地理信息解析完成",imagePath);

        long end = System.currentTimeMillis();
        log.info("地理信息解析总耗时：{}",end-begin);

        return imageInfo;
    }


    /**
     * 计算中心经度所处区域
     * @param minLon
     * @param maxLon
     * @return
     */
    private static int calculateLonRangeIndex(double minLon, double maxLon){
        int rangeIndex = 0;
        double[] lonRange = new double[]{60,120,180,240,300,360};
        double centerLon = 180 + (maxLon + minLon) / 2;
        for (int index = 0; index <= 5; index++) {
            if (lonRange[index] > centerLon){
                rangeIndex = index;
                break;
            }
        }

        return rangeIndex;
    }

    /**
     * 计算
     * @return
     */
    private static String calculateImageSizeSpec(int width, int height){
        int length = Math.min(width,height);
        if (length < 1000)
            return "TINY";
        else {
            if (length < 2000)
                return "SMALL";
            else {
                if (length < 5000)
                    return "MEDIUM";
                else
                    return "LARGE";
            }
        }
    }

}
