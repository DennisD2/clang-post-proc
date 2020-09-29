package de.spurtikus.clangpostproc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StreamHelper {

    static OutputStream getOutputStream(String outFileName) {
        OutputStream ostream = null;
        try {
            ostream = new FileOutputStream(outFileName);
        } catch (FileNotFoundException e) {
            System.out.println("Cannot write file");
            e.printStackTrace();
        }
        return ostream;
    }

    static void closeStream(OutputStream ostream) throws IOException {
        ostream.flush();
        ostream.close();
    }

    public static void write(OutputStream outputStream, StringBuilder sb) throws IOException {
        outputStream.write(sb.toString().getBytes());
    }
}
