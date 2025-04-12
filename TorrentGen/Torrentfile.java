package TorrentGen;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Torrentfile {

    public void TorrentFile() {
        File file = new File("C:\\Users\\Hello\\OneDrive\\Desktop\\Advanced java");
       
        HashMap<String, Object> torrent = new HashMap<>();
        torrent.put("announcer", "http://bttracker.debian.org:6969/announce");
        torrent.put("creation date", System.currentTimeMillis() / 1000L);
        torrent.put("created by", "TorrentGen");
        torrent.put("encoding in", "UTF-8");

        HashMap<String, Object> info = new HashMap<>();
        info.put("name", file.getName());
        
        if(!file.isDirectory()){
            System.out.println("notDirectory");
            info.put("length", file.length());
            info.put("piece length", 256 * 1024);
            
        }else{
            System.out.println("Directory");
            List<Map<String, Object>> files = new ArrayList<>();
            List<File> filesList = ListFilesRecursively(file);
            for (File f : filesList) {
                HashMap<String, Object> fileMap = new HashMap<>();
                fileMap.put("path", GetRelativePath(file, f));
                fileMap.put("length", f.length());
                files.add(fileMap);
            }
            info.put("files", files);
        }
        torrent.put("info", info);
    }

    private Object GetRelativePath(File root, File file) {
        Path basepath = root.toPath().toAbsolutePath().normalize();
        Path filepath = file.toPath().toAbsolutePath().normalize();
        if (!filepath.startsWith(basepath)) {
            Throwable t = new Throwable("File is not a child of root directory");
            t.printStackTrace();
        }
        Path relativePath = basepath.relativize(filepath);
        List<String> pathList = new ArrayList<>();
        for (Path path : relativePath) {
            pathList.add(path.toString());
        }
        return pathList;
    }

    private List<File> ListFilesRecursively(File file) {
        Stack<File> queue = new Stack<>();
        queue.add(file);
        List<File> listFiles = new ArrayList<>();
        while (!queue.isEmpty()) {
            File currentFile = queue.pop();
            if (currentFile.isDirectory()) {
                if (currentFile.listFiles() != null) {
                    File[] files = currentFile.listFiles();
                    Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
                    for (int i = files.length - 1; i >= 0; i--) {
                        queue.add(files[i]);
                    }
                }
            } else {
                listFiles.add(currentFile);
            }
        }
        return listFiles;
    }
    public static void main(String[] args) {
        new Torrentfile().TorrentFile();
    }
}
