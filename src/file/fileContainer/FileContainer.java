package file.fileContainer;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import file.XmlFile;
import file.fileUtil.*;
import process.mode.*;
import setting.*;

public class FileContainer{
    private XmlFile userFile = null;
    private File setting = null;

    /**
     * Bulid object, automatically to fetch files and initialize
     * 
     * @param langFile language xml file
     */
    public FileContainer(XmlFile langFile){
        refetch(langFile);
    }

    /**
     * Refetch files and environment
     * 
     * @param langFile language xml file
     * 
     */
    public void refetch(XmlFile langFile){
        try {
            // use default path to initialize
            setting = new File(Setting.enterSettingDir + Setting.enterSettingName);
            // check setting initialize and set backup and user file
            checkSetting(langFile);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Execute to backup user file with encoding
     * 
     * @param langFile language xml file
     * @param isAuto   is auto backup or not
     */
    public void execBackup(XmlFile langFile, boolean isAuto) throws FileNotFoundException {
        File dir = new File(Setting.enterBackupDir);
        File backup = new File(Setting.enterBackupDir + FileUtil.getTimeStamp() + Setting.enterBackupName);
        Scanner userFileSc = new Scanner(new BufferedReader(
                new InputStreamReader(new FileInputStream(userFile.getPath()), StandardCharsets.UTF_8)));
        BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(backup), StandardCharsets.UTF_8));
        ArrayList<String> lines = new ArrayList<>();
        String errorHint = ((Element) XmlFile.navigate(langFile.getFileNode("backup"), "state")).getAttribute("error");
        String backupHint;
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (isAuto) {
            backupHint = XmlFile.navigate(langFile.getFileNode("backup"), "fix", "autoBackup").getTextContent();
        } else {
            backupHint = XmlFile.navigate(langFile.getFileNode("backup"), "fix", "manualBackup").getTextContent();
        }
        System.out.println(backupHint);
        try {
            backup.createNewFile();
            while (userFileSc.hasNextLine()) {
                lines.add(userFileSc.nextLine());
            }
            // encode user file to backup with Base64
            for (int i = 0; i < lines.size(); ++i) {
                bw.write(Base64.getEncoder().encodeToString(lines.get(i).getBytes()) + "\r\n");
            }
            File[] backups = FileUtil.findFileMatches("backup", Setting.enterBackupDir, false, false);
            int exceedNumber = backups.length - Setting.backupKeepNumber;
            if (exceedNumber > 0) {
                // sort the backups in latest to oldest
                FileUtil.sortBackups(backups, true);
                for (int i = backups.length - 1; i >= backups.length - exceedNumber; --i) {
                    backups[i].delete();
                }
            }
            userFileSc.close();
            bw.flush();
            bw.close();
        } catch (IOException ioe) {
            // delete the exception backup
            System.out.println(errorHint + " : " + ioe.getMessage());
            backup.delete();
        }
    }

    /**
     * Execute to recover user file with decoding backup
     * 
     * @param langFile     language xml file
     * @param targetBackup target backup to recover from
     * @return true if success recover, false if not
     */
    public boolean execBackupRecover(XmlFile langFile, File targetBackup) throws FileNotFoundException {
        String errorHint = ((Element) XmlFile.navigate(langFile.getFileNode("backup"), "state")).getAttribute("error");
        String recoverHint = XmlFile.navigate(langFile.getFileNode("backup"), "fix", "recover").getTextContent();
        if (!targetBackup.exists() || targetBackup.getName().contains("crash")) {
            System.out.println(errorHint);
            return false;
        }
        boolean isOriginUserFileExist = userFile.exists();
        Scanner backupSc = new Scanner(
                new BufferedReader(new InputStreamReader(new FileInputStream(targetBackup), StandardCharsets.UTF_8)));
        BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(userFile.getPath()), StandardCharsets.UTF_8));
        ArrayList<String> lines = new ArrayList<>();
        System.out.println(recoverHint);
        while (backupSc.hasNextLine()) {
            lines.add(backupSc.nextLine());
        }
        try {
            // decode backup to user file with Base64
            for (int i = 0; i < lines.size(); ++i) {
                bw.write(new String(Base64.getDecoder().decode(lines.get(i).getBytes())) + "\r\n");
            }
            backupSc.close();
            bw.flush();
            bw.close();
            if(userFile.normalizeXml()){
                return true;
            }
            throw new IllegalArgumentException("Normalize failed.");
        } catch (IllegalArgumentException ex) {
            System.out.println(errorHint + " : " + ex.getMessage());
            try {
                backupSc.close();
                bw.flush();
                bw.close();
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
                System.out.println(ioe.getStackTrace());
            }
            // change to name to crashed
            targetBackup.renameTo(new File(targetBackup.getPath() + "_crashed"));
            if (!isOriginUserFileExist) {
                userFile.delete();
            }
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            System.out.println(ioe.getStackTrace());
        }
        return false;
    }

    /**
     * Execute to recover user file from clone
     * 
     * @param langFile    language xml file
     * @param targetClone target clone to recover from
     * @return true if recover success, false if error happen
     */
    public boolean execCloneRecover(XmlFile langFile, File targetClone) throws Exception {
        String errorHint = ((Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state"))
                .getAttribute("error");
        String recoverHint = XmlFile.navigate(langFile.getFileNode("userFile"), "fix", "recover").getTextContent();
        System.out.println(recoverHint);
        if (!userFile.replace(new XmlFile(targetClone, false), true)) {
            System.out.println(errorHint);
            return false;
        }
        return true;
    }

    /**
     * Check and reflash user file
     * 
     * @param langFile             language xml file
     * @param isNeedCheckNormalize is need to check normalized or not
     */
    public void checkUserFile(XmlFile langFile, boolean isNeedCheckNormalize) throws Exception{
        Node problemNode = XmlFile.navigate(langFile.getFileNode("userFile"), "problem");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Element state = (Element)XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        File dir = new File(Setting.enterUserFileDir);
        System.out.println(XmlFile.navigate(langFile.getFileNode("userFile"), "ini").getTextContent());
        if(!dir.exists()){
            dir.mkdirs();
        }
        ArrayList<File> backups = new ArrayList<>(Arrays.asList(FileUtil.findFileMatches("backup", "", true, false)));
        for (int i = 0; i < backups.size(); ++i) {
            if (backups.get(i).getName().contains("crash")) {
                backups.remove(backups.get(i--));
            }
        }
        ArrayList<File> cloneFiles = new ArrayList<>(Arrays.asList(FileUtil.findFileMatches("userFile", "", true, false)));
        for(int i = 0; i < cloneFiles.size(); ++i){
            String clonePath = cloneFiles.get(i).getPath().replaceAll("\\\\", "/");
            String userPath = userFile.getPath().replaceAll("\\\\", "/");
            if(clonePath.equals(userPath)){
                cloneFiles.remove(cloneFiles.get(i--));
            }
        }
        // if user file no exist
        if (!userFile.exists()) {
            String createUserFileHint = XmlFile.navigate(fixNode, "create").getTextContent();
            boolean[] needReScan = {false};
            do{
                // if no backup
                if (backups.size() == 0) {
                    String noBackupHint = XmlFile.navigate(problemNode
                    , "gone", "noBackup", "question").getTextContent();
                    String noBackupWithCloneHint = XmlFile.navigate(problemNode
                    , "gone", "noBackup", "chooseWithClone").getTextContent();
                    String noBackupNoCloneHint = XmlFile.navigate(problemNode
                    , "gone", "noBackup", "chooseNoClone").getTextContent();
                    while(true){
                        System.out.println(noBackupHint);
                        String choose = "2";
                        do{
                            if (cloneFiles.size() == 0) {
                                System.out.println(noBackupNoCloneHint);
                            } else {
                                System.out.println(noBackupWithCloneHint);
                                choose = FileUtil.sc.nextLine();
                            }
                        } while(!choose.matches("^[12]$"));
                        // use clone as user file
                        if(choose.equals("1")){
                            if(!useCloneAsUserFile(cloneFiles, langFile)){
                                continue;
                            }
                        // create new user file
                        }else{
                            if(cloneFiles.size() != 0 && !TextMode.confirm(fixNode)){
                                continue;
                            }
                            System.out.println(createUserFileHint);
                            userFile.createNewFile(getClass().getClassLoader().getResourceAsStream(Setting.sourceDir + Setting.templateName)
                            , true);
                        }
                        break;
                    }
                    needReScan[0] = false;
                // if only one backup
                } else if(backups.size() == 1){
                    String hasBackupHint = XmlFile.navigate(problemNode
                    , "gone", "hasBackup", "question").getTextContent();
                    String hasBackupWithClone = XmlFile.navigate(problemNode
                    , "gone", "hasBackup", "chooseWithClone").getTextContent();
                    String hasBackupNoClone = XmlFile.navigate(problemNode
                    , "gone", "hasBackup", "chooseNoClone").getTextContent();
                    while(true){
                        String choose;
                        System.out.println(hasBackupHint);
                        do{
                            if(cloneFiles.size() == 0){
                                System.out.println(hasBackupNoClone);
                                choose = FileUtil.sc.nextLine();
                                if(choose.matches("^[12]$")){
                                    break;
                                }
                            } else{
                                System.out.println(hasBackupWithClone);
                                choose = FileUtil.sc.nextLine();
                                if(choose.matches("^[1-3]$")){
                                    break;
                                }
                            }
                        } while(true);
                        // if no clone
                        if(cloneFiles.size() == 0){
                            // recover from backup
                            if(choose.equals("1")){
                                if(!useBackupRecoverUserFile(backups, langFile, needReScan)){
                                    continue;
                                }
                            // create new user file
                            } else{
                                // cancel current choose
                                if (!TextMode.confirm(fixNode)) {
                                    System.out.println(state.getAttribute("cancel"));
                                    continue;
                                }
                                System.out.println(createUserFileHint);
                                userFile.createNewFile(getClass().getClassLoader().getResourceAsStream(Setting.sourceDir + Setting.templateName)
                                , true);
                                needReScan[0] = false;
                            }
                            break;
                        // if has clone
                        } else{
                            // recover from backup
                            if(choose.equals("1")){
                                if(!useBackupRecoverUserFile(backups, langFile, needReScan)){
                                    continue;
                                }
                            // use clone as user file
                            } else if(choose.equals("2")){
                                if(!useCloneAsUserFile(cloneFiles, langFile)){
                                    continue;
                                }
                            // create new user file
                            } else{
                                // cancel current choose
                                if (!TextMode.confirm(fixNode)) {
                                    System.out.println(state.getAttribute("cancel"));
                                    continue;
                                }
                                System.out.println(createUserFileHint);
                                userFile.createNewFile(getClass().getClassLoader().getResourceAsStream(Setting.sourceDir + Setting.templateName)
                                , true);
                            }
                            break;
                        }
                    }
                // if have multiple backup
                } else{
                    String haveBackupHint = XmlFile.navigate(problemNode
                    , "gone", "haveBackup", "question").getTextContent();
                    String haveBackupWithClone = XmlFile.navigate(problemNode
                    , "gone", "haveBackup", "chooseWithClone").getTextContent();
                    String haveBackupNoClone = XmlFile.navigate(problemNode
                    , "gone", "haveBackup", "chooseNoClone").getTextContent();
                    while(true){
                        String choose;
                        System.out.println(haveBackupHint);
                        do {
                            if (cloneFiles.size() == 0) {
                                System.out.println(haveBackupNoClone);
                                choose = FileUtil.sc.nextLine();
                                if (choose.matches("^[12]$")) {
                                    break;
                                }
                            } else {
                                System.out.println(haveBackupWithClone);
                                choose = FileUtil.sc.nextLine();
                                if (choose.matches("^[1-3]$")) {
                                    break;
                                }
                            }
                        } while (true);
                        // if no clone
                        if (cloneFiles.size() == 0) {
                            // recover from backup(s)
                            if (choose.equals("1")) {
                                if(!useBackupRecoverUserFile(backups, langFile, needReScan)){
                                    continue;
                                }
                            // create new user file
                            } else {
                                // cancel current choose
                                if (!TextMode.confirm(fixNode)) {
                                    System.out.println(state.getAttribute("cancel"));
                                    continue;
                                }
                                System.out.println(createUserFileHint);
                                userFile.createNewFile(getClass().getClassLoader().getResourceAsStream(Setting.sourceDir + Setting.templateName)
                                , true);
                                needReScan[0] = false;
                            }
                            break;
                            // if has clone
                        } else {
                            // recover from backup(s)
                            if (choose.equals("1")) {
                                if(!useBackupRecoverUserFile(backups, langFile, needReScan)){
                                    continue;
                                }
                            // use clone as user file
                            } else if (choose.equals("2")) {
                                if(!useCloneAsUserFile(cloneFiles, langFile)){
                                    continue;
                                }
                            // create new user file
                            } else {
                                // cancel current choose
                                if (!TextMode.confirm(fixNode)) {
                                    System.out.println(state.getAttribute("cancel"));
                                    continue;
                                }
                                System.out.println(createUserFileHint);
                                userFile.createNewFile(getClass().getClassLoader().getResourceAsStream(Setting.sourceDir + Setting.templateName)
                                , true);
                                needReScan[0] = false;
                            }
                            break;
                        }
                    }
                }
            } while(needReScan[0]);
        }
        if(isNeedCheckNormalize){
            normalizeUserFile(langFile);
        }
    }

    /**
     * Normalize the user file
     * 
     * @param langFile language xml file
     */
    public void normalizeUserFile(XmlFile langFile){
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Node repairNode = XmlFile.navigate(langFile.getFileNode("userFile"), "problem", "repair");
        Element state = (Element)XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        boolean isNormalizeComplete = userFile.normalizeXml();
        while (!isNormalizeComplete) {
            String choose;
            System.out.println(XmlFile.navigate(repairNode, "question").getTextContent());
            do{
                System.out.println(XmlFile.navigate(repairNode, "choose").getTextContent());
                choose = FileUtil.sc.nextLine();
            } while(!choose.matches("^[1-3]$"));
            try{
                if(!TextMode.confirm(fixNode)){
                    System.out.println(state.getAttribute("cancel"));
                    continue;
                }
                // recheck user file
                if(choose.equals("1")){
                    userFile.delete();
                    checkUserFile(langFile, false);
                // create new user file
                } else if(choose.equals("2")){
                    userFile.replace(getClass().getClassLoader().getResourceAsStream(Setting.sourceDir + Setting.templateName));
                }
                isNormalizeComplete = userFile.normalizeXml();
            } catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }

    /**
     * Reflash the user file
     */
    public void reflashUserFile(){
        userFile.reflash();
    }

    /**
     * @return document of user file
     */
    public Document getUserFileDocument(){
        return userFile.getDocument();
    }

    /**
     * Optimize the user file(to check clone(s) file)
     * 
     * @param langFile language xml file
     */
    public void optimizeUserFile(XmlFile langFile){
        Node problemNode = XmlFile.navigate(langFile.getFileNode("userFile"), "problem");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        ArrayList<File> cloneFiles = new ArrayList<>(Arrays.asList(FileUtil.findFileMatches("userFile", "", true, false)));
        for(int i = 0; i < cloneFiles.size(); ++i){
            String clonePath = cloneFiles.get(i).getPath().replaceAll("\\\\", "/");
            String userPath = userFile.getPath().replaceAll("\\\\", "/");
            if(clonePath.equals(userPath)){
                cloneFiles.remove(cloneFiles.get(i--));
            }
        }
        String removeHint = XmlFile.navigate(fixNode, "remove").getTextContent();
        String saveHint = XmlFile.navigate(fixNode, "save").getTextContent();
        String choose;
        // if has a single clone
        if(cloneFiles.size() == 1){
            while(true){
                System.out.println(XmlFile.navigate(problemNode, "clone", "single", "question").getTextContent());
                do{
                    System.out.println(XmlFile.navigate(problemNode, "clone", "single", "choose").getTextContent());
                    choose = FileUtil.sc.nextLine();
                } while(!choose.matches("^[12]$"));
                if(!TextMode.confirm(fixNode)){
                    System.out.println(state.getAttribute("cancel"));
                    continue;
                }
                // remove
                if(choose.equals("1")){
                    System.out.println(removeHint);
                    cloneFiles.get(0).delete();
                    cloneFiles.clear();
                // save
                } else{
                    System.out.println(saveHint);
                    FileUtil.saveFilesToPath(cloneFiles.toArray(new File[0]), Setting.savedCloneDir);
                }
                break;
            }
        // if have multiple clones
        } else if(cloneFiles.size() > 1){
            while(true){
                System.out.println(XmlFile.navigate(problemNode, "clone", "multiple", "question").getTextContent());
                do {
                    System.out.println(XmlFile.navigate(problemNode, "clone", "multiple", "choose").getTextContent());
                    choose = FileUtil.sc.nextLine();
                } while (!choose.matches("^[1-3]$"));
                // remove all clones
                if(choose.equals("1")){
                    if(!TextMode.confirm(fixNode)){
                        continue;
                    }
                    System.out.println(removeHint);
                    for(File clone : cloneFiles){
                        clone.delete();
                    }
                    cloneFiles.clear();
                // choose to remove
                } else if(choose.equals("2")){
                    String chooseRemoveHint = XmlFile.navigate(fixNode
                    , "order", "choose", "clone", "remove").getTextContent();
                    Set<Integer> deleteIndexes = new HashSet<>();
                    String chooseRemove;
                    do{
                        for(int i = 0; i < cloneFiles.size(); ++i){
                            boolean click = false;
                            for(int index : deleteIndexes){
                                if(index == i){
                                    click = true;
                                }
                            }
                            System.out.println((i + 1) + " : " + cloneFiles.get(i).getPath() + (click ? "    [v]" : ""));
                        }
                        System.out.println(chooseRemoveHint);
                        chooseRemove = FileUtil.sc.nextLine();
                        if(chooseRemove.matches("^[0-9]+$")){
                            int targetIndex = Integer.valueOf(chooseRemove) - 1;
                            if (!(targetIndex < cloneFiles.size() && targetIndex >= 0)) {
                                continue;
                            }
                            if (!deleteIndexes.add(targetIndex)) {
                                deleteIndexes.remove(targetIndex);
                            }
                         } else if(chooseRemove.equals("finish")){
                             if(deleteIndexes.size() == 0){
                                 System.out.println(state.getAttribute("error"));
                                 continue;
                             }else if(TextMode.confirm(fixNode)){
                                 break;
                             }
                         }
                    } while(!chooseRemove.equals("-1"));
                    // cancel choose clone(s) to remove
                    if(chooseRemove.equals("-1")){
                        System.out.println(state.getAttribute("cancel"));
                        continue;
                    } 
                    System.out.println(removeHint);
                    for(int deleteIndex : deleteIndexes){
                        cloneFiles.get(deleteIndex).delete();
                    }
                    for(int i = 0; i < cloneFiles.size(); ++i){
                        if(!cloneFiles.get(i).exists()){
                            cloneFiles.remove(cloneFiles.get(i--));
                        }
                    }
                    // save the remains
                    System.out.println(saveHint);
                    FileUtil.saveFilesToPath(cloneFiles.toArray(new File[0]), Setting.savedCloneDir);
                // save
                } else{
                    if(!TextMode.confirm(fixNode)){
                        System.out.println(state.getAttribute("cancel"));
                        continue;
                    }
                    System.out.println(saveHint);
                    FileUtil.saveFilesToPath(cloneFiles.toArray(new File[0]), Setting.savedCloneDir);
                }
                break;
            }
        }
        System.out.println(state.getAttribute("done"));
    }

    /**
     * Optimize the backup file
     * 
     * @param langFile language xml file
     */
    public void optimizeBackup(XmlFile langFile){
        Node optimizeNode = XmlFile.navigate(langFile.getFileNode("backup"), "fix", "optimize");
        Element state = (Element)XmlFile.navigate(langFile.getFileNode("backup"), "state");
        String crashRemoveHint = XmlFile.navigate(optimizeNode, "crashRemove").getTextContent();
        String correctLocationHint = XmlFile.navigate(optimizeNode, "correctLocation").getTextContent();
        ArrayList<File> backups = new ArrayList<>(Arrays.asList(FileUtil.findFileMatches("backup", "", true, false)));
        System.out.println(crashRemoveHint);
        for(int i = 0; i < backups.size(); ++i){
            if(backups.get(i).getName().contains("crash")){
                backups.get(i).delete();
                backups.remove(backups.get(i--));
            }
        }
        System.out.println(correctLocationHint);
        FileUtil.saveFilesToPath(backups.toArray(new File[0]), Setting.enterBackupDir);
        System.out.println(state.getAttribute("done"));
    }

    /**
     * Optimize all the files and dir
     */
    public void optimizeOther(){
        // backups/ other name file
        for(File backupsOtherFile : FileUtil.findFileMatches("", Setting.enterBackupDir, true, false)){
            if(backupsOtherFile.getName().matches("^[0-9]+(.*)")){
                if(backupsOtherFile.getName().matches("^[0-9]+backup$")){
                    continue;
                }
                String name = backupsOtherFile.getName().replaceAll("^[0-9]+", "");
                backupsOtherFile.renameTo(new File(Setting.enterBackupDir
                 + backupsOtherFile.getName().replace(name, "backup")));
            } else{
                backupsOtherFile.delete();
            }
        }
        // backups/ other dir
        for(File backupsDir : FileUtil.sortFileByCompare(FileUtil.findDirMatches(""
        , Setting.enterBackupDir, true, false), true) ){
            System.out.println(backupsDir.getPath());
            backupsDir.delete();
        }
        // config/
        for(File configOtherFile : FileUtil.findFileMatches("", Setting.enterSettingDir, true, false)){
            if(!configOtherFile.getName().equals(Setting.enterSettingName)){
                configOtherFile.delete();
            }
        }
        // backups/ other dir
        for (File configDir : FileUtil.sortFileByCompare(FileUtil.findDirMatches(""
        , Setting.enterSettingDir, true, false), true)) {
            configDir.delete();
        }
    }

    /**
     * Use the clone file as the user file when user file not exist
     * 
     * @param cloneFiles clone files of user file
     * @param langFile   language xml file
     * @return true if use clone as user file successfully, false if cancel
     */
    private boolean useCloneAsUserFile(ArrayList<File> cloneFiles, XmlFile langFile) throws Exception{
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Element state = (Element)XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        String chooseCloneReplaceHint = XmlFile.navigate(fixNode
        , "order", "choose", "clone", "recover").getTextContent();
        String chooseClone;
        do {
            for (int i = 0; i < cloneFiles.size(); ++i) {
                System.out.println((i + 1) + " : " + cloneFiles.get(i).getPath());
            }
            System.out.println(chooseCloneReplaceHint);
            chooseClone = FileUtil.sc.nextLine();
            if (chooseClone.matches("^[0-9]+$") && Integer.valueOf(chooseClone) <= cloneFiles.size()
                    && Integer.valueOf(chooseClone) > 0 && TextMode.confirm(fixNode)) {
                break;
            }
        } while (!chooseClone.equals("-1"));
        // cancel use clone as user file
        if (chooseClone.equals("-1")) {
            System.out.println(state.getAttribute("cancel"));
            return false;
        }
        File target = cloneFiles.get(Integer.valueOf(chooseClone) - 1);
        if (!execCloneRecover(langFile, target)) {
            cloneFiles.remove(target);
            return false;
        }
        return true;
    }

    /**
     * Use the backup file to recover the user file when user file not exist
     * 
     * @param backups    backups array list
     * @param langFile   language xml file
     * @param needReScan reference to outside boolean needReScan
     * @return true if backup success or exception, false if cancel
     */
    private boolean useBackupRecoverUserFile(ArrayList<File> backups, XmlFile langFile
    , boolean[] needReScan) throws FileNotFoundException{
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        String chooseBackupRecoverHint = XmlFile.navigate(fixNode
        , "order", "choose", "backup", "recover").getTextContent();
        int targetIndex = 0;
        File[] temps = FileUtil.sortBackups(backups.toArray(new File[0]), true);
        if(backups.size() > 1){
            String chooseBackup;
            do {
                for (int i = 0; i < temps.length; ++i) {
                    System.out.println((i + 1) + " : " + temps[i].getPath()
                     + " (" + FileUtil.timeStampToString(temps[i].getPath()) + ")");
                }
                System.out.println(chooseBackupRecoverHint);
                chooseBackup = FileUtil.sc.nextLine();
                if (chooseBackup.matches("^[0-9]+$") && Integer.valueOf(chooseBackup) <= temps.length
                        && Integer.valueOf(chooseBackup) > 0 && TextMode.confirm(fixNode)) {
                    break;
                }
            } while (!chooseBackup.equals("-1"));
            // cancel recover from backup
            if (chooseBackup.equals("-1")) {
                System.out.println(state.getAttribute("cancel"));
                return false;
            }
            targetIndex = Integer.valueOf(chooseBackup) - 1;
        } else{
            // cancel recover from backup
            if(!TextMode.confirm(fixNode)){
                System.out.println(state.getAttribute("cancel"));
                return false;
            }
        }
        if (!execBackupRecover(langFile, temps[targetIndex])) {
            backups.remove(temps[targetIndex]);
            needReScan[0] = true;
        }else{
            needReScan[0] = false;
        }
        return true;
    }
    
    /**
     * Check and reflash setting file
     * 
     * @param langFile language xml file
     */
    private void checkSetting(XmlFile langFile) throws Exception {
        File dir = new File(Setting.enterSettingDir);
        if (!setting.exists()) {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            setting.createNewFile();
        } else {
            Scanner iniSc = new Scanner(new BufferedReader(new InputStreamReader(new FileInputStream(setting)
            , StandardCharsets.UTF_8)));
            while(iniSc.hasNextLine()){
                String []str = iniSc.nextLine().trim().split("\\s*=\\s*");
                if (str.length > 1){
                    iniHelper(str[0], str[1]);
                }
            }
            iniSc.close();
        }
        System.out.println(XmlFile.navigate(langFile.getFileNode("setting"), "ini").getTextContent());
        iniUpdate();
        System.out.println(XmlFile.navigate(langFile.getFileNode("setting")
        , "state").getAttributes().getNamedItem("done").getNodeValue());
    }

    /**
     * Get the setting data to program
     * 
     * @param key   line of the ini key
     * @param value line of the ini value
     */
    private void iniHelper(String key, String value){
        value = value.replaceAll("\"", "");
        // lang | language
        if (key.equalsIgnoreCase("lang") || key.equalsIgnoreCase("language")){
            if(value.matches(".*[Ee][Nn].*") || value.matches(".*[Uu][Ss].*")){
                Setting.lang = "en_US";
            } else if (value.matches(".*[Zz][Hh].*") || value.matches(".*[Tt][Ww].*")){
                Setting.lang = "zh_TW";
            } else if (value.matches(".*[Jj][Aa].*") || value.matches(".*[Jj][Pp].*")){
                Setting.lang = "ja_JP";
            }
        // userFileDir
        } else if (key.equalsIgnoreCase("userFileDir") || key.equalsIgnoreCase("userFileDirectory")){
            Setting.enterUserFileDir = value;
        // userFileName
        } else if(key.equalsIgnoreCase("userFileName")){
            if(value.isEmpty()){
                return;
            }
            Setting.enterUserFileName = value;
        // backupDir
        } else if(key.equalsIgnoreCase("backupDir") || key.equalsIgnoreCase("backupDirectory")){
            Setting.enterBackupDir = value;
        // autoBackup
        } else if(key.equalsIgnoreCase("autoBackup")){
            if (value.equalsIgnoreCase("false") || value.equals("0")){
                Setting.autoBackup = false;
            } else if(value.equalsIgnoreCase("true") || value.equals("1")){
                Setting.autoBackup = true;
            }
        // backupKeepNumber
        } else if(key.equalsIgnoreCase("backupKeepNumber")){
            if (!value.matches("^[0-9]+$")){
                return;
            }
            int number = Integer.valueOf(value);
            if (number >= 1 && number <= 30){
                ArrayList<File> backups = new ArrayList<File>(Arrays.asList(FileUtil.findFileMatches("backup"
                , Setting.enterBackupDir, false, false)));
                for(int i = 0; i < backups.size(); ++i){
                    if(backups.get(i).getName().contains("crash")){
                        backups.get(i).delete();
                        backups.remove(i--);
                    }
                }
                int exceedNumber = backups.size() - number;
                if(exceedNumber > 0){
                    File[] sortedBackups = FileUtil.sortBackups(backups.toArray(new File[0]), false);
                    for(int i = 0; i < exceedNumber; ++i){
                        sortedBackups[i].delete();
                    }
                }
                Setting.backupKeepNumber = number;
            }
        // autoBackupIntervalTimes
        } else if(key.equalsIgnoreCase("autoBackupIntervalTimes")){
            if (!value.matches("^[0-9]+$")) {
                return;
            }
            int times = Integer.valueOf(value);
            if (times >= 1 && times <= 10){
                Setting.autoBackupIntervalTimes = Integer.valueOf(value);
            }
        // userInterface
        } else if(key.equalsIgnoreCase("userInterface") || key.equalsIgnoreCase("UI")){
            if (value.equalsIgnoreCase("text")){
                Setting.userInterface = "text";
            } else if (value.equalsIgnoreCase("graphic")|| value.equalsIgnoreCase("graphics")){
                Setting.userInterface = "graphic";
            }
        // contactDataLimit
        } else if (key.equalsIgnoreCase("contactDataLimit")) {
            if (value.equalsIgnoreCase("false") || value.equals("0")) {
                Setting.contactDataLimit = false;
            } else if (value.equalsIgnoreCase("true") || value.equals("1")) {
                Setting.contactDataLimit = true;
            }
        // bookDataLimit
        }else if(key.equalsIgnoreCase("bookDataLimit")){
            if (value.equalsIgnoreCase("false") || value.equals("0")) {
                Setting.bookDataLimit = false;
            } else if (value.equalsIgnoreCase("true") || value.equals("1")) {
                Setting.bookDataLimit = true;
            }
        // workDataLimit
        } else if(key.equalsIgnoreCase("workDataLimit")){
            if(value.equalsIgnoreCase("false") || value.equals("0")){
                Setting.workDataLimit = false;
            } else if(value.equalsIgnoreCase("true") || value.equals("1")){
                Setting.workDataLimit = true;
            }
        }
    }
    
    /**
     * Update and rewrite setting file
     */
    public void iniUpdate() throws IOException {
        BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(setting), StandardCharsets.UTF_8));
        userFile = new XmlFile(Setting.enterUserFileDir + Setting.enterUserFileName, false);
        bw.write("lang = \"" + Setting.lang + "\"\r\n");
        bw.write("userFileDir = \"" + Setting.enterUserFileDir + "\"\r\n");
        bw.write("userFileName = \"" + Setting.enterUserFileName + "\"\r\n");
        bw.write("backupDir = \"" + Setting.enterBackupDir + "\"\r\n");
        bw.write("autoBackup = \"" + Setting.autoBackup + "\"\r\n");
        bw.write("backupKeepNumber = \"" + Setting.backupKeepNumber + "\"\r\n");
        bw.write("autoBackupIntervalTimes = \"" + Setting.autoBackupIntervalTimes + "\"\r\n");
        bw.write("userInterface = \"" + Setting.userInterface + "\"\r\n");
        bw.write("contactDataLimit = \"" + Setting.contactDataLimit + "\"\r\n");
        bw.write("bookDataLimit = \"" + Setting.bookDataLimit + "\"\r\n");
        bw.write("workDataLimit = \"" + Setting.workDataLimit + "\"\r\n");
        bw.flush();
        bw.close();
    }
}