import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class Main {
    public static final int QUANTITY = 10_000;
    public static BlockingQueue<String> repeatA = new ArrayBlockingQueue<>(100);
    public static BlockingQueue<String> repeatB = new ArrayBlockingQueue<>(100);
    public static BlockingQueue<String> repeatC = new ArrayBlockingQueue<>(100);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        new Thread(() -> {
            for (int i = 0; i < QUANTITY; i++) {
                String text = generateText("abc", 100_000);
                try {
                    repeatA.put(text);
                    repeatB.put(text);
                    repeatC.put(text);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        List<Future<String>> futures = new ArrayList<>();
        try (ExecutorService executorService = Executors.newFixedThreadPool(3)) {
            final Future<String> taskA = executorService.submit(myCallable('a'));
            futures.add(taskA);
            final Future<String> taskB = executorService.submit(myCallable('b'));
            futures.add(taskB);
            final Future<String> taskC = executorService.submit(myCallable('c'));
            futures.add(taskC);
            executorService.shutdown();
        }

        for (Future<String> fut : futures) {
            System.out.println(fut.get().substring(0, 100));
        }
    }

    public static String generateText(String letters, int length) {
        Random random = new Random();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < length; i++) {
            text.append(letters.charAt(random.nextInt(letters.length())));
        }
        return text.toString();
    }

    public static Callable<String> myCallable(char repeatedChars) {
        return () -> {
            String result = "";
            String originalString = "";
            int maxCount = 0;
            int originalCount;
            for (int i = 0; i < QUANTITY; i++) {
                originalString = switch (repeatedChars) {
                    case 'a' -> repeatA.take();
                    case 'b' -> repeatB.take();
                    case 'c' -> repeatC.take();
                    default -> originalString;
                };
                originalCount = originalString.length() - originalString.replaceAll(String.valueOf(repeatedChars), "").length();

                if (originalCount > maxCount) {
                    maxCount = originalCount;
                    result = originalString;
                }
            }
            return result;
        };
    }
}

