package edu.scau.scauarchiveinsight;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import edu.scau.scauarchiveinsight.config.PythonProcessManager;
import edu.scau.scauarchiveinsight.service.OCRLogService;

@SpringBootTest
class ScauArchiveInsightApplicationTests {
    // 加载真实应用配置，但不启动外部进程、不改写用户现存任务。
    @MockBean PythonProcessManager python;
    @MockBean OCRLogService logs;

    @Test
    void contextLoads() {
    }

}
