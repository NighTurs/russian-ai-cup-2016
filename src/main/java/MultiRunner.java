import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiRunner {

    private static final int DEFAULT_PORT = 31001;

    public static void main(String[] args) throws InterruptedException {
        int strategiesToRun = Integer.parseInt(args[0]);
        ExecutorService executor = Executors.newFixedThreadPool(strategiesToRun);
        for (int i = DEFAULT_PORT; i < DEFAULT_PORT + strategiesToRun; i++) {
            executor.execute(new RunnerRunnable(i));
            Thread.sleep(1000);
        }
    }

    private static class RunnerRunnable implements Runnable {
        private final int port;

        public RunnerRunnable(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                new Runner(new String[]{"127.0.0.1", Integer.toString(port), "0000000000000000"}).run();
            } catch (IOException e) {
                throw new RuntimeException("Failed to start on port " + port, e);
            }
        }
    }
}
