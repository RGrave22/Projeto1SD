package tukano.servers.java;

import tukano.api.java.Blobs;
import tukano.api.java.Result;

import java.io.IOException;

import java.nio.file.Files;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;


public class JavaBlobs implements Blobs {


    private static final String FILE_PATH = "%s\\BlobFiles\\%s";

    private static Logger Log = Logger.getLogger(JavaBlobs.class.getName());

    @Override
    public Result<Void> upload(String blobId, byte[] bytes) {
        if (!blobId.startsWith("SH"))
            return Result.error(Result.ErrorCode.FORBIDDEN);

        String projectPath = System.getProperty("user.dir");
        String filePath = String.format(FILE_PATH, projectPath, blobId);
        Log.info("UPLOAD: " + filePath);

        if (Files.exists(Paths.get(filePath))) {
            byte[] existingBytes = null;
            try {
                existingBytes = Files.readAllBytes(Paths.get(filePath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if(!Arrays.equals(existingBytes, bytes))
                return Result.error(Result.ErrorCode.CONFLICT);
            else {
                return Result.ok();
            }
        } else {
            try {
                Files.write(Path.of(filePath), bytes);
                return Result.ok(); // File created successfully
            } catch (IOException e) {
                // Handle IOException
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Result<byte[]> download(String blobId) {
        Log.info("DOWNLOAD: " + blobId);
    //    if (!blobId.startsWith("SH"))
    //        return Result.error(Result.ErrorCode.FORBIDDEN);

        String projectPath = System.getProperty("user.dir");
        String filePath = String.format(FILE_PATH, projectPath, blobId);

        if (Files.exists(Path.of(filePath))) {
            byte[] existingBytes = null;
            try {
                existingBytes = Files.readAllBytes(Path.of(filePath));
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
        String projectPath = System.getProperty("user.dir");
        String filePath = String.format(FILE_PATH, projectPath, blobId);
        Log.info("DELETE : " + filePath);

        try {
            Files.delete(Path.of(filePath));
            return Result.ok();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
