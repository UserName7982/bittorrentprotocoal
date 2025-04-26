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
        File file = new File("C:\\Users\\Hello\\OneDrive\\Pictures\\Anime characters\\Hunter x Hunter");

        HashMap<String, Object> torrent = new HashMap<>();
        torrent.put("announce",
                        "http://192.168.1.6:6969/announce");
        // torrent.put("creation date", System.currentTimeMillis() / 1000L);
        torrent.put("created by", "TorrentGen");
        torrent.put("encoding", "UTF-8");

        HashMap<String, Object> info = new HashMap<>();
        info.put("name", file.getName());

        if (!file.isDirectory()) {
            info.put("length", file.length());
            info.put("piece length",DEFAULT_PIECE_LENGTH);
            System.out.println(singlehashfile(file).length);
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
            byte[] pieces=generatehashpieces(filesList);
            info.put("pieces", pieces);
            System.out.println(pieces.length);
            info.put("piece length", DEFAULT_PIECE_LENGTH);
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
        System.out.println(new String(encoded));
        // decode decode = new decode(encoded);
        // System.out.println(decode.Decode());
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
            Throwable t = new Throwable(file + ": " + "File is not a child of root directory: " + " " + root);
            t.printStackTrace();
        }
        Path relativePath = basepath.relativize(filepath);
        List<String> pathList = new ArrayList<>();
        for (Path path : relativePath) {
            pathList.add(path.toString());
        }
        return pathList;
    }
    /* for seeing bytes hashes */
    // public static String bytesToHex(byte[] bytes) {
    // StringBuilder sb = new StringBuilder();
    // for (byte b : bytes) {
    // sb.append(String.format("%02x", b));
    // }
    // return sb.toString();
    // }

    private List<File> ListFilesRecursively(File file) throws IOException {
        Stack<File> stack = new Stack<>();
        stack.push(file);
        List<File> listFiles = new ArrayList<>();
    
        while (!stack.isEmpty()) {
            File current = stack.pop();
            if (current.isDirectory()) {
                File[] children = current.listFiles();
                if (children != null) {
                    // Sort files lexicographically
                    Arrays.sort(children, (a, b) -> a.getName().compareTo(b.getName()));
                    // Push to stack in reverse order to process in lex order
                    for (int i = children.length - 1; i >= 0; i--) {
                        stack.push(children[i]);
                    }
                }
            } else {
                listFiles.add(current);
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
        ByteArrayOutputStream piecesHash = new ByteArrayOutputStream();
        byte[] buffer = new byte[DEFAULT_PIECE_LENGTH];
        int bufferPosition = 0;
        MessageDigest md = MessageDigest.getInstance("SHA-1");

        for (File file : files) {
            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                int bytesRead;
                while ((bytesRead = in.read(buffer, bufferPosition, buffer.length - bufferPosition)) != -1) {
                    bufferPosition += bytesRead;
                    if (bufferPosition == DEFAULT_PIECE_LENGTH) {
                        md.update(buffer, 0, bufferPosition);
                        piecesHash.write(md.digest());
                        md.reset();
                        bufferPosition = 0;
                    }
                }
            }
        }
    
        // Handle the final partial piece
        if (bufferPosition > 0) {
            md.update(buffer, 0, bufferPosition);
            piecesHash.write(md.digest());
        }
    
        return piecesHash.toByteArray();
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
