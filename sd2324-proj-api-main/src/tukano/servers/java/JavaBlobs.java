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


//    private static Logger Log = Logger.getLogger(JavaBlobs.class.getName());

    @Override
    public Result<Void> upload(String blobId, byte[] bytes) {

        if (!blobId.startsWith("SH"))
            return Result.error(Result.ErrorCode.FORBIDDEN);

        Path filePath = Paths.get(String.format(FILE_PATH, DIR_PATH, blobId));


//        Log.info("UPLOAD: " + filePath);

        if(!Files.exists(filePath)) {
//            Log.info("FILE NOT EXISTS : " + filePath);
            try {
                Files.createFile(filePath);
                Files.write(filePath, bytes);
//                Log.info("FILE WRITTEN : " + filePath);
                return Result.ok();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
//            Log.info("FILE EXISTS : " + filePath);
            try {
                byte[] existingBytes = Files.readAllBytes(filePath);
                if(!Arrays.equals(existingBytes, bytes)) {
//                    Log.info("CONFLICT : " + filePath);
                    return Result.error(Result.ErrorCode.CONFLICT);
                }
                else {
//                    Log.info("WITHOUT CONFLICT : " + filePath);
                    return Result.ok();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Result<byte[]> download(String blobId) {
//        Log.info("DOWNLOAD: " + blobId);

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

//        Log.info("DELETE : " + filePath);

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

//        Log.info("FOLDER_PATH : " + folderPath);

        File folder = new File(String.valueOf(folderPath));

        Path path = Paths.get(String.valueOf(folderPath));
        int count = 0;

        if(!Files.exists(path)) {
//            Log.info("FOLDER_PATH NOT EXISTS ");
            return Result.ok(count);
        }

        File[] files = folder.listFiles();

        for (File file : files) {
            if (file.isFile()) {
                count++;
            }
        }
//        Log.info("FOLDER_PATH EXISTS : " + count);
        return Result.ok(count);
    }
}
