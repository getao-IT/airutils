package cn.aircas.utils.image.geo;

import org.apache.commons.lang3.StringUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

import java.util.Arrays;

/**
 * 常用的地理信息工具类，获取经纬度信息，经纬度，像素转换等
 * @author vanishrain
 */
public class GeoUtils {

    //经纬度
    public static String COORDINATE_LONLAT = "LONLAT";
    //投影
    public static String COORDINATE_PROJECTION = "PROJECTION";

    static {
        gdal.AllRegister();
        gdal.SetConfigOption("GDAL_PAM_ENABLED","FALSE");
    }

    /**
     * 判断是否为投影坐标
     * @return
     */
    public static boolean isProjection(Dataset dataset){
        String projection = dataset.GetProjection();
        double[] geoTransform = dataset.GetGeoTransform();
        if (projection!=null && projection.contains("PROJCS") && geoTransform[0] > 180)
            return true;
        return false;
    }

    /**
     * 判断是否为投影坐标
     * @return
     */
    public static boolean isProjection(String imagePath){
        gdal.SetConfigOption("GDAL_PAM_ENABLED","FALSE");
        Dataset dataset = gdal.Open(imagePath);
        return isProjection(dataset);
    }

    /**
     * 判断影像是否有地理信息
     * @param dataset
     * @return
     */
    public static boolean hasGeoInfo(Dataset dataset){
        boolean flag = true;
        double[] geoTransform = dataset.GetGeoTransform();
        double[] noGeoTransform = new double[]{0,1,0,0,0,1};
        if (Arrays.equals(geoTransform,noGeoTransform) || StringUtils.isBlank(dataset.GetProjection()))
            flag = false;
        return flag;
    }

    /**
     * 判断影像是否有地理信息
     * @param imageFilePath
     * @return
     */
    public static boolean hasGeoInfo(String imageFilePath){
        boolean flag = true;
        Dataset dataset = gdal.Open(imageFilePath);
        double[] geoTransform = dataset.GetGeoTransform();
        double[] noGeoTransform = new double[]{0,1,0,0,0,1};
        if (Arrays.equals(geoTransform,noGeoTransform) || StringUtils.isBlank(dataset.GetProjection()))
            flag = false;
        return flag;
    }


    /**
     * 获取影像的经纬度范围
     * @param imageFilePath 影像路径
     * @param coordinateType 坐标类型 GeoUtils.COORDINATE_LONLAT GeoUtils.COORDINATE_PROJECTION  经纬度坐标或者投影坐标
     * @return
     */
    public static double[] getCoordinateRange(String imageFilePath, String coordinateType){
        Dataset dataset = gdal.Open(imageFilePath);
        return getCoordinateRange(dataset,coordinateType);
    }

    /**
     * 获取影像的经纬度范围
     * @param dataset gdal dataset
     * @param coordinateType 坐标类型 GeoUtils.COORDINATE_LONLAT GeoUtils.COORDINATE_PROJECTION  经纬度坐标或者投影坐标
     * @return
     */
    public static double[] getCoordinateRange(Dataset dataset, String coordinateType){
        int imageWidth = dataset.getRasterXSize();
        int imageHeight = dataset.getRasterYSize();
        double[] geoTransform = dataset.GetGeoTransform();
        String originalCoordinateType = isProjection(dataset) ? GeoUtils.COORDINATE_PROJECTION : GeoUtils.COORDINATE_LONLAT;

        double rightX = geoTransform[0] + Math.abs(imageWidth * geoTransform[1]) + imageHeight * geoTransform[2];
        double rightY = geoTransform[3] + imageWidth * geoTransform[4] - Math.abs(imageHeight * geoTransform[5]);
        if(!coordinateType.equals(originalCoordinateType)){
            double[] leftUpPoint = coordinateConvertor(geoTransform[0],geoTransform[3],dataset,coordinateType);
            double[] rightBottomPoint = coordinateConvertor(rightX,rightY,dataset,coordinateType);
            return new double[]{leftUpPoint[0],rightBottomPoint[1],rightBottomPoint[0],leftUpPoint[1]};
        }

        return new double[]{geoTransform[0],rightY,rightX,geoTransform[3]};

    }

    /**
     * 像素点转坐标点
     * @param x
     * @param y
     * @param dataset
     * @param coordinateType
     * @return
     */
    public static double[] pixel2Coordinate(double x, double y, Dataset dataset, String coordinateType){
        double[] geoTransform = dataset.GetGeoTransform();

        double rightX = geoTransform[0] + Math.abs(x * geoTransform[1]) + y * geoTransform[2];
        double rightY = geoTransform[3] + x * geoTransform[4] - Math.abs(y * geoTransform[5]);
        String originalCoordinateType = isProjection(dataset) ? GeoUtils.COORDINATE_PROJECTION : GeoUtils.COORDINATE_LONLAT;

        if (originalCoordinateType.equalsIgnoreCase(coordinateType))
            return new double[]{rightX,rightY};
        return coordinateConvertor(rightX,rightY,dataset,coordinateType);
    }

    /**
     * 像素点转坐标点
     * @param x
     * @param y
     * @param imagePath
     * @param coordinateType
     * @return
     */
    public static double[] pixel2Coordinate(double x, double y, String imagePath, String coordinateType){
        Dataset dataset = gdal.Open(imagePath);
        return pixel2Coordinate(x,y,dataset,coordinateType);
    }


    /**
     * 经纬度范围转像素范围
     * @param coordinateRange 经纬度范围
     * @param srcPath 影像路径
     * @return 像素范围
     */
    public static double[] lonLatRange2PixelRange(double[] coordinateRange, String srcPath){
        Dataset dataset = gdal.Open(srcPath);
        return lonLatRange2PixelRange(coordinateRange,dataset);
    }

    /**
     * 经纬度范围转像素范围
     * @param coordinateRange 经纬度范围
     * @param dataset gdal dataset
     * @return 像素范围
     */
    public static double[] lonLatRange2PixelRange(double[] coordinateRange, Dataset dataset){
        double[] range;

        if (isProjection(dataset)){
            if(coordinateRange[0]>=-180 && coordinateRange[0]<=180){
                double[] upLeftPoint = coordinateConvertor(coordinateRange[0],coordinateRange[3],dataset, GeoUtils.COORDINATE_PROJECTION);
                double[] bottomRightPoint = coordinateConvertor(coordinateRange[2],coordinateRange[1],dataset, GeoUtils.COORDINATE_PROJECTION);
                coordinateRange = new double[]{upLeftPoint[0],bottomRightPoint[1],bottomRightPoint[0],upLeftPoint[1]};
            }
            range = convertCoordinateRangeToPixelRange(coordinateRange,dataset, GeoUtils.COORDINATE_PROJECTION);
        }else {
            range = convertCoordinateRangeToPixelRange(coordinateRange, dataset, GeoUtils.COORDINATE_LONLAT);
        }

        return range;
    }

    /**
     * 坐标转换 经纬度、投影坐标相互转换
     * @param x
     * @param y
     * @param imagePath 影像路径
     * @param coordinateType 坐标类型
     * @return 转换后的坐标点
     */
    public static double[] coordinateConvertor(double x, double y, String imagePath, String coordinateType){
        Dataset dataset = gdal.Open(imagePath);
        return coordinateConvertor(x,y,dataset,coordinateType);
    }


    /**
     * 坐标转换 经纬度、投影坐标相互转换
     * @param x
     * @param y
     * @param dataset gdal dataset
     * @param coordinateType 坐标类型
     * @return 转换后的坐标点
     */
    public static double[] coordinateConvertor(double x, double y, Dataset dataset, String coordinateType){
        double[] coordinate = new double[]{0,0,0};
        String projection = dataset.GetProjection();
        CoordinateTransformation coordinateTransformation;
        SpatialReference srcSpatialReference = new SpatialReference(projection);
        SpatialReference destSpatialReference =  srcSpatialReference.CloneGeogCS();

        if (COORDINATE_LONLAT.equals(coordinateType))
            coordinateTransformation = new CoordinateTransformation(srcSpatialReference, destSpatialReference);
        else
            coordinateTransformation = new CoordinateTransformation(destSpatialReference, srcSpatialReference);
        coordinateTransformation.TransformPoint(coordinate, x, y);

        return coordinate;
    }


    /**
     * 将经纬度坐标转换成像素坐标
     * @param lon
     * @param lat
     * @param dataset
     * @param coordinateType
     * @return
     */
    public static double[] convertCoordinateToPixel(double lon, double lat, Dataset dataset, String coordinateType){
        double[] srcInfo;
        int srcWidth = dataset.getRasterXSize();
        int srcHeight = dataset.getRasterYSize();
        String originalCoordinateType = isProjection(dataset) ? GeoUtils.COORDINATE_PROJECTION : GeoUtils.COORDINATE_LONLAT;
        srcInfo = getCoordinateRange(dataset,originalCoordinateType);

        double srcLatRange = srcInfo[3] - srcInfo[1];
        double srcLonRange = srcInfo[2] - srcInfo[0];

        int xPixel = (int) (srcWidth * (lon - srcInfo[0]) / srcLonRange);
        int yPixel = (int) (srcHeight * (srcInfo[3] - lat) / srcLatRange);

        return new double[]{xPixel,yPixel};
    }


    /**
     * 将经纬度坐标转换成像素坐标
     * @param lon
     * @param lat
     * @param imagePath
     * @param coordinateType
     * @return
     */
    public static double[] convertCoordinateToPixel(double lon, double lat, String imagePath, String coordinateType){
        Dataset dataset = gdal.Open(imagePath);
        return convertCoordinateToPixel(lon,lat,dataset,coordinateType);
    }

    /**
     * 将经纬度范围转换为像素范围
     * @param range 经纬度坐标点
     * @param dataset 原始影像
     * @return 切片的像素范围
     */
    private static double[] convertCoordinateRangeToPixelRange(double[] range, Dataset dataset, String coordinateType){
        double[] upLeftPoint = convertCoordinateToPixel(range[0],range[3],dataset,coordinateType);
        double[] bottomRight = convertCoordinateToPixel(range[2],range[1],dataset,coordinateType);

        return new double[]{upLeftPoint[0],upLeftPoint[1],bottomRight[0],bottomRight[1]};
    }


    /**
     * 将经纬度范围转换为像素范围
     * @param range 经纬度坐标点
     * @param imagePath 原始影像路径
     * @return 切片的像素范围
     */
    private static double[] convertCoordinateRangeToPixelRange(double[] range, String imagePath, String coordinateType){
        Dataset dataset = gdal.Open(imagePath);
        return convertCoordinateRangeToPixelRange(range,dataset,coordinateType);
    }

}
