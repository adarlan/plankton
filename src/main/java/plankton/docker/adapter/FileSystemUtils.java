package plankton.docker.adapter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class FileSystemUtils {

    static void writeFile(String filePath, Iterable<String> strings) throws IOException {
        File file = new File(filePath);
        writeFile(file, strings);
    }

    static void writeFile(File file, Iterable<String> strings) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file);) {
            writeFile(fileOutputStream, strings);
        }
    }

    static void writeFile(FileOutputStream fileOutputStream, Iterable<String> strings) throws IOException {
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);) {
            writeFile(outputStreamWriter, strings);
        }
    }

    static void writeFile(OutputStreamWriter outputStreamWriter, Iterable<String> strings) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);) {
            writeFile(bufferedWriter, strings);
        }
    }

    static void writeFile(BufferedWriter bufferedWriter, Iterable<String> strings) throws IOException {
        for (String string : strings) {
            bufferedWriter.write(string);
            bufferedWriter.newLine();
        }
    }
}
