package cn.aircas.utils.image.slice;

import cn.aircas.utils.file.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import java.io.File;
import java.io.IOException;

/**
 * 影像切片工具
 * @author vanishrain
 */
@Slf4j
public class SliceGenerateUtil {

    /**
     * 根据范围对影像进行切割
     * @param range 像素范围
     * @param srcPath
     * @param outputPath
     */
    public static void generateSlice(double[] range, String srcPath, String outputPath){
        gdal.AllRegister();
        gdal.SetConfigOption("GDAL_PAM_ENABLED","FALSE");

        String driverName = "GTiff";
        File srcFile = new File(srcPath);
        String fileExtension = FilenameUtils.getExtension(srcFile.getName());
        if (fileExtension.equalsIgnoreCase("png"))
            driverName = "PNG";
        if (fileExtension.equalsIgnoreCase("jpg") || fileExtension.equalsIgnoreCase("jpeg"))
            driverName = "JPEG";
        if (fileExtension.equalsIgnoreCase("tif") || fileExtension.equalsIgnoreCase("tiff"))
            driverName = "GTiff";
        Dataset dataset = gdal.Open(srcPath);
        Driver jpegDriver = gdal.GetDriverByName(driverName);
        Driver pDriver = gdal.GetDriverByName("MEM");
        int srcWidth = dataset.getRasterXSize();
        int srcHeight = dataset.getRasterYSize();

        int minX = range[0] >= 0 ? (int) range[0] : 0;
        int minY = range[1] >= 0 ? (int) range[1] : 0;
        int maxX = range[2] >= srcWidth ? srcWidth : (int) range[2];
        int maxY = range[3] >= srcHeight ? srcHeight : (int) range[3];
        int sliceWidth = maxX - minX;
        int sliceHeight = maxY - minY;

        File outputFile = new File(outputPath);
        if(!outputFile.getParentFile().exists())
            outputFile.getParentFile().mkdirs();

        int pDSOutBand = Math.min(dataset.getRasterCount(), 3);
        Dataset pDSOut = pDriver.Create(outputPath, sliceWidth, sliceHeight, pDSOutBand, gdalconst.GDT_Byte);
        sliceDataset(dataset,pDSOut,minX,minY,sliceWidth,sliceHeight,sliceWidth,sliceHeight);

        double[] srcGeoTransform = dataset.GetGeoTransform();
        if(srcGeoTransform!=null){
            double sliceMinLon = srcGeoTransform[0] + range[0] * srcGeoTransform[1] + range[1] * srcGeoTransform[2];
            double sliceMaxLat = srcGeoTransform[3] + range[0] * srcGeoTransform[4] + range[1] * srcGeoTransform[5];
            double[] sliceGeoTransform = new double[]{sliceMinLon,srcGeoTransform[1],srcGeoTransform[2],sliceMaxLat,srcGeoTransform[4],srcGeoTransform[5]};
            pDSOut.SetGeoTransform(sliceGeoTransform);
        }

        jpegDriver.CreateCopy(outputPath,pDSOut);
        pDSOut.delete();
        dataset.delete();
        jpegDriver.delete();
    }


    /**
     * 根据原图像数据和切片xml对图像进行切割
     * 存在output路径中
     * @param sliceNo
     * @param size
     * @param srcPath
     * @param outputPath
     */
    public static double[] generateSliceByFixedSize(int sliceNo, int size, String srcPath, String outputPath, boolean consistency){
        gdal.AllRegister();
        gdal.SetConfigOption("GDAL_PAM_ENABLED","FALSE");
        double[] sliceGeoTransform = null;
        Dataset dataset = gdal.Open(srcPath);

        double[] range = sliceNo2PixelRange(dataset.getRasterXSize(),dataset.getRasterYSize(),sliceNo,size,consistency);

        double[] srcGeoTransform = dataset.GetGeoTransform();
        if(srcGeoTransform!=null){
            double sliceMinLon = srcGeoTransform[0] + range[0] * srcGeoTransform[1] + range[1] * srcGeoTransform[2];
            double sliceMaxLat = srcGeoTransform[3] + range[0] * srcGeoTransform[4] + range[1] * srcGeoTransform[5];
            sliceGeoTransform = new double[]{sliceMinLon,srcGeoTransform[1],srcGeoTransform[2],sliceMaxLat,srcGeoTransform[4],srcGeoTransform[5]};
        }

        Driver pDriver = gdal.GetDriverByName("GTiff");
        int minX = (int)range[0], minY = (int)range[1], maxX = (int)range[2], maxY = (int)range[3];
        int sliceWidth = maxX - minX, sliceHeight = maxY - minY;
        File outputFile = new File(outputPath);
        if(!outputFile.getParentFile().exists())
            outputFile.getParentFile().mkdirs();

        int pDSOutBand = dataset.getRasterCount() == 2 ? 1 : Math.min(dataset.getRasterCount(), 3);
        Dataset pDSOut = pDriver.Create(outputPath, sliceWidth, sliceHeight, pDSOutBand, gdalconst.GDT_Byte);
        if (sliceGeoTransform !=null)
            pDSOut.SetGeoTransform(sliceGeoTransform);
        sliceDataset(dataset,pDSOut,minX,minY,sliceWidth,sliceHeight,sliceWidth,sliceHeight);

        dataset.delete();
        pDSOut.delete();

        return range;
    }

    /**
     * 计算第sliceNo块切片的像素范围
     * @param imageWidth
     * @param imageHeight
     * @param sliceNo
     * @param sliceSize
     * @param consistency 针对每行每列最后一张图和其他图大小可能不一致的问题，判断是否需要维持一致性
     * @return
     */
    public static double[] sliceNo2PixelRange(int imageWidth, int imageHeight, int sliceNo, int sliceSize, boolean consistency){
        int maxWidthCount = (int)Math.ceil((double) imageWidth / sliceSize);

        double[] pixelRange = new double[4];
        pixelRange[0] = sliceNo % maxWidthCount  * sliceSize;
        pixelRange[1] = sliceNo / maxWidthCount * sliceSize;
        pixelRange[2] = Math.min(pixelRange[0] + sliceSize, imageWidth);
        pixelRange[3] = Math.min(pixelRange[1] + sliceSize, imageHeight);

        if (((pixelRange[0] + sliceSize) > imageWidth) && consistency) {
            pixelRange[0]= imageWidth - sliceSize;
        }

        if (((pixelRange[1] + sliceSize) > imageHeight) && consistency) {
            pixelRange[1] = imageHeight - sliceSize;
        }

        return pixelRange;
    }

    /**
     * 计算像素点落在哪个切片号内
     * @param x 像素x
     * @param y 像素y
     * @param sliceSize 切片大小
     * @param imageWidth 影像宽度
     * @return
     */
    public static int pixelPoint2SliceNo(double x, double y, int sliceSize, int imageWidth){
        int maxWidthCount = (int)Math.ceil((double) imageWidth / sliceSize);
        return (int)y / sliceSize * maxWidthCount + (int)x / sliceSize;
    }


    public static void sliceDataset(Dataset dataset, Dataset pDSOut, int minX, int minY, int offsetWidth, int offsetLength, int sliceWidth, int sliceHeight){
        int[] bands = getRGBBand(dataset);
        pDSOut.SetProjection(dataset.GetProjection());
        float[] imageHistogramMinMax = calculateImageHistogramMinMax(dataset);
        for (int band = 1; band <= bands.length; band++) {
            Band pBandRead = dataset.GetRasterBand(bands[band-1]);
            Band pBandWrite = pDSOut.GetRasterBand(band);

            if (pBandRead.GetRasterDataType() == 2 || pBandRead.GetRasterDataType() == 3){
                int[] out = new int[sliceWidth*sliceHeight];
                float minBandHist = imageHistogramMinMax[(band-1)*2];
                float maxBandHist = imageHistogramMinMax[(band-1)*2 + 1];
                pBandRead.ReadRaster(minX,minY,offsetWidth,offsetLength,sliceWidth,sliceHeight,
                        gdalconst.GDT_Int32,out);

                for (int index = 0; index < out.length ; index++){
                    int temp = (int) ((out[index] - minBandHist) * 255 / (maxBandHist - minBandHist + 1));
                    out[index] = temp;
                }
                pBandWrite.WriteRaster(0,0,sliceWidth,sliceHeight,out);
            }else {
                byte[] out = new byte[sliceWidth*sliceHeight];
                pBandRead.ReadRaster(minX,minY,offsetWidth,offsetLength,sliceWidth,sliceHeight,
                        gdalconst.GDT_Byte,out);
                pBandWrite.WriteRaster(0,0,sliceWidth,sliceHeight,out);
            }
            pBandRead.delete();
            pBandWrite.delete();
        }
    }

    /**
     * 根据波段数和位数选择波段
     * @param dataset
     * @return
     */
    public static int[] getRGBBand(Dataset dataset){
        int[] rgbBand;
        int bandCounts = dataset.getRasterCount() == 2 ? 1 : dataset.getRasterCount();
        int dataType = dataset.GetRasterBand(1).GetRasterDataType();

        if (dataType == 2){
            switch (bandCounts){
                case 1: rgbBand = new int[]{1} ;break;
                case 3: rgbBand = new int[]{1,2,3} ; break;
                case 4: rgbBand = new int[]{3,2,1}; break;
                case 60: rgbBand = new int[]{23,46,56}; break;
                case 63: rgbBand = new int[]{25,41,53}; break;
                case 123: rgbBand = new int[]{15,33,45}; break;
                default: rgbBand = new int[] {1,2,3} ;break;
            }
        }else {
            switch (bandCounts){
                case 1: rgbBand = new int[]{1} ;break;
                case 3: rgbBand = new int[]{1,2,3} ; break;
                case 4: rgbBand = new int[]{3,2,1}; break;
                case 63: rgbBand = new int[]{45,33,15}; break;
                case 123: rgbBand = new int[]{45,33,15}; break;
                case 182: rgbBand = new int[]{45,33,15}; break;
                case 288: rgbBand = new int[]{45,33,15}; break;
                default: rgbBand = new int[] {1,2,3} ;break;
            }
        }
        return rgbBand;
    }

    public static int[] getBandHist(String imagePath, int bandNo){
        gdal.AllRegister();
        int minHistogram = 1;
        Dataset dataset = gdal.Open(imagePath);
        int maxHistogram = dataset.GetRasterBand(1).GetRasterDataType() == 2 ? 65535 : 256;
        int[] bandHist = new int[maxHistogram];

        Band pBandRead = dataset.GetRasterBand(bandNo);
        pBandRead.GetHistogram(minHistogram,maxHistogram, bandHist, false, true);
        return bandHist;
    }

    /**
     * 计算直方图最大最小值
     * @param dataset
     * @return
     */
    public static float[] calculateImageHistogramMinMax(Dataset dataset){
        int minHistogram = 1;
        int maxHistogram = 65536;
        int[] bands = getRGBBand(dataset);
        float[] imageHistogramMinMax = new float[bands.length * 2]; //用一个数组存储不同波段内的大小值
        int[] bandHist = new int[maxHistogram];
        float[] histR = new float[maxHistogram];
        for(int band = 1; band <= bands.length; ++band)
        {
            float sumMin = 0;
            float sumMax = 0;
            float total = 0.0f;
            Band pBandRead = dataset.GetRasterBand(bands[band-1]);
            pBandRead.GetHistogram(minHistogram,maxHistogram, bandHist, false, true);
            for (int i = 0; i < maxHistogram; ++i)
            {
                total += bandHist[i];
            }
            for (int i = 0; i < maxHistogram; ++i)
            {
                histR[i] = (float)(bandHist[i]) / total;
            }

            //假设有黑边，从第256个像素开始计算，去掉前1%和后1%的像素
            int calculateMinStart = 256;
            for (int index = calculateMinStart; index < maxHistogram; ++index) {
                sumMin += histR[index];
                if (sumMin >= 0.2) {
                    imageHistogramMinMax[(band-1)*2] = index;
                    break;
                }
            }

            for (int index = maxHistogram - 1; index > imageHistogramMinMax[(band-1)*2]; --index)
            {
                sumMax += histR[index];
                if (sumMax >= 0.01)
                {
                    imageHistogramMinMax[(band-1)*2 + 1] = index;
                    break;
                }
            }
        }
        return imageHistogramMinMax;
    }

    public static void main(String[] args) {
        gdal.AllRegister();
        String na = "tef.tf";
        String sdf = FilenameUtils.getExtension(na);
        System.out.println("sdf");
        Dataset dataset = gdal.Open("d:\\meiguo1_1_10.tif");
        double[] range = new double[]{0,0,500,500};
        generateSlice(range,"d:\\meiguo1_1_10.tif","d:\\1.tif");

//        SliceGenerateUtil.generateSlice(range,"d:\\GF02_PA2_034331_20201225_MY151_01_078_L1A_01.tif","d:\\1.jpg");
//        String thumb = CreateThumbnail.createBase64Thumbnail("d:\\1.jpg","d:\\thumb_1.jpg",512);
//        System.out.println("sdf");
        //calculateImageHistogramMinMax(dataset);
    }

}
