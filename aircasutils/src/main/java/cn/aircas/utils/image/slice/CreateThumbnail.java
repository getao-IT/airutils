package cn.aircas.utils.image.slice;

import cn.aircas.utils.file.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

@Slf4j
public class CreateThumbnail {

	private static final long BIG_FILE_SIZE = 150 * 1024L;

	public static boolean createThumbnail(String filePath, String output, int maxLength) {
		float scale ;
		gdal.AllRegister();
		Driver pDriver = gdal.GetDriverByName("MEM");
		gdal.SetConfigOption("GDAL_PAM_ENABLED","FALSE");
		gdal.SetConfigOption("GDAL_FILENAME_IS_UTF8", "YES");
		gdal.SetConfigOption("SHAPE_ENCODING","");
		Dataset pDataset = gdal.Open(filePath, gdalconst.GA_ReadOnly);

		int xSize,ySize;
		int dataXSize = pDataset.GetRasterXSize();
		int dataYSize = pDataset.getRasterYSize();
		if (dataXSize > dataYSize) {
			xSize = maxLength;
			scale = (float) maxLength / (float)dataXSize;
			ySize = (int) (dataYSize * scale);
		}
		else {
			ySize = maxLength;
			scale = (float)maxLength / (float)dataYSize;
			xSize = (int) (scale * dataXSize);
		}

		int pDSOutBand = Math.min(pDataset.getRasterCount(), 3);
		Dataset pDSOut = pDriver.Create("", xSize, ySize,pDSOutBand, gdalconst.GDT_Byte);
		SliceGenerateUtil.sliceDataset(pDataset,pDSOut,0,0,dataXSize,dataYSize,xSize,ySize);

		Driver poDriver = gdal.GetDriverByName("JPEG");
		poDriver.CreateCopy(output,pDSOut);
		poDriver.delete();
		pDSOut.delete();
		return true;
	}


	/**
	 * 创建base64缩略图，满足一定大小的做缩略图
	 * @param samplePath
	 * @param thumbPath
	 * @param size
	 * @return
	 */
	public static String createBase64Thumbnail(String samplePath, String thumbPath, int size){
		String thumbnail = null;
		boolean generateThumbnail = true;

		try {
			byte[] thumbnailData;
			File sampleFile = FileUtils.getFile(samplePath);

			//如果不为tiff或者tif格式，并且文件小于512k，则不进行缩略图制作
			if (!FilenameUtils.isExtension(sampleFile.getName(),new String[]{"tif","tiff","TIF","TIFF"})
					&& sampleFile.length() < BIG_FILE_SIZE)
				thumbnailData = FileUtils.getImageByteArray(FileUtils.getStringPath(samplePath));
			else{
				CreateThumbnail.createThumbnail(samplePath,thumbPath,size);
				thumbnailData = FileUtils.getImageByteArray(FileUtils.getStringPath(thumbPath));
			}

			if (thumbnailData!=null){
				thumbnail = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(thumbnailData);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("生成影像: {} 的缩略图失败",samplePath);
		}

		if (generateThumbnail) {
			try {
				org.apache.commons.io.FileUtils.forceDelete(new File(thumbPath));
			} catch (IOException e) {
				log.error("删除缩略图失败：{}",thumbPath);
			}
		}

		return thumbnail;
	}
}
