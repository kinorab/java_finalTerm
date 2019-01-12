package file.fileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileUtil{
    private static File[] emptyFile = {};
    public static Scanner sc = new Scanner(System.in);
    
    /**
     * Find files with matches fileName and filter
     * 
     * @param fileName             target file name
     * @param dirFilter            where dir to start finding(blank is root
     *                             directory)
     * @param isShowAllSubDirFiles show all files in subDir or not
     * @param isNameStrictMatches  if not is use (.*)fileName(.*).ext
     * @return array of found files, if empty return 0
     */
    public static File[] findFileMatches(String fileName, String dirFilter, boolean isShowAllSubDirFiles,
            boolean isNameStrictMatches) {
        // add filter to dir path
        File dir = new File(dirFilter.isEmpty() ? "." : dirFilter);
        if (!dir.exists())
            return emptyFile;
        ArrayList<File> temp = new ArrayList<>();
        ArrayList<File> files = new ArrayList<>();
        findfileMatchesHelper(dir, temp, isShowAllSubDirFiles);
        for (File file : temp) {
            String pathStr = file.getPath();
            String str = pathStr.substring(pathStr.lastIndexOf("\\") + 1);
            String checkStr;
            int lastIndex = fileName.lastIndexOf(".");
            if (isNameStrictMatches) {
                checkStr = fileName;
            } else if (lastIndex == -1) {
                checkStr = "(.*)" + fileName + "(.*)";
            } else {
                checkStr = "(.*)" + fileName.substring(0, lastIndex) + "(.*)"
                        + fileName.substring(lastIndex, fileName.length());
            }
            if (str.matches("^" + checkStr + "$")) {
                // System.out.println();
                files.add(new File(pathStr.replaceFirst("^\\.\\\\", "")));
            }
        }
        return files.toArray(new File[0]);
    }

    /**
     * Find dirs with matches dirName and filter
     * 
     * @param dirName             target dir name
     * @param dirFilter           where dir to start finding(blank is root
     *                            directory)
     * @param isShowAllSubDirs    show all dirs in subDir or not
     * @param isNameStrictMatches if not is use (.*)dirName(.*)
     * @return array of found dirs, if empty return 0
     */
    public static File[] findDirMatches(String dirName, String dirFilter, boolean isShowAllSubDirs,
            boolean isNameStrictMatches) {
        // add filter to dir path
        File dir = new File(dirFilter.isEmpty() ? "." : dirFilter);
        if (!dir.exists())
            return emptyFile;
        ArrayList<File> temp = new ArrayList<>();
        ArrayList<File> dirs = new ArrayList<>();
        findDirMatchesHelper(dir, temp, isShowAllSubDirs);
        for (File directory : temp) {
            String str = directory.toPath().toString();
            String checkStr;
            if (isNameStrictMatches) {
                checkStr = dirName;
            } else {
                checkStr = "(.*)" + dirName + "(.*)";
            }
            if (str.matches(checkStr)) {
                dirs.add(directory);
            }
        }
        return dirs.toArray(new File[0]);
    }

    /**
     * Save an array of files to certain path
     * 
     * @param files   array of files
     * @param path    target path to place
     * @param filters to filt some path without move
     */
    public static void saveFilesToPath(File[] files, String path, String ...otherFilters) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            path = path.replaceAll("\\\\", "/");
            for (int i = 0, num = 1; i < files.length; ++i) {
                String fileCurrentPath = files[i].getPath().replace(files[i].getName(), "").replaceAll("\\\\", "/");
                if(fileCurrentPath.equals(path)){
                    continue;
                }
                for(String filter : otherFilters){
                    if (fileCurrentPath.equals(filter)) {
                        continue;
                    }
                }
                File targetFile = dir.toPath().resolve(files[i].getName()).toFile();
                while (true) {
                    if (targetFile.exists()) {
                        String fileName = targetFile.getName().replaceAll("(\\(.*\\))+", "");
                        if (fileName.indexOf(".") != -1) {
                            targetFile = targetFile.toPath()
                                    .resolveSibling(fileName.substring(0, fileName.indexOf(".")) + "("
                                            + String.valueOf(num) + ")" + fileName.substring(fileName.indexOf(".")))
                                    .toFile();
                        } else {
                            targetFile = targetFile.toPath().resolveSibling(fileName + "(" + String.valueOf(num) + ")")
                                    .toFile();
                        }
                        num++;
                        continue;
                    }
                    Files.move(files[i].toPath(), targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
                    break;
                }

            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            System.out.println(ioe.getStackTrace());
        }
    }

    /**
     * Sort backups in date sort
     * 
     * @param files         backup files
     * @param isLatestFirst is lastest element place in front index
     * @return an array of backups
     */
    public static File[] sortBackups(File[] backups, boolean isLatestFirst) {
        Arrays.sort(backups, (File left, File right) -> {
            return isLatestFirst
                    ? right.getName().replaceAll("([^0-9]*)backup(.*)", "")
                            .compareTo(left.getName().replaceAll("([^0-9]*)backup(.*)", ""))
                    : left.getName().replaceAll("([^0-9]*)backup(.*)", "")
                            .compareTo(right.getName().replaceAll("([^0-9]*)backup(.*)", ""));
        });
        return backups;
    }

    /**
     * Sort the files(or dir) by subber file(or dir) in same parent dir
     * 
     * @param files      file name or dir name
     * @param isSubFirst is subbest element place front of parent dir
     * @return an array of sorted files
     */
    public static File[] sortFileByCompare(File[] files, boolean isSubFirst){
        Arrays.sort(files, (File left, File right) -> {
            return isSubFirst
                    ? right.getPath().compareTo(left.getPath())
                    : left.getPath().compareTo(right.getPath());
        });
        return files;
    }
    /**
     * Get the time stamp
     * 
     * @return the string of timestamp
     */
    public static String getTimeStamp() {
        Date date = new Date();
        Timestamp ts = new Timestamp(date.getTime());
        return String.valueOf(ts.getTime());
    }

    /**
     * Get the time from string which include time stamp
     * 
     * @param timeStampStringName
     * @return the time date format MM/dd/yyyy HH:mm:ss
     */
    public static String timeStampToString(String timeStampStringName) {
        String numberFront = timeStampStringName.replaceAll("[^0-9]+", "");
        if (numberFront.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date date = new Date(Long.parseLong(numberFront));
            timeStampStringName = sdf.format(date);
            return timeStampStringName;
        } catch (NumberFormatException nfe) {
            System.out.println(nfe.getMessage());
        }
        return "";
    }

    /**
     * Help to find and match files
     * 
     * @param dir                  dir position to start
     * @param container            container to contain files
     * @param isShowAllSubDirFiles fetch all subDir files to container or not
     */
    private static void findfileMatchesHelper(File dir, ArrayList<File> container, boolean isShowAllSubDirFiles) {
        File[] list = dir.listFiles();
        if (list.length == 0)
            return;
        for (File d : list) {
            if (d.isFile()) {
                container.add(d);
            } else if (isShowAllSubDirFiles) {
                findfileMatchesHelper(d, container, isShowAllSubDirFiles);
            }
        }
    }

    /**
     * Help to find and match dirs
     * 
     * @param dir              dir position to start
     * @param container        container to contain dirs
     * @param isShowAllSubDirs fetch all subDir dirs to container or not
     */
    private static void findDirMatchesHelper(File dir, ArrayList<File> container, boolean isShowAllSubDirs) {
        File[] list = dir.listFiles();
        if (list.length == 0)
            return;
        for (File d : list) {
            if (d.isDirectory()) {
                container.add(d);
                if (isShowAllSubDirs) {
                    findDirMatchesHelper(d, container, isShowAllSubDirs);
                }
            }
        }
    }
}