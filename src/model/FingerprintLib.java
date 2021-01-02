package model;

import com.sun.deploy.util.ArrayUtil;
import com.sun.tools.javac.util.ArrayUtils;

import java.io.*;
import java.util.ArrayList;

/***
 * This class contains methods for calculating "fingerprints" from audio output.  Generally, these fingerprints
 * involve identifying the loudest frequencies within pre-defined frequency ranges.
 *
 * It also contains methods for saving these fingerprints to csv files
 */
public class FingerprintLib {
    private static final String OUTPUT_DIR = "prints/";

    // Windows use to use in FFT.  With sample rate of 44.1 khz = 0.0928 seconds for window size.
    private static final int WINDOW_SIZE = 4096;

    public static void saveSongFingerPrint(String file) throws IOException, InterruptedException {
        saveSongFingerPrint(file, OUTPUT_DIR);
    }

    /***
     * Save int[] of loudest frequences in intervals defined in static RANGES array for each time window.  Output is
     * csv with one set of frequences per line.
     *
     * If input file is "dir1/dir2/file.wav" output will be "dir1/dir2/outputDir/file-fp.csv"
     * @param filePath path to the .wav file including filename and .wav extension.  eg "dir1/file.wav"
     * @param outputDir subdirectory inside file location (which must exist!) to save .csv files into
     * @throws InterruptedException
     * @throws IOException
     */
    public static void saveSongFingerPrint(String filePath, String outputDir) throws InterruptedException, IOException {
        AudioReader reader = AudioReader.getAudioStreamFor(filePath);
        byte[] b = reader.readAllData();
        Complex[][] fftFrames = FFT.performFFT(b, WINDOW_SIZE);
        System.out.println(fftFrames.length);

        ArrayList<int[]> allKeyFrequencies = new ArrayList<>();

        for (int i = 0; i < fftFrames.length - 10; i++) {
            int[] keyPoints = getKeyFrequenciesFor(fftFrames[i]);
            int[] nextKeyPoints = getKeyFrequenciesFor(fftFrames[i+10]);
            int[] mergedKeyPoints = new int[keyPoints.length + 1];

            for (int j = 0; j < keyPoints.length; j++) {
                mergedKeyPoints[j] = keyPoints[j];
            }

            mergedKeyPoints[2] = nextKeyPoints[0];
            allKeyFrequencies.add(mergedKeyPoints);
        }

        outputDir = slashify(outputDir);
        String fileName = getFileNameFor(filePath);
        String path = getPathFor(filePath);

        writeToFile(outputDir + replaceExtension(fileName,".wav", "-fp.csv"), allKeyFrequencies);
    }

    /***
     * Ensure correct format for directory string.  Convert backslashes to forward slashes.  Add trailing slash.
     * @param outputDirectory
     * @return
     */
    private static String slashify(String outputDirectory) {
        outputDirectory = outputDirectory.replace('\\', '/');
        if (!outputDirectory.endsWith("/")) outputDirectory += "/";   // add trailing slash
        return outputDirectory;
    }

    /***
     * Return path not including filename
     * @param filePath
     * @return
     */
    private static String getPathFor(String filePath) {
        filePath = filePath.replace('\\', '/');
        int lastDir = filePath.lastIndexOf("/");
        if (lastDir == -1) return "";
        return filePath.substring(0, lastDir+1);
    }

    /***
     * Return filename not including path
     * @param filePath
     * @return
     */
    private static String getFileNameFor(String filePath) {
        filePath = filePath.replace('\\', '/');
        int lastDir = filePath.lastIndexOf("/");
        return filePath.substring(lastDir+1);
    }

    /***
     * Replace file extension in name
     * @param file
     * @param oldExtension
     * @param newExtension
     * @return
     */
    private static String replaceExtension(String file, String oldExtension, String newExtension) {
        int index = file.indexOf(oldExtension);         // TODO: can probably be shorter with .replace
        if (index == -1) return file+newExtension;
        return file.substring(0,index)+newExtension;
    }

    private static String join(String delimeter, int[] list) {
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < list.length-1; i++) {
            b.append(list[i]);
            b.append(delimeter);
        }

        b.append(list[list.length-1]);
        return b.toString();
    }

    public static void writeToFile(String filePath, ArrayList<int[]> data) {
        try (FileWriter f = new FileWriter(filePath);
             BufferedWriter b = new BufferedWriter(f);
             PrintWriter writer = new PrintWriter(b);) {

            for (int[] freq : data) {
                writer.println( join(",", freq) );
            }

        } catch (Exception errorObj) {
            System.out.println("There was an error with the file");
            errorObj.printStackTrace();
        }
    }



    public static int[] getKeyFrequenciesFor(Complex[] results) {
        int[] ranges = {44, 120, 180};
        int[] keyFrequencies = new int[ranges.length - 1];


        for (int i = 0; i < ranges.length - 1; i++) {
            int lowIndex = ranges[i];
            int highIndex = ranges[i+1];

            int loudestFreq = getLoudestIndexBetween(lowIndex, highIndex, results);
            keyFrequencies[i] = loudestFreq;
        }
        return keyFrequencies;
    }


    private static int getLoudestIndexBetween(int lowIndex, int highIndex, Complex[] results) {
        double max = results[lowIndex].abs();
        int maxIndex = lowIndex;

        for (int i = lowIndex; i < highIndex; i++) {
            if (results[i].abs() > max) {
                max = results[i].abs();
                maxIndex = i;
            }
        }

        return maxIndex;
    }

}
