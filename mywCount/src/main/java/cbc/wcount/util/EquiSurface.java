package cbc.wcount.util;

import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

import org.opengis.feature.simple.SimpleFeatureType;

import wcontour.Contour;
import wcontour.Interpolate;
import wcontour.global.Border;
import wcontour.global.PointD;
import wcontour.global.PolyLine;
import wcontour.global.Polygon;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class EquiSurface {
    /**
     * 生成等值面
     *
     * @param trainData    训练数据,[[lon1,lat1,val1],[lon2,lat2,val2],.....]
     * @param dataInterval 数据间隔[0,10,20,30,.....]
     * @param size         大小，宽，高
     * @param boundryFile  四至，边界文件SHP
     * @param isclip       是否裁剪
     * @return 返回geojson数据，配置颜色使用lvalue字段
     */
    public String calEquiSurface(double[][] trainData,
                                 double[] dataInterval,
                                 int[] size,
                                 String boundryFile,
                                 boolean isclip) {
        String geojsonpogylon = "";
        try {
            double _undefData = -9999.0;
            SimpleFeatureCollection polygonCollection = null;
            List<PolyLine> cPolylineList = new ArrayList<PolyLine>();
            List<Polygon> cPolygonList = new ArrayList<Polygon>();

            int width = size[0],
                    height = size[1];
            double[] _X = new double[width];
            double[] _Y = new double[height];

            File file = new File(boundryFile);
            ShapefileDataStore shpDataStore = null;

            shpDataStore = new ShapefileDataStore(file.toURL());
            //设置编码
            Charset charset = Charset.forName("GBK");
            shpDataStore.setCharset(charset);
            String typeName = shpDataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = null;
            featureSource = shpDataStore.getFeatureSource(typeName);
            SimpleFeatureCollection fc = featureSource.getFeatures();

            double minX = fc.getBounds().getMinX();
            double minY = fc.getBounds().getMinY();
            double maxX = fc.getBounds().getMaxX();
            double maxY = fc.getBounds().getMaxY();

            Interpolate.createGridXY_Num(minX, minY, maxX, maxY, _X, _Y);

            double[][] _gridData = new double[width][height];

            int nc = dataInterval.length;

            _gridData = Interpolate.interpolation_IDW_Neighbor(trainData,
                    _X, _Y, 12, _undefData);// IDW插值
            int[][] S1 = new int[_gridData.length][_gridData[0].length];
            List<Border> _borders = Contour.tracingBorders(_gridData, _X, _Y,
                    S1, _undefData);

            cPolylineList = Contour.tracingContourLines(_gridData, _X, _Y, nc,
                    dataInterval, _undefData, _borders, S1);// 生成等值线

            cPolylineList = Contour.smoothLines(cPolylineList);// 平滑

            cPolygonList = Contour.tracingPolygons(_gridData, cPolylineList,
                    _borders, dataInterval);

            geojsonpogylon = getPolygonGeoJson(cPolygonList);

            if (isclip) {

                SimpleFeatureCollection featureCollection1 =  readGeoJson(geojsonpogylon.toString());
                SimpleFeatureCollection sm = clipPolygonFeatureCollection(fc, featureCollection1);
                FeatureCollection featureCollection = sm;

                FeatureJSON featureJSON = new FeatureJSON();
                StringWriter writer = new StringWriter();
                featureJSON.writeFeatureCollection(featureCollection, writer);
                geojsonpogylon = writer.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return geojsonpogylon;
    }

    /**
     * 裁剪图形
     * @param fc
     * @return
     */
    private SimpleFeatureCollection clipPolygonFeatureCollection(FeatureCollection fc,
                                                                 SimpleFeatureCollection gs) throws SchemaException, IOException {
        SimpleFeatureCollection simpleFeatureCollection = null;
        SimpleFeatureType TYPE = DataUtilities.createType("polygons",
                "the_geom:MultiPolygon,lvalue:double,hvalue:double");
        List<SimpleFeature> list = new ArrayList<>();
        SimpleFeatureIterator contourFeatureIterator = gs.features();
        FeatureIterator dataFeatureIterator = fc.features();
        while (dataFeatureIterator.hasNext()){
            SimpleFeature dataFeature = (SimpleFeature) dataFeatureIterator.next();
            Geometry dataGeometry = (Geometry) dataFeature.getDefaultGeometry();
            contourFeatureIterator = gs.features();
            while (contourFeatureIterator.hasNext()){
                SimpleFeature contourFeature = contourFeatureIterator.next();
                Geometry contourGeometry = (Geometry) contourFeature.getDefaultGeometry();
                Double lv = (Double) contourFeature.getProperty("lvalue").getValue();
                Double hv = (Double) contourFeature.getProperty("hvalue").getValue();
                if (dataGeometry.getGeometryType() == "MultiPolygon"){
                    for (int i = 0; i <dataGeometry.getNumGeometries(); i++){
                        Geometry geom = dataGeometry.getGeometryN(i);
                        if (geom.intersects(contourGeometry)){
                            Geometry geo = geom.intersection(contourGeometry);
                            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
                            featureBuilder.add(geo);
                            featureBuilder.add(lv);
                            featureBuilder.add(hv);
                            SimpleFeature feature = featureBuilder.buildFeature(null);
                            list.add(feature);

                        }
                    }

                } else {
                    if (dataGeometry.intersects(contourGeometry)){
                        Geometry geo = dataGeometry.intersection(contourGeometry);
                        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
                        featureBuilder.add(geo);
                        featureBuilder.add(lv);
                        featureBuilder.add(hv);
                        SimpleFeature feature = featureBuilder.buildFeature(null);
                        list.add(feature);

                    }

                }

            }
        }

        contourFeatureIterator.close();
        dataFeatureIterator.close();
        simpleFeatureCollection = new ListFeatureCollection(TYPE, list);

        return simpleFeatureCollection;

    }

    private String getPolygonGeoJson(List<Polygon> cPolygonList) {
        String geo = null;
        String geometry = " { \"type\":\"Feature\",\"geometry\":";
        String properties = ",\"properties\":{ \"hvalue\":";

        String head = "{\"type\": \"FeatureCollection\"," + "\"features\": [";
        String end = "  ] }";
        if (cPolygonList == null || cPolygonList.size() == 0) {
            return null;
        }
        try {
            for (Polygon pPolygon : cPolygonList) {

                List<Object> ptsTotal = new ArrayList<Object>();
                List<Object> pts = new ArrayList<Object>();

                PolyLine pline = pPolygon.OutLine;

                for (PointD ptD : pline.PointList) {
                    List<Double> pt = new ArrayList<Double>();
                    pt.add(ptD.X);
                    pt.add(ptD.Y);
                    pts.add(pt);
                }

                ptsTotal.add(pts);

                if (pPolygon.HasHoles()) {
                    for (PolyLine cptLine : pPolygon.HoleLines) {
                        List<Object> cpts = new ArrayList<Object>();
                        for (PointD ccptD : cptLine.PointList) {
                            List<Double> pt = new ArrayList<Double>();
                            pt.add(ccptD.X);
                            pt.add(ccptD.Y);
                            cpts.add(pt);
                        }
                        if (cpts.size() > 0) {
                            ptsTotal.add(cpts);
                        }
                    }
                }

                JSONObject js = new JSONObject();
                js.put("type", "Polygon");
                js.put("coordinates", ptsTotal);
                double hv = pPolygon.HighValue;
                double lv = pPolygon.LowValue;

                if (hv == lv) {
                    if (pPolygon.IsClockWise) {
                        if (!pPolygon.IsHighCenter) {
                            hv = hv - 0.1;
                            lv = lv - 0.1;
                        }

                    } else {
                        if (!pPolygon.IsHighCenter) {
                            hv = hv - 0.1;
                            lv = lv - 0.1;
                        }
                    }
                } else {
                    if (!pPolygon.IsClockWise) {
                        lv = lv + 0.1;
                    } else {
                        if (pPolygon.IsHighCenter) {
                            hv = hv - 0.1;
                        }
                    }

                }

                geo = geometry + js.toString() + properties + hv
                        + ", \"lvalue\":" + lv + "} }" + "," + geo;

            }
            if (geo.contains(",")) {
                geo = geo.substring(0, geo.lastIndexOf(","));
            }

            geo = head + geo + end;
        } catch (Exception e) {
            e.printStackTrace();
            return geo;
        }
        return geo;
    }

    /**
     * 返回文件内容字符串
     */
    public static String readJsonArrayFromFile(String filePath) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                contentBuilder.append(line);
            }
        }
        String jsonContent = contentBuilder.toString();
        return jsonContent;//new JSONArray(readJsonArrayFromFile(jsonContent));
    }

    public static SimpleFeatureCollection readGeoJson(String geojsonString) throws IOException {
        FeatureJSON featureJSON = new FeatureJSON();
        try (StringReader reader = new StringReader(geojsonString)) {
            return (SimpleFeatureCollection) featureJSON.readFeatureCollection(reader);
        }
    }

    /**
     * 写入内容到文件
     */
    public static void writeStringToFile(String content, String filePath) throws IOException {
        File targetFile = new File(filePath);
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
        }
        if (!targetFile.exists() && !targetFile.createNewFile()) {
            throw new IOException("Failed to create file: " + targetFile.getAbsolutePath());
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
            writer.write(content);
        }
    }


}
