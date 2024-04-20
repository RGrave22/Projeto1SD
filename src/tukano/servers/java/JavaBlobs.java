package tukano.servers.java;

import tukano.api.java.Blobs;
import tukano.api.java.Result;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;


public class JavaBlobs implements Blobs {


    private static final String FILE_PATH = "%s/%s";
    private static final String DIR_PATH = System.getProperty("user.dir");


    @Override
    public Result<Void> upload(String blobId, byte[] bytes) {

        if (!blobId.startsWith("SH"))
            return Result.error(Result.ErrorCode.FORBIDDEN);

        Path filePath = Paths.get(String.format(FILE_PATH, DIR_PATH, blobId));

        if(!Files.exists(filePath)) {
            try {
                Files.createFile(filePath);
                Files.write(filePath, bytes);
                return Result.ok();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                byte[] existingBytes = Files.readAllBytes(filePath);
                if(!Arrays.equals(existingBytes, bytes)) {
                    return Result.error(Result.ErrorCode.CONFLICT);
                }
                else {
                    return Result.ok();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Result<byte[]> download(String blobId) {

        Path filePath = Paths.get(String.format(FILE_PATH, DIR_PATH, blobId));

        if (Files.exists(filePath)) {
            byte[] existingBytes = null;
            try {
                existingBytes = Files.readAllBytes(filePath);
                return Result.ok(existingBytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else {
            return Result.error(Result.ErrorCode.NOT_FOUND);
        }
    }

    @Override
    public Result<Void> delete(String blobId) {

        Path filePath = Paths.get(String.format(FILE_PATH, DIR_PATH, blobId));

        try {
            Files.delete(filePath);
            return Result.ok();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result<Integer> getNumFiles() {
        Path folderPath = Paths.get(System.getProperty("user.dir"));

        File folder = new File(String.valueOf(folderPath));

        Path path = Paths.get(String.valueOf(folderPath));
        int count = 0;

        if(!Files.exists(path)) {
            return Result.ok(count);
        }

        File[] files = folder.listFiles();

        for (File file : files) {
            if (file.isFile()) {
                count++;
            }
        }
        return Result.ok(count);
    }
}
