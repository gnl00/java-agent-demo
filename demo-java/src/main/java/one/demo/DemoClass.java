package one.demo;

import java.util.concurrent.TimeUnit;

public class DemoClass {
    public static void main(String[] args) {
        new DemoClass().timer();
    }

    public void timer() {
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(2);
                System.out.println("current time=" + System.currentTimeMillis());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
