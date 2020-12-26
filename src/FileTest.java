import model.FingerprintLib;

import java.io.File;
import java.io.IOException;

public class FileTest {
    public static void main(String[] args) throws IOException, InterruptedException {

        File musicFolder = new File("music");
        File[] files = musicFolder.listFiles();

        for (File f : files) {
            String name = f.getName();

            if (name.endsWith(".wav")) {
                System.out.println("============== PROCESSING " + name + "========================");
                FingerprintLib.saveSongFingerPrint("music/" + name, "prints");
            } else {
                System.out.println("Skipping file " + name);
            }

            System.out.println(name);
        }



    }
}
