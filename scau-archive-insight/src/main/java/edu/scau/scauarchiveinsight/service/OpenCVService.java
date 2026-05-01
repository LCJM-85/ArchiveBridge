package edu.scau.scauarchiveinsight.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class OpenCVService {

    /**
     * 调用 Python 脚本对图片进行清晰化处理
     * @param imagePath 原始图片路径
     * @return 处理后的图片路径（Python 脚本返回的）
     */
    public String enhanceImage(String imagePath) {
        try {
            String python = ".venv/Scripts/python.exe";

            String scriptPath = "src/main/python/opencv/opencv.py";

            ProcessBuilder pb = new ProcessBuilder(
                    python,
                    scriptPath,
                    imagePath
            );

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8")
            );

            String resultPath = reader.readLine();

            process.waitFor();

            if (resultPath == null || resultPath.startsWith("ERROR")) {
                return resultPath;
            }

            return resultPath;

        } catch (Exception e) {
            e.printStackTrace();
            return "图片增强失败：" + e.getMessage();
        }
    }
}
