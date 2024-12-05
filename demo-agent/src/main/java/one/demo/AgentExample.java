package one.demo;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class AgentExample {
    /**
     * 以vm参数的形式载入，在程序main方法执行之前执行
     * 需要在manifest文件中配置属性Premain-Class
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        // Instrumentation 提供的 addTransformer 方法，在类加载时会回调 ClassFileTransformer 接口
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                // 只修改指定的 Class
                if (!className.equals("one/demo/DemoClass")) {
                    return classfileBuffer;
                }

                byte[] transformed = null;
                CtClass cl = null;
                try {
                    // CtClass、ClassPool、CtMethod、ExprEditor 都是javassist提供的字节码操作的类。Ct= ClassType
                    ClassPool classPool = ClassPool.getDefault();
                    cl = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                    CtMethod[] methods = cl.getDeclaredMethods();
                    for (CtMethod method : methods) {
                        // ExprEditor= Expression Editor，用于修改字节码 A translator of method bodies.
                        method.instrument(new ExprEditor() {
                            @Override
                            public void edit(MethodCall m) throws CannotCompileException {
                                // 修改方法调用
                                // m.replace("{ System.out.println(\"MethodCall: \" + $1); $proceed($$); }");
                                m.replace("{ long stime = System.currentTimeMillis();" + " $_ = $proceed($$);"
                                        + "System.out.println(\"" + m.getClassName() + "." + m.getMethodName()
                                        + " cost:\" + (System.currentTimeMillis() - stime) + \" ms\"); }");
                            }
                        });
                    }
                    // javassist会把输入的Java代码再编译成字节码byte[]
                    transformed = cl.toBytecode();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (null != cl) {
                        // ClassPool默认不会回收，需要手动清理
                        cl.detach();
                    }
                }
                return transformed;
            }
        });
        System.out.println();
    }

    /**
     * 以Attach的方式载入，在Java程序启动后执行
     * 其jar包的manifest需要配置属性Agent-Class
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("custom agent == agentmain");
    }
}
