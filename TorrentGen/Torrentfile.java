package TorrentGen;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import DecodingFile.decode;
import EncodingFile.endcode;

public class Torrentfile {
    private static final int DEFAULT_PIECE_LENGTH = 262144; // 256KB

    /**
     * Generates a torrent file from a given file or directory. The file's path
     * and length are stored in the torrent file, and the file is broken up into
     * pieces of a given length. For each piece, a SHA-1 hash is generated and
     * stored in the torrent file. If the given file is a directory, the
     * directory's contents are recursively searched for files to include in the
     * torrent file.
     *
     * @throws NoSuchAlgorithmException
     *                                  if the SHA-1 algorithm is not available
     * @throws IOException
     *                                  if an I/O error occurs while generating the
     *                                  torrent file
     */
    public void TorrentFile() throws NoSuchAlgorithmException, IOException {
        File file = new File("C:\\Users\\Hello\\OneDrive\\Desktop\\chat_app");

        HashMap<String, Object> torrent = new HashMap<>();
        torrent.put("announce", "http://bttracker.debian.org:6969/announce");
        // torrent.put("creation date", System.currentTimeMillis() / 1000L);
        torrent.put("created by", "TorrentGen");
        torrent.put("encoding", "UTF-8");

        HashMap<String, Object> info = new HashMap<>();
        info.put("name", file.getName());

        if (!file.isDirectory()) {
            info.put("length", file.length());
            info.put("piece length", 256 * 1024);
            info.put("pieces", singlehashfile(file));
        } else {
            List<Map<String, Object>> files = new ArrayList<>();
            List<File> filesList = ListFilesRecursively(file);
            for (File f : filesList) {
                HashMap<String, Object> fileMap = new HashMap<>();
                fileMap.put("path", GetRelativePath(file, f));
                if (f != null) {
                    fileMap.put("length", f.length());
                }
                files.add(fileMap);
            }
            info.put("files", files);
            info.put("pieces", generatehashpieces(filesList));
        }
        torrent.put("info", info);
        
        endcode encoder = new endcode();
        byte[] encoded = encoder.encode(torrent);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file.getName() + ".torrent")) {
            fileOutputStream.write(encoded);
            fileOutputStream.close();
        } catch (IOException e) {
            Throwable t = new Throwable("Failed to write torrent file");
            t.printStackTrace();
        }
        decode decode = new decode(encoded);
        System.out.println(decode.Decode());
        System.out.println(torrent);
    }

    /**
     * Computes the relative path from a root directory to a given file.
     * If the file is not a descendant of the root directory, an error is printed.
     * 
     * @param root the root directory from which the relative path is calculated
     * @param file the file for which the relative path is needed
     * @return a List of Strings representing the relative path components
     */

     private Object GetRelativePath(File root, File file) {
        Path basepath = root.toPath().toAbsolutePath().normalize();
        Path filepath = file.toPath().toAbsolutePath().normalize();
        if (!filepath.startsWith(basepath)) {
            Throwable t = new Throwable(file+": "+"File is not a child of root directory: "+" "+root);
            t.printStackTrace();
        }
        Path relativePath = basepath.relativize(filepath);
        List<String> pathList = new ArrayList<>();
        for (Path path : relativePath) {
            pathList.add(path.toString());
        }
        return pathList;
    }
    /*for seeing bytes hashes */
    // public static String bytesToHex(byte[] bytes) {
    //     StringBuilder sb = new StringBuilder();
    //     for (byte b : bytes) {
    //         sb.append(String.format("%02x", b));
    //     }
    //     return sb.toString();
    // }

    private List<File> ListFilesRecursively(File file) throws IOException {
        Stack<File> queue = new Stack<>();
        queue.add(file);
        List<File> listFiles = new ArrayList<>();

        while (!queue.isEmpty()) {
            File currentFile = queue.pop();
            if (currentFile.isDirectory()) {
                File[] children = currentFile.listFiles();
                if (children != null) {
                    // Sort files lexicographically
                    Arrays.sort(children, (a, b) -> a.getName().compareTo(b.getName()));
                    // Add to stack in reverse order for depth-first search
                    for (int i = children.length - 1; i >= 0; i--) {
                        queue.add(children[i]);
                    }
                } else {
                    System.err.println("Skipping unreadable directory: " + currentFile);
                }
            } else {
                listFiles.add(currentFile);
            }
        }
        return listFiles;
    }

    public byte[] singlehashfile(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[DEFAULT_PIECE_LENGTH];
            int bytepostion = 0;
            int byteread = 0;
            while ((byteread = inputStream.read(buffer, bytepostion, buffer.length - bytepostion)) != -1) {
                bytepostion += byteread;
                if (bytepostion == buffer.length) {
                    md.update(buffer, 0, bytepostion);
                    baos.write(md.digest());
                    md.reset();
                    bytepostion = 0;
                }
            }
            if (bytepostion > 0) {
                md.update(buffer, 0, bytepostion);
                baos.write(md.digest());
            }
        } catch (IOException e) {
            throw new IOException("Failed to read file: " + file.getPath(), e.getCause());
        }
        return baos.toByteArray();
    }

    public byte[] generatehashpieces(List<File> files) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        int bytepostion = 0;
        byte[] buffer = new byte[DEFAULT_PIECE_LENGTH];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (File f : files) {
            try (InputStream inputStream = new BufferedInputStream(new FileInputStream(f))) {
                int byteread = 0;
                while ((byteread = inputStream.read(buffer, bytepostion, buffer.length - bytepostion)) != -1) {
                    bytepostion += byteread;
                    System.out.println(buffer.length);
                    if (bytepostion == buffer.length) {
                        md.update(buffer, 0, bytepostion);
                        baos.write(md.digest()); // <== FIXED: digest() with no arguments
                        md.reset();
                        bytepostion = 0;
                    }
                }
            } catch (IOException e) {
                throw new IOException("Failed to read file: " + f.getPath(), e.getCause());
            }
        }
        if (bytepostion > 0) {
            md.update(buffer, 0, bytepostion);
            try {
                baos.write(md.digest()); // <== FIXED
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return baos.toByteArray();
    }

    /**
     * Optional
     * Returns a list of files in a directory sorted lexicographically
     * 
     * @param args
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    // private List<File> getSortedFiles(File directory) throws IOException {
    // List<File> files = new ArrayList<>();
    // Files.walk(directory.toPath())
    // .filter(p -> !Files.isDirectory(p))
    // .sorted()
    // .forEach(p -> files.add(p.toFile()));
    // return files;
    // }

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        new Torrentfile().TorrentFile();
    }
}
