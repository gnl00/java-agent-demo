# java-agent-demo
> 简言：使用 Java 字节码修改技术（javaassistant/ASM）+ JVMTI 保留的接口来实现 Java 字节码的动态修改。

> 头铁试了 javaassistant + JDK21，未支持。顺手查了一下 javaassistant 好像对 JDK17 以上的支持还不完整。

> *你发任你发，我用 Java 8<sub>tm</sub>*

## 了解 Java instrument

`java.lang.instrument` 是 Java 虚拟机提供的一个 API，允许 Java 程序在运行时动态地修改字节码。

```java
public interface Instrumentation {
    /**
     * 注册一个Transformer，从此之后的类加载都会被Transformer拦截。
     * Transformer可以直接对类的字节码byte[]进行修改
     */
    void addTransformer(ClassFileTransformer transformer);
    
    /**
     * 对JVM已经加载的类重新触发类加载。使用的就是上面注册的Transformer。
     * retransformation可以修改方法体，但是不能变更方法签名、增加和删除方法/类的成员属性
     */
    void retransformClasses(Class<?>... classes) throws UnmodifiableClassException;
    
    /**
     * 获取一个对象的大小
     */
    long getObjectSize(Object objectToSize);
    
    /**
     * 将一个jar加入到bootstrap classloader的 classpath里
     */
    void appendToBootstrapClassLoaderSearch(JarFile jarfile);
    
    /**
     * 获取当前被JVM加载的所有类对象
     */
    Class[] getAllLoadedClasses();
}
```

其中最常用的方法就是 addTransformer 了，这个方法可以在类加载时做拦截，对输入的类的字节码进行修改。

addTransformer 方法配置之后，后续的类加载都会被 Transformer 拦截。对于已经加载过的类，可以执行 retransformClasses 来重新触发这个 Transformer 的拦截。

## Java agent 的加载

一个 Java agent 既可以在 VM 启动时加载，也可以在 VM 启动后加载：
* 启动时加载：通过 VM 的启动参数 `-javaagent:**.jar` 来启动
* 启动后加载：在 VM 启动后的任何时间点，通过 attach api，动态地启动 agent

agent加载时，Java agent的jar包先会被加入到system class path中，然后agent的类会被system class loader加载。没错，这个system class loader就是所在的Java程序的class loader，这样agent就可以很容易的获取到想要的class。

对于VM启动时加载的Java agent，其premain方法会在程序main方法执行之前被调用，此时大部分Java类都没有被加载（“大部分”是因为，agent类本身和它依赖的类还是无法避免的会先加载的），是一个对类加载埋点做手脚（addTransformer）的好机会。如果此时premain方法执行失败或抛出异常，那么JVM的启动会被终止。

对于VM启动后加载的Java agent，其agentmain方法会在加载之时立即执行。如果agentmain执行失败或抛出异常，JVM会忽略掉错误，不会影响到正在running的Java程序。

## Attach API

Java agent可以在JVM启动后再加载，就是通过Attach API实现的。Attach API 可以跨 JVM 进程通讯的工具，能够将某种指令从一个 JVM 进程发送给另一个 JVM 进程。

加载agent只是Attach API发送的各种指令中的一种， 诸如jstack打印线程栈、jps列出Java进程、jmap做内存dump等功能，都属于Attach API可以发送的指令。

## 参考
* https://developer.aliyun.com/article/1004682
* https://www.cnblogs.com/rickiyang/p/11368932.html
