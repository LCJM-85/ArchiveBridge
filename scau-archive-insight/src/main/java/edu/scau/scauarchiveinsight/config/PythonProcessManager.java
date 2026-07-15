package edu.scau.scauarchiveinsight.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class PythonProcessManager implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(PythonProcessManager.class);
    private Process pythonProcess;

    @Value("${llm.api-key}")
    private String llmApiKey;

    @Value("${llm.base-url}")
    private String llmBaseUrl;

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private String dbPort;

    @Value("${DB_NAME:scau_archive}")
    private String dbName;

    @Value("${DB_USER:postgres}")
    private String dbUser;

    @Value("${DB_PASS:123456}")
    private String dbPass;

    @Override
    public void afterPropertiesSet() {
        String userDir = System.getProperty("user.dir");
        File scriptInSub = new File(userDir + "/src/main/python/ai_assistant/main.py");
        File scriptInRoot = new File(userDir + "/scau-archive-insight/src/main/python/ai_assistant/main.py");
        File scriptFile = scriptInSub.exists() ? scriptInSub : scriptInRoot;

        if (!scriptFile.exists()) {
            log.warn("AI 助手 Python 脚本未找到，跳过启动: {}", scriptFile.getAbsolutePath());
            return;
        }

        String venvPython = scriptFile.getParentFile().getParentFile().getAbsolutePath()
                + "/.venv/Scripts/python.exe";

        ProcessBuilder pb = new ProcessBuilder(venvPython, scriptFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        pb.environment().put("LLM_API_KEY", llmApiKey != null ? llmApiKey : "");
        pb.environment().put("LLM_BASE_URL", llmBaseUrl != null ? llmBaseUrl : "");
        pb.environment().put("DB_HOST", dbHost);
        pb.environment().put("DB_PORT", dbPort);
        pb.environment().put("DB_NAME", dbName);
        pb.environment().put("DB_USER", dbUser);
        pb.environment().put("DB_PASS", dbPass);

        try {
            pythonProcess = pb.start();

            // 读取 Python 进程的输出和错误，写入日志
            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains("Error") || line.contains("ERROR") || line.contains("Traceback")) {
                            log.error("[AI Python] {}", line);
                        } else if (line.contains("Uvicorn running")) {
                            log.info("AI 助手 Python 服务已启动 (pid={})", pythonProcess.pid());
                        } else {
                            log.debug("[AI Python] {}", line);
                        }
                    }
                } catch (IOException ignored) {}
            });
            reader.setDaemon(true);
            reader.start();

            // 等一秒检查进程是否还活着
            Thread.sleep(1000);
            if (!pythonProcess.isAlive()) {
                log.error("AI 助手 Python 服务启动后异常退出，请检查配置");
            }
        } catch (IOException e) {
            log.error("启动 AI 助手 Python 服务失败", e);
        } catch (InterruptedException ignored) {
        }
    }

    @PreDestroy
    public void destroy() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroy();
            log.info("AI 助手 Python 服务已停止");
        }
    }
}
