package edu.scau.scauarchiveinsight.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class PythonProcessManagerTest {
    @Test
    void destroyDisablesWatchdogAndInterruptsItsSleep() throws Exception {
        PythonProcessManager manager = new PythonProcessManager();
        ReflectionTestUtils.invokeMethod(manager, "startWatchdog");
        manager.destroy();
        assertEquals(false, ReflectionTestUtils.getField(manager, "running"));
        Thread watchdog = (Thread) ReflectionTestUtils.getField(manager, "watchdogThread");
        watchdog.join(1000);
        assertFalse(watchdog.isAlive(), "关闭后旧监控线程必须退出");
    }

}
