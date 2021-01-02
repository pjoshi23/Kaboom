import model.*;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class PerformMatch {
    private static final int WINDOW_SIZE = 4096;

    public static void main(String[] args) {
        HashMap<String, List<Match>> database = buildDatabase("prints/");
        performMatch(database);
    }

    private static void performMatch(HashMap<String, List<Match>> db) {
        HashMap<String, Integer> songHits = new HashMap<>();

        int totalHits = 0;

        ArrayList<Complex[]> fftFrames = new ArrayList<Complex[]>();
        ArrayList<int[]> keyFreqList = new ArrayList<>();

        AudioReader reader = AudioReader.getMicStream(5000);
        ByteArrayOutputStream out = reader.getOutputStream();

        System.out.println("Loading audio");
        while (!reader.isRunning()) {
            System.out.println("Waiting for audio stream...");
        }

        System.out.print("Reading audio");
        int currentIndex = 0;
        while (reader.isRunning()) {
            byte audioData[] = out.toByteArray();

            // Convert batch of audio data into FFT frames
            if (audioData.length > WINDOW_SIZE) {
                out.reset();
                FFT.performFFT(audioData, WINDOW_SIZE, fftFrames);
            }

            // Process results from fftFrames list
            // Need there to be at least 10 tenths of a second (1 second) of audio available
            if (fftFrames.size() >= 10) {
                Complex[] firstSample = fftFrames.remove(0);
                Complex[] nextSample = fftFrames.get(8);

                int[] keyFreq = FingerprintLib.getKeyFrequenciesFor(firstSample);
                int[] keyFreq1 = FingerprintLib.getKeyFrequenciesFor(nextSample);

                int[] mergedFingerprint = new int[keyFreq.length + 1];
                for (int i = 0; i < keyFreq.length; i++) {
                    mergedFingerprint[i] = keyFreq[i];
                }

                mergedFingerprint[2] = keyFreq1[0];
                keyFreqList.add(mergedFingerprint);
            }

            // build songHits list
            if (keyFreqList.size() > currentIndex) {
                int[] fingerPrint = keyFreqList.get(currentIndex);
                String key = Arrays.toString(fingerPrint);

                if (db.containsKey(key)) {
                    List<Match> list = db.get(key);

                    for (Match m : list) {
                        if (songHits.containsKey(m.getFileName())) {
                            int total = songHits.get(m.getFileName());
                            songHits.put(m.getFileName(), total + 1);
                        } else {
                            songHits.put(m.getFileName(), 1);
                        }
                    }

                    printResultsSoFar(songHits);
                }

                currentIndex++;
            }
        }
    }

    private static int gethashFor(int[] firstRow) {
        // TODO: get a hash for fingerprint

        return 0;
    }

    private static void printResultsSoFar(HashMap<String, Integer> songHits) {
        for (String song : songHits.keySet()) {
            int numHits = songHits.get(song);
            System.out.println(song + "\t :: " + numHits);
        }

        System.out.println("*******************************************************");
    }

    private static void recordMatch(HashMap<String, Integer> songHits, List<Match> currentMatchList) {
        for (Match m : currentMatchList) {
            if (songHits.containsKey(m.getFileName())) {
                int n = songHits.get(m.getFileName());
                songHits.put(m.getFileName(), n + 1);
            } else {
                songHits.put(m.getFileName(), 1);
            }
        }
    }

    private static HashMap<String, List<Match>> buildDatabase(String dataDir) {
        HashMap<String, List<Match>> db = new HashMap<>();

        // TODO: loop over .csv files in dataDir and use FingerprintLib to read fingerprints
        // TODO: load db with all fingerprints
        File printFolder = new File(dataDir);
        File[] files = printFolder.listFiles();
        for (File f : files) {
            List<int[]> fingerPrint = loadFingerprintsFrom(f);

            for (int i = 0; i < fingerPrint.size(); i++) {
                int[] fp = fingerPrint.get(i);
                String key = Arrays.toString(fp);

                Match m = new Match(f.getName(), i*10*10);

                if (db.containsKey(key)) {
                    List<Match> matches = db.get(key);
                    matches.add(m);
                } else {
                    List<Match> list = new ArrayList<>();
                    list.add(m);
                    db.put(key, list);

                }

            }
        }

        return db;
    }

    private static void loadDbWithPrints(HashMap<String, List<Match>> db, List<int[]> keyFreqList, String songName) {
        // TODO: helper method to load db with fingerprints
    }

    private static List<int[]> loadFingerprintsFrom(File f) {
        List<int[]> out = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {

            String line = br.readLine();
            while (line != null) {
                int[] datarow = getDataRow(line);
                out.add(datarow);
                line = br.readLine();
            }

        } catch (Exception errorObj) {
            System.err.println("There was a problem reading the file");
        }

        return out;
    }

    private static int[] getDataRow(String line) {
        String[] data = line.split(",");
        int[] out = new int[data.length];

        for (int i = 0; i < data.length; i++) {
            String val = data[i];
            out[i] = Integer.parseInt(val.trim());
        }
        return out;
    }

}