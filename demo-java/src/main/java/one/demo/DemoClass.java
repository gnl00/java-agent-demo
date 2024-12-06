package one.demo;

import java.util.concurrent.TimeUnit;

/**
 * 1、demo-agent 打包
 * 2、vm option 添加 -javaagent:demo-agent/target/demo-agent-1.0-SNAPSHOT.jar
 * 3、启动 DemoClass#main
 */
public class DemoClass {
    public static void main(String[] args) {
        DemoClass dc = new DemoClass();
        for (;;) {
            dc.timer();
        }
    }

    public void timer() {
        try {
            TimeUnit.SECONDS.sleep(2);
            System.out.println("current time=" + System.currentTimeMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
