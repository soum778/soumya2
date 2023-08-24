package util;

import java.io.*;
import java.util.*;

/**
 * this class become somehow useless after many update of the project
 */
public class Converter {
    /**
     * load all url in the file into a list
     * @param path path to the txt file
     * @return a queue contain all url
     */
    public static Queue<String> readRemainingQueue(String path) {
        Queue<String> queue = new LinkedList<>();
        File file = new File(path);
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()){
                queue.add(sc.nextLine());
            }
        } catch (FileNotFoundException e) { e.printStackTrace(); }
        return queue;
    }

    public static byte[] idTobyteArray(int id) {
        return String.valueOf(id).getBytes();
    }

}
