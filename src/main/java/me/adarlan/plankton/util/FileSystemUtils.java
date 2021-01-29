package me.adarlan.plankton.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.bash.BashScriptFailedException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileSystemUtils {

    public static void createDirectory(String path) {
        try {
            BashScript.run("mkdir -p " + path);
        } catch (BashScriptFailedException e) {
            throw new FileSystemException("Unable to create directory " + path, e);
        }
    }

    public static void copyFile(String fromPath, String toPath) {
        try {
            BashScript.run("cp " + fromPath + " " + toPath);
        } catch (BashScriptFailedException e) {
            throw new FileSystemException("Unable to copy file from " + fromPath + " to " + toPath, e);
        }
    }

    public static void copyDirectoryContent(String fromPath, String toPath) {
        try {
            BashScript.run("cp -R " + fromPath + "/. " + toPath + "/");
        } catch (BashScriptFailedException e) {
            throw new FileSystemException("Unable to copy directory content from " + fromPath + " to " + toPath, e);
        }
    }

    public static String relativePath(String fromPath, String toPath) {
        return toPath.substring(fromPath.length() + 1);
    }

    public static void writeFile(String filePath, Iterable<String> strings) {
        File file = new File(filePath);
        writeFile(file, strings);
    }

    public static void writeFile(File file, Iterable<String> strings) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file);) {
            writeFile(fileOutputStream, strings);
        } catch (IOException e) {
            throw new FileSystemException("Unable to write file", e);
        }
    }

    public static void writeFile(FileOutputStream fileOutputStream, Iterable<String> strings) {
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);) {
            writeFile(outputStreamWriter, strings);
        } catch (IOException e) {
            throw new FileSystemException("Unable to write file with FileOutputStream", e);
        }
    }

    public static void writeFile(OutputStreamWriter outputStreamWriter, Iterable<String> strings) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);) {
            writeFile(bufferedWriter, strings);
        } catch (IOException e) {
            throw new FileSystemException("Unable to write file with OutputStreamWriter", e);
        }
    }

    public static void writeFile(BufferedWriter bufferedWriter, Iterable<String> strings) {
        strings.forEach(string -> {
            try {
                bufferedWriter.write(string);
                bufferedWriter.newLine();
            } catch (IOException e) {
                throw new FileSystemException("Unable to write file with BufferedWriter", e);
            }
        });
    }

}
