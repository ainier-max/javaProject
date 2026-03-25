package cbc.wcount.util;

import java.io.IOException;
import java.util.Random;

import org.json.JSONArray;

import static cbc.wcount.util.EquiSurface.writeStringToFile;


public class MainClass {
    public static void main(String[] args) throws IOException {
        System.out.println("开始执行等值面生成");
        long start = System.currentTimeMillis();
        EquiSurface equiSurface = new EquiSurface();
        //1、载入气象900+点数据，json数组数据
        String jsonFile = "E:\\workspaceForCom\\后端实现等值图（汤）\\等值面\\等值面\\isopleth\\数据\\rainText.json";
        JSONArray jsonArray =new JSONArray(equiSurface.readJsonArrayFromFile(jsonFile)) ;
        double[][] trainData = new double[jsonArray.length()][3];
        for (int i = 0; i < jsonArray.length(); i++){
            trainData[i][0] = jsonArray.getJSONObject(i).getDouble("lon");
            trainData[i][1] = jsonArray.getJSONObject(i).getDouble("lat");
            trainData[i][2] = jsonArray.getJSONObject(i).getDouble("val");
        }
//        //2、载入测试点数据，geojson格式
//        String jsonFile = "E:\\3.示例工程\\等值面\\isopleth\\数据\\rainText1.geojson";
//        org.json.JSONObject json =new org.json.JSONObject(readJsonArrayFromFile(jsonFile));
//        JSONArray feats = json.getJSONArray("features");
//        double[][] trainData = new double[feats.length()][3];
//        for (int i = 0; i < feats.length(); i++){
//            double prop = feats.getJSONObject(i).getJSONObject("properties").getDouble("val");
//            org.json.JSONArray coordinates = feats.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates");
//            trainData[i][0] = coordinates.getDouble(0);
//            trainData[i][1] = coordinates.getDouble(1);
//            trainData[i][2] = prop;
//        }

        double[] dataInterval = new double[]{0, 10, 25, 50,100,250,400,1000};
        //边界的空间文件路径
        //String boundryFile = "E:\\workspaceForCom\\后端实现等值图（汤）\\等值面\\等值面\\isopleth\\数据\\数字沙盘范围.shp";
        String boundryFile = "E:\\workspaceForCom\\后端实现等值图（汤）\\等值面\\等值面\\isopleth\\数据\\引汤灌区范围（一张图成果）.shp";
        int[] size = new int[]{100, 100};//越大越细，执行越慢
        //是否裁切边界，false不裁切为矩形
        boolean isclip = true;

        try {
            //*****核心***执行生成等值面，形成geojson***************
            String strJson = equiSurface.calEquiSurface(trainData, dataInterval, size, boundryFile, isclip);
            System.out.println("差值成功, 结果：" +strJson);
            //保存到文件，名称随机result0-100
//            Random random = new Random();
//            int randomNumber = random.nextInt(100 ) ;
//
//            String strFile = "E:\\3.示例工程\\等值面\\isopleth\\结果\\result"+randomNumber+".json";
//            writeStringToFile(strJson,strFile);

            System.out.println(strFile + "差值成功, 共耗时" + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
