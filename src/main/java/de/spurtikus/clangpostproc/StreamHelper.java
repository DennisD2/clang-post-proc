package de.spurtikus.clangpostproc;

import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@Slf4j
public class StreamHelper {

    static OutputStream getOutputStream(String outFileName) {
        OutputStream ostream = null;
        try {
            ostream = new FileOutputStream(outFileName);
        } catch (FileNotFoundException e) {
            log.error("Cannot write file: {}", outFileName, e);
        }
        return ostream;
    }

    static void closeStream(OutputStream ostream) {
        try {
            ostream.flush();
            ostream.close();
        } catch (IOException e) {
            log.error("Cannot flush or close file", e);
        }
    }

    public static void write(OutputStream outputStream, StringBuilder sb) throws IOException {
        outputStream.write(sb.toString().getBytes());
    }
}
