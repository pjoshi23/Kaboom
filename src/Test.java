import model.AudioReader;
import java.io.ByteArrayOutputStream;

public class Test {
    public static void main(String[] args) throws InterruptedException {
        AudioReader reader = AudioReader.getMicStream(5000);

        ByteArrayOutputStream raw = reader.getOutputStream();

        Thread.sleep(100);
        raw.reset();

        while (reader.isRunning()) {
            byte[] data = raw.toByteArray();

            Thread.sleep(100);

            for (int i = 0; i < data.length; i++) {
                System.out.print(data[i] + " ");
            }
        }

    }
}
