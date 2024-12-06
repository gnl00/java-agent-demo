package one.demo;

import java.lang.instrument.Instrumentation;

public class MyAgent {

    public static void premain(String agentArgs) {
        System.out.println("MyAgent pre-main one arg");
    }

    /**
     * 带 Instrumentation 参数的方法优先级高于 premain(String)，如果都存在，优先加载 premain(String, Instrumentation)
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("MyAgent pre-main, two args");
    }

    /**
     * 带 Instrumentation 参数的方法优先级高于 agentmain(String)，如果都存在，优先加载 agentmain(String, Instrumentation)
     */
    public static void agentmain(String agentArgs) {}

    /**
     * 以Attach的方式载入，在Java程序启动后执行
     * 其jar包的manifest需要配置属性Agent-Class
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        // attach 成功后，会在其他 Java 进程的执行过程中输出
        System.out.println("MyAgent agent-main");
    }
}
