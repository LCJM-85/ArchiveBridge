package edu.scau.scauarchiveinsight.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfToImageService {

    // ====================== 固定配置（你的项目相对路径）======================
    private static final String PYTHON_PATH = "src/main/python/.venv/Scripts/python.exe";
    private static final String SCRIPT_PATH = "src/main/python/pdf2image/pdf2image.py";

    /**
     * 调用 Python 将 PDF 转成图片（输出到 PDF 同目录）
     * @param pdfAbsolutePath 传入 PDF 的绝对路径
     * @return 生成的所有图片的绝对路径列表
     */
    public List<String> convertPdfToImages(String pdfAbsolutePath) {
        List<String> imagePaths = new ArrayList<>();

        try {
            // 构建命令：python 脚本路径 PDF路径
            ProcessBuilder pb = new ProcessBuilder(
                    PYTHON_PATH,
                    SCRIPT_PATH,
                    pdfAbsolutePath
            );

            Process process = pb.start();

            // 读取 Python 返回的图片路径（每行一个路径）
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8")
            );

            String line;
            while ((line = reader.readLine()) != null) {
                imagePaths.add(line.trim());
            }

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return imagePaths;
    }
}