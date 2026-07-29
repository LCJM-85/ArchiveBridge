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

    @Value("${python.venv-path:}")
    private String customVenvPath;

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

    @Value("${DB_PASS}")
    private String dbPass;

    private volatile boolean running = true;

    @Override
    public void afterPropertiesSet() {
        startPythonProcess();
        startWatchdog();
    }

    private void startPythonProcess() {
        String userDir = System.getProperty("user.dir");
        File scriptInSub = new File(userDir + "/src/main/python/ai_assistant/main.py");
        File scriptInRoot = new File(userDir + "/scau-archive-insight/src/main/python/ai_assistant/main.py");
        File scriptFile = scriptInSub.exists() ? scriptInSub : scriptInRoot;

        if (!scriptFile.exists()) {
            log.warn("AI 助手 Python 脚本未找到，跳过启动: {}", scriptFile.getAbsolutePath());
            return;
        }

        String venvPython = customVenvPath != null && !customVenvPath.isEmpty()
                ? customVenvPath
                : scriptFile.getParentFile().getParentFile().getAbsolutePath()
                    + "/.venv/Scripts/python.exe";

        ProcessBuilder pb = new ProcessBuilder(venvPython, scriptFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        pb.environment().put("GLM_API_KEY", llmApiKey != null ? llmApiKey : "");
        pb.environment().put("LLM_BASE_URL", llmBaseUrl != null ? llmBaseUrl : "");
        pb.environment().put("DB_HOST", dbHost);
        pb.environment().put("DB_PORT", dbPort);
        pb.environment().put("DB_NAME", dbName);
        pb.environment().put("DB_USER", dbUser);
        pb.environment().put("DB_PASS", dbPass);

        try {
            pythonProcess = pb.start();
            log.info("AI 助手 Python 服务启动中 (pid={})...", pythonProcess.pid());

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

            Thread.sleep(1000);
            if (!pythonProcess.isAlive()) {
                log.error("AI 助手 Python 服务启动后异常退出，请检查配置");
            }
        } catch (IOException e) {
            log.error("启动 AI 助手 Python 服务失败", e);
        } catch (InterruptedException ignored) {
        }
    }

    private void startWatchdog() {
        Thread watchdog = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(30_000);
                } catch (InterruptedException ignored) {
                    break;
                }
                if (!running) break;

                if (pythonProcess != null && !pythonProcess.isAlive()) {
                    log.warn("AI 助手 Python 服务已退出 (退出码: {})，正在重启...", pythonProcess.exitValue());
                    startPythonProcess();
                }
            }
        }, "python-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        log.debug("AI 助手 watchdog 已启动（每 30 秒检查一次）");
    }

    @PreDestroy
    public void destroy() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroy();
            log.info("AI 助手 Python 服务已停止");
        }
    }
}
