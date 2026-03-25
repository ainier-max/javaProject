package cbc.wcount.util;

import org.json.JSONArray;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static cbc.wcount.util.EquiSurface.writeStringToFile;

public class MainClass {
    public static void main(String[] args) throws IOException {
        System.out.println("开始执行等值面生成");
        long start = System.currentTimeMillis();
        Path projectDir = resolveProjectDir();
        EquiSurface equiSurface = new EquiSurface();

        String jsonFile = projectDir.resolve(Paths.get("数据", "rainText.json")).toString();
        JSONArray jsonArray = new JSONArray(equiSurface.readJsonArrayFromFile(jsonFile));
        double[][] trainData = new double[jsonArray.length()][3];
        for (int i = 0; i < jsonArray.length(); i++) {
            trainData[i][0] = jsonArray.getJSONObject(i).getDouble("lon");
            trainData[i][1] = jsonArray.getJSONObject(i).getDouble("lat");
            trainData[i][2] = jsonArray.getJSONObject(i).getDouble("val");
        }

//        String jsonFile = projectDir.resolve(Paths.get("数据", "rainText1.geojson")).toString();
//        org.json.JSONObject json = new org.json.JSONObject(equiSurface.readJsonArrayFromFile(jsonFile));
//        JSONArray feats = json.getJSONArray("features");
//        double[][] trainData = new double[feats.length()][3];
//        for (int i = 0; i < feats.length(); i++) {
//            double prop = feats.getJSONObject(i).getJSONObject("properties").getDouble("val");
//            org.json.JSONArray coordinates = feats.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates");
//            trainData[i][0] = coordinates.getDouble(0);
//            trainData[i][1] = coordinates.getDouble(1);
//            trainData[i][2] = prop;
//        }

        double[] dataInterval = new double[]{0, 10, 25, 50, 100, 250, 400, 1000};
//        String boundryFile = projectDir.resolve(Paths.get("数据", "数字沙盘范围.shp")).toString();
        String boundryFile = projectDir.resolve(Paths.get("数据", "引汤灌区范围（一张图成果）.shp")).toString();
        int[] size = new int[]{100, 100};
        boolean isclip = true;

        try {
            String strJson = equiSurface.calEquiSurface(trainData, dataInterval, size, boundryFile, isclip);
            System.out.println("等值面执行成功, 结果: " + strJson);

            String randomNumber = String.valueOf(System.currentTimeMillis());
            String strFile = projectDir.resolve(Paths.get("结果", "result" + randomNumber + ".json")).toString();
            writeStringToFile(strJson, strFile);

            System.out.println(strFile + " 输出成功, 共耗时 " + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Path resolveProjectDir() {
        Path workingDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (isProjectDir(workingDir)) {
            return workingDir;
        }

        Path moduleDir = workingDir.resolve("mywCount").normalize();
        if (isProjectDir(moduleDir)) {
            return moduleDir;
        }

        throw new IllegalStateException("Cannot find project directory from " + workingDir);
    }

    private static boolean isProjectDir(Path dir) {
        return Files.exists(dir.resolve("pom.xml")) && Files.isDirectory(dir.resolve("数据"));
    }
}
