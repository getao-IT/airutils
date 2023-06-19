package cn.aircas.utils.image;

import cn.aircas.utils.image.emun.CoordinateSystemType;
import lombok.Data;

@Data
public class ImageInfo {
    private int bands;
    private int width;
    private int height;
    private String bit;
    private double minLon;
    private double maxLon;
    private double minLat;
    private double maxLat;
    private double[] range;
    private String projection;
    private int lonRangeIndex;
    private double resolution;
    private String imageSizeSpec;
    private double[] projectionRange;
    private CoordinateSystemType coordinateSystemType;

}
