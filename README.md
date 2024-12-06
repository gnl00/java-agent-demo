# java-agent-demo
> 简言：使用 Java 字节码修改技术（javassist/ASM）+ JVMTI 保留的接口来实现 Java 字节码的动态修改。

> 头铁试了 javassist + JDK21，未支持。顺手查了一下 javassist 好像对 JDK17 以上的支持还不完整。

> *你发任你发，我用 Java 8<sub>tm</sub>*

## 了解 Java instrument

`java.lang.instrument` 底层实现依赖于 JVMTI(JVM Tool Interface，提供的 API 接口)，允许 Java 程序在运行时动态地修改字节码。

JVMTI 是基于事件驱动的，JVM每执行到一定的逻辑就会调用对应事件的回调接口。

```java
public interface Instrumentation {
    /**
     * 类的字节码修改称为类转换(Class Transform)，
     * 类转换其实最终都回归到类重定义Instrumentation#redefineClasses()方法
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

## Java agent 的使用

先把 java agent 打包成带 java-assistant 依赖的 fat jar。

### 针对 jar 包

`java -javaagent:agent-demo-1.0-SNAPSHOT.jar -jar spring-app.jar`

### 针对单个可执行类

```shell
javac Hello.java

java -javaagent:agent-demo-1.0-SNAPSHOT.jar Hello
# 如果 Hello 在 com.demo 包下，需要加上包名
java -javaagent:agent-demo-1.0-SNAPSHOT.jar com.demo.Hello
```

### 使用 IDE

如果使用 idea，可以在启动 vm 参数中加入 `-javaagent:agent-demo-1.0-SNAPSHOT.jar` 启动（非 debug 模式）。

## Attach API

Java agent可以在JVM启动后再加载，就是通过Attach API实现的。Attach API 可以跨 JVM 进程通讯的工具，能够将某种指令从一个 JVM 进程发送给另一个 JVM 进程。

加载agent只是Attach API发送的各种指令中的一种， 诸如jstack打印线程栈、jps列出Java进程、jmap做内存dump等功能，都属于Attach API可以发送的指令。

### Attach API 的使用
> `com.sun.tools.attach` 需要手动导入 `your-JDK-path/lib/tools.jar`

Attach API 很简单，只有 2 个主要的类，都在 com.sun.tools.attach 包里面：
* VirtualMachine 字面意义表示一个Java 虚拟机，也就是程序需要监控的目标虚拟机，提供了获取系统信息(比如获取内存dump、线程dump，类信息统计(比如已加载的类以及实例个数等)， loadAgent，Attach 和 Detach。 该类允许通过 attach 方法传入一个 jvm 的 pid 远程连接到 jvm 上。

  通过 loadAgent 方法向 jvm 注册一个代理程序 agent，在该 agent 的代理程序中会得到一个 Instrumentation 实例，该实例可以 在 class 加载前改变 class 的字节码，也可以在 class 加载后重新加载。在调用 Instrumentation 实例的方法时，这些方法会使用 ClassFileTransformer 接口中提供的方法进行处理。
* VirtualMachineDescriptor 是一个描述虚拟机的容器类，配合 VirtualMachine 类完成各种功能

由于是进程间通讯，那代表着使用Attach API的程序需要是一个独立的Java程序，通过attach目标进程，与其进行通讯。
下面的代码表示了向进程pid为1234的JVM发起通讯，加载一个名为agent.jar的Java agent。

通过VirtualMachine类的attach(pid)方法，便可以attach到一个运行中的java进程上，之后便可以通过loadAgent(agentJarPath)来将agent的jar包注入到对应的进程，然后对应的进程会调用agentmain方法。

```java
// VirtualMachine 等相关类位于 JDK 的 tools.jar
VirtualMachine vm = VirtualMachine.attach("1234");  // 1234表示目标JVM进程pid
try {
    vm.loadAgent("/path/to/agent.jar");    // 指定agent的jar包路径，发送给目标进程
} finally {
    vm.detach();
}
```

vm.loadAgent之后，相应的agent就会被目标JVM进程加载，并执行agentmain方法。

## java agent 限制

1、新类和老类的父类必须相同；

2、新类和老类实现的接口数也要相同，并且是相同的接口；

3、新类和老类访问符必须一致。 新类和老类字段数和字段名要一致；

4、新类和老类新增或删除的方法必须是 private static/final 修饰的；

5、只能修改类中的方法体，不能修改方法参数个数、类型

> 除了上面的方式，如果想要重新定义一个类，可以考虑使用自定义类加载器的方式：创建一个自定义类加载器，通过加载字节码创建一个全新的类。不过也存在只能通过反射调用该类的局限性。

## 参考

* https://developer.aliyun.com/article/1004682
* https://www.cnblogs.com/rickiyang/p/11368932.html
* https://www.javassist.org/tutorial/tutorial.html
* https://y4er.com/posts/javassist-learn/ javassist 学习
