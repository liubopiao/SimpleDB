package example;

public class test {
    public static void main(String[] args) {
        long maxMemory = Runtime.getRuntime().maxMemory();
        System.out.println("最大堆内存: " + (maxMemory / (1024 * 1024)) + " MB");
    }


}
