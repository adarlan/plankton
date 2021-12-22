package plankton.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileSystemUtils {

    public static void writeFile(String filePath, Iterable<String> strings) {
        File file = new File(filePath);
        writeFile(file, strings);
    }

    public static void writeFile(File file, Iterable<String> strings) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file);) {
            writeFile(fileOutputStream, strings);
        } catch (IOException e) {
            throw new FileSystemUtilsException("Unable to write file", e);
        }
    }

    public static void writeFile(FileOutputStream fileOutputStream, Iterable<String> strings) {
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);) {
            writeFile(outputStreamWriter, strings);
        } catch (IOException e) {
            throw new FileSystemUtilsException("Unable to write file with FileOutputStream", e);
        }
    }

    public static void writeFile(OutputStreamWriter outputStreamWriter, Iterable<String> strings) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);) {
            writeFile(bufferedWriter, strings);
        } catch (IOException e) {
            throw new FileSystemUtilsException("Unable to write file with OutputStreamWriter", e);
        }
    }

    public static void writeFile(BufferedWriter bufferedWriter, Iterable<String> strings) {
        strings.forEach(string -> {
            try {
                bufferedWriter.write(string);
                bufferedWriter.newLine();
            } catch (IOException e) {
                throw new FileSystemUtilsException("Unable to write file with BufferedWriter", e);
            }
        });
    }
}
