package process.mode;

import java.io.*;
import java.util.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import file.*;
import setting.*;
import file.fileContainer.*;
import file.fileUtil.*;

public class TextMode{

    /**
     * Backup manually in text mode
     * 
     * @param langFile language xml file
     * @param fc       file container of user file, backup, setting file
     * @return true if set not to be canceled
     * @throws IOException
     */
    public static boolean backupOption(XmlFile langFile, FileContainer fc) throws IOException{
        Node fixNode = XmlFile.navigate(langFile.getFileNode("backup"), "fix");
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("backup"), "state");
        if(!confirm(fixNode)){
            System.out.println(state.getAttribute("cancel"));
            return false;
        }
        fc.execBackup(langFile, false);
        return true;
    }

    /**
     * Recover in text mode
     *  
     * @param langFile language xml file
     * @param fc       file container of user file, backup, setting file
     * @return true if recover, false if canceled
     * @throws IOException
     */
    public static boolean recoverOption(XmlFile langFile, FileContainer fc) throws IOException{
        Node fixNode = XmlFile.navigate(langFile.getFileNode("backup"), "fix");
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("backup"), "state");
        String noBackupHint = XmlFile.navigate(fixNode, "noBackup").getTextContent();
        String chooseRecoverHint = XmlFile.navigate(fixNode, "order", "choose", "recover").getTextContent();
        File[] backups = FileUtil.sortBackups(FileUtil.findFileMatches("backup", Setting.enterBackupDir, false, false), true);
        String choose;
        if(backups.length == 0){
            System.out.println(noBackupHint);
            return false;
        }
        do{
            for(int i = 0; i < backups.length; ++i){
                System.out.println((i + 1) + " : " + backups[i].getName()
                 + " (" + FileUtil.timeStampToString(backups[i].getName()) + ")");
            }
            System.out.println(chooseRecoverHint);
            choose = FileUtil.sc.nextLine();
            if(choose.matches("^[0-9]+$") && Integer.valueOf(choose) <= backups.length
            && Integer.valueOf(choose) > 0 && confirm(fixNode)){
                break;
            }
        } while(!choose.equals("-1"));
        if(choose.equals("-1")){
            System.out.println(state.getAttribute("cancel"));
            return false;
        }
        if(!fc.execBackupRecover(langFile, backups[Integer.valueOf(choose)-1])){
            return false;
        }
        return true;
    }

    /**
     * Delete backup in text mode
     * 
     * @param langFile language xml file
     * @return true if delete, false if canceled
     */
    public static boolean deleteBackupOption(XmlFile langFile){
        Node fixNode = XmlFile.navigate(langFile.getFileNode("backup"), "fix");
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("backup"), "state");
        String chooseDeleteHint = XmlFile.navigate(fixNode, "order", "choose", "remove").getTextContent();
        String removeHint = XmlFile.navigate(fixNode, "remove").getTextContent();
        String choosedHint = XmlFile.navigate(fixNode, "order", "choosed").getTextContent();
        String noBackupHint = XmlFile.navigate(fixNode, "noBackup").getTextContent();
        Set<Integer> deleteIndexes = new HashSet<>();
        File[] backups = FileUtil.sortBackups(FileUtil.findFileMatches("backup", Setting.enterBackupDir, false, false), true);
        String choose;
        if(backups.length == 0){
            System.out.println(noBackupHint);
            return false;
        }
        do{
            for(int i = 0; i < backups.length; ++i){
                boolean click = false;
                for(int deleteIndex : deleteIndexes){
                    if(deleteIndex == i){
                        click = true;
                    }
                }
                System.out.println((i + 1) + " : " + backups[i].getName() + (click ? "    [v]" : ""));
            }
            System.out.println(chooseDeleteHint + "\r\n" + choosedHint + deleteIndexes.size());
            choose = FileUtil.sc.nextLine();
            if(choose.matches("^[0-9]+$")){
                int targetIndex = Integer.valueOf(choose)-1;
                if(!(targetIndex < backups.length && targetIndex >= 0)){
                    continue;
                }
                if (!deleteIndexes.add(targetIndex)) {
                    deleteIndexes.remove(targetIndex);
                }
            } else if(choose.equals("finish")){
                if(deleteIndexes.size() == 0){
                    System.out.println(state.getAttribute("error"));
                    continue;
                } else if(confirm(fixNode)){
                    break;
                }
            }
        } while(!choose.equals("-1"));
        
        if(choose.equals("-1")){
            System.out.println(state.getAttribute("cancel"));
            return false;
        }
        System.out.println(removeHint);
        for(int deleteIndex : deleteIndexes){
            backups[deleteIndex].delete();
        }
        return true;
    }

    /**
     * Set language option in text mode
     * 
     * @param langFile    language xml file
     * @param problemNode problem node in language xml file
     * @param state       setting state
     * @return true if set not to be canceled
     */
    public static boolean setLanguageOption(XmlFile langFile){
        Node problemNode = XmlFile.navigate(langFile.getFileNode("setting"), "problem");
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("setting"), "state");
        String chooseLang;
        System.out.println(XmlFile.navigate(problemNode, "setLanguage", "question").getTextContent());
        do {
            System.out.println(XmlFile.navigate(problemNode, "setLanguage", "choose").getTextContent() + " --("
                    + Setting.lang + ")--");
            chooseLang = FileUtil.sc.nextLine();
        } while (!chooseLang.matches("^[1-3]|(-1)$"));
        // change to en_US
        if (chooseLang.equals("1") && !Setting.lang.equals("en_US")) {
            Setting.lang = "en_US";
        // change to zh_TW
        } else if (chooseLang.equals("2") && !Setting.lang.equals("zh_TW")) {
            Setting.lang = "zh_TW";
        // change to ja_JP
        } else if(chooseLang.equals("3") && !Setting.lang.equals("ja_JP")){
            Setting.lang = "ja_JP";            
        // cancel
        } else if (chooseLang.equals("-1")) {
            System.out.println(state.getAttribute("cancel"));
            return false;
            // if selection repeats current language
        } else {
            System.out.println(state.getAttribute("invalid"));
            return false;
        }
        System.out.println(XmlFile.navigate(langFile.getFileNode("setting"), "fix", "language").getTextContent());
        return true;
    }

    /**
     * Set backup option in text mode
     * 
     * @param problemNode   problem node in language xml file
     * @param fixNode       fix node in language xml file
     * @param state         setting state
     * @return true if set not be canceled
     */
    public static boolean setBackupOption(XmlFile langFile){
        Node problemNode = XmlFile.navigate(langFile.getFileNode("setting"), "problem");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("setting"), "fix");
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("setting"), "state");
        String cancelHint = state.getAttribute("cancel");
        String chooseBackup;
        System.out.println(XmlFile.navigate(problemNode, "setBackup", "question").getTextContent());
        do {
            System.out.println(XmlFile.navigate(problemNode, "setBackup", "choose").getTextContent());
            chooseBackup = FileUtil.sc.nextLine();
        } while (!chooseBackup.matches("^[1-3]|(-1)$"));
        // autoBackup setting
        if (chooseBackup.equals("1")) {
            String chooseAutoBackup;
            String autoBackupHint = Setting.autoBackup
                    ? XmlFile.navigate(fixNode, "order", "choose", "disableAutoBackup").getTextContent()
                    : XmlFile.navigate(fixNode, "order", "choose", "enableAutoBackup").getTextContent();
            String autoBackupExecHint = Setting.autoBackup
                    ? XmlFile.navigate(fixNode, "autoOff").getTextContent()
                    : XmlFile.navigate(fixNode, "autoOn").getTextContent();
            do {
                System.out.println(autoBackupHint);
                chooseAutoBackup = FileUtil.sc.nextLine();
            } while (!chooseAutoBackup.matches("^[yn]$"));
            if (chooseAutoBackup.equals("y")) {
                System.out.println(autoBackupExecHint);
                Setting.autoBackup = !Setting.autoBackup;
                // cancel autoBackup setting
            } else {
                System.out.println(cancelHint);
                return false;
            }
            // initialize interval times
            process.Process.intervalTimes = 0;
        // change max keep backup number
        } else if (chooseBackup.equals("2")) {
            File[] backups = FileUtil.findFileMatches("backup", Setting.enterBackupDir, false, false);
            String changeKeepNumberHint = XmlFile.navigate(fixNode, "keep").getTextContent();
            String removeHint = XmlFile.navigate(fixNode, "remove").getTextContent();
            String chooseBackupKeep;
            do {
                System.out.println(XmlFile.navigate(fixNode, "order", "choose", "backupKeep").getTextContent() + " --("
                        + backups.length + " / " + Setting.backupKeepNumber + ")--");
                chooseBackupKeep = FileUtil.sc.nextLine();
            } while (!chooseBackupKeep.matches("^[1-9]|[1-2]?[0-9]|30|(-1)$"));
            int exceedNumber = backups.length - Integer.valueOf(chooseBackupKeep);
            // cancel keep backup number setting
            if (chooseBackupKeep.equals("-1")) {
                System.out.println(cancelHint);
                return false;
                // if set keep number bigger then current backup numbers
            } else if (exceedNumber > 0) {
                String choose;
                System.out.println(XmlFile.navigate(problemNode, "setBackupKeep", "question").getTextContent());
                do {
                    System.out.println(XmlFile.navigate(problemNode, "setBackupKeep", "choose").getTextContent());
                    choose = FileUtil.sc.nextLine();
                } while (!choose.matches("^[12]|(-1)$"));
                // latest to older sort
                FileUtil.sortBackups(backups, true);
                // remove old backups
                if (choose.equals("1")) {
                    if (!confirm(fixNode)) {
                        System.out.println(cancelHint);
                        return false;
                    }
                    System.out.println(removeHint);
                    for (int i = backups.length - 1; i >= backups.length - exceedNumber; --i) {
                        backups[i].delete();
                    }
                    // choose backups to remove
                } else if (choose.equals("2")) {
                    String deleteChoose;
                    Set<Integer> deleteIndexes = new HashSet<>();
                    String chooseRemoveHint = XmlFile.navigate(fixNode, "order", "choose", "remove").getTextContent();
                    String nowChooseHint = XmlFile.navigate(fixNode, "order", "choosed").getTextContent();
                    String requiredHint = XmlFile.navigate(fixNode, "order", "required").getTextContent();
                    String notEnoughHint = XmlFile.navigate(fixNode, "order", "notEnough").getTextContent();
                    do {
                        for (int i = 0; i < backups.length; ++i) {
                            boolean click = false;
                            for (int j : deleteIndexes) {
                                if (j == i) {
                                    click = true;
                                }
                            }
                            System.out.println((i + 1) + " : " + backups[i].getName() + " ("
                             + FileUtil.timeStampToString(backups[i].getName()) + ")" + (click ? "    [v]" : ""));
                        }
                        System.out.println(chooseRemoveHint + "\r\n" + nowChooseHint + deleteIndexes.size() + "\r\n"
                                + requiredHint + exceedNumber);
                        deleteChoose = FileUtil.sc.nextLine();
                        // if deleteChoose is integer matchescis
                        if (deleteChoose.matches("^[0-9]+$")) {
                            int index = Integer.valueOf(deleteChoose) - 1;
                            if (!deleteIndexes.add(index)) {
                                deleteIndexes.remove(index);
                            }
                        } else if (deleteChoose.equals("finish")) {
                            if (deleteIndexes.size() >= exceedNumber) {
                                break;
                            } else {
                                System.out.println(notEnoughHint);
                            }
                        }
                    } while (!deleteChoose.equals("-1"));
                    if (deleteChoose.equals("-1") || !confirm(fixNode)) {
                        System.out.println(cancelHint);
                        return false;
                    }
                    System.out.println(removeHint);
                    for(int deleteIndex : deleteIndexes){
                        backups[deleteIndex].delete();
                    }
                    // cancel keep backup number setting
                } else {
                    System.out.println(cancelHint);
                    return false;
                }
            }
            System.out.println(changeKeepNumberHint);
            Setting.backupKeepNumber = Integer.valueOf(chooseBackupKeep);
        // auto backup interval time(s)
        } else if (chooseBackup.equals("3")) {
            String intervalTimesHint = XmlFile.navigate(fixNode, "order", "choose", "backupInterval").getTextContent();
            String changeIntervalHint = XmlFile.navigate(fixNode, "interval").getTextContent();
            String chooseIntervalTimes;
            do {
                System.out.println(intervalTimesHint + " --(" + Setting.autoBackupIntervalTimes + ")--");
                chooseIntervalTimes = FileUtil.sc.nextLine();
            } while (!chooseIntervalTimes.matches("^[1-9]|10|(-1)$"));
            // cancel auto backup interval time(s)
            if (chooseIntervalTimes.equals("-1")) {
                System.out.println(cancelHint);
                return false;
            }
            System.out.println(changeIntervalHint);
            Setting.autoBackupIntervalTimes = Integer.valueOf(chooseIntervalTimes);
            // initialize interval time(s)
            process.Process.intervalTimes = 0;
            // cancel backup options
        } else {
            System.out.println(cancelHint);
            return false;
        }
        return true;
    }

    /**
     * Set user interface in text mode
     * 
     * @param problemNode problem node of language file
     * @param fixNode     fix node of language file
     * @param state       setting state
     * @return true if set not to be canceled
     */
    public static boolean setUserInterface(XmlFile langFile){
        Node problemNode = XmlFile.navigate(langFile.getFileNode("setting"), "problem");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("setting"), "fix");
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("setting"), "state");
        String changeInterfaceHint = XmlFile.navigate(fixNode, "UI").getTextContent();
        String cancelHint = state.getAttribute("cancel");
        String invalidHint = state.getAttribute("invalid");
        String chooseInterface;
        System.out.println(XmlFile.navigate(problemNode, "setUI", "question").getTextContent());
        do {
            System.out.println(XmlFile.navigate(problemNode, "setUI", "choose").getTextContent() + " --("
                    + Setting.userInterface + " mode)--");
            chooseInterface = FileUtil.sc.nextLine();
        } while (!chooseInterface.matches("^[12]|(-1)$"));
        if (chooseInterface.equals("2") && !Setting.userInterface.equals("graphic")) {
            if (!confirm(fixNode)) {
                System.out.println(cancelHint);
                return false;
            }
            System.out.println(changeInterfaceHint);
            Setting.userInterface = "graphic";
        } else if (chooseInterface.equals("-1")) {
            System.out.println(cancelHint);
            return false;
        } else {
            System.out.println(invalidHint);
            return false;
        }
        return true;
    }

    /**
     * Optimize the hole program and files in text mode
     * 
     * @param fixNode fix node of language file
     * @param state   setting state
     * @return ture if set not to be canceled
     */
    public static boolean optimize(XmlFile langFile, FileContainer fc) throws Exception{
        Node fixNode = XmlFile.navigate(langFile.getFileNode("setting"), "fix");
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("setting"), "state");
        String cancelHint = state.getAttribute("cancel");
        if (!(confirm(fixNode) && optimizeConfirm(fixNode))) {
            System.out.println(cancelHint);
            return false;
        }
        String optimizeUserFileHint = XmlFile.navigate(fixNode, "optimize", "userFile").getTextContent();
        String optimizeBackupHint = XmlFile.navigate(fixNode, "optimize", "backup").getTextContent();
        String optimizeSettingHint = XmlFile.navigate(fixNode, "optimize", "setting").getTextContent();
        String optimizeAllHint = XmlFile.navigate(fixNode, "optimize", "all").getTextContent();
        // optimize user file
        System.out.println(optimizeUserFileHint);
        fc.checkUserFile(langFile, true);
        fc.optimizeUserFile(langFile);
        // optimize backup file
        System.out.println(optimizeBackupHint);
        fc.optimizeBackup(langFile);
        // optimize setting file
        System.out.println(optimizeSettingHint);
        fc.refetch(langFile);
        // optimize all file and dir
        System.out.println(optimizeAllHint);
        fc.optimizeOther();
        return true;
    }

    /**
     * Refetch the setting file in text mode
     * 
     * @param langFile      language xml file
     * @param fc            file container of user file, backup, setting
     * @param fixNode       fix node of language file
     * @param state         setting state
     * @return true if set not to be canceled
     */
    public static boolean refetchSetting(XmlFile langFile, FileContainer fc){
        Node fixNode = XmlFile.navigate(langFile.getFileNode("setting"), "fix");
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("setting"), "state");
        String refetchHint = XmlFile.navigate(fixNode, "refetch").getTextContent();
        String cancelHint = state.getAttribute("cancel");
        if (!confirm(fixNode)) {
            System.out.println(cancelHint);
            return false;
        }
        System.out.println(refetchHint);
        fc.refetch(langFile);
        process.Process.intervalTimes = 0;
        return true;
    }

    /**
     * Ask users to confirm their choose
     * 
     * @param fixNode fix node of language file
     * @return true if confirm done, false if canceled
     */
    public static boolean confirm(Node fixNode) {
        String choose;
        while (true) {
            System.out.println(XmlFile.navigate(fixNode, "confirm").getTextContent());
            choose = FileUtil.sc.nextLine();
            if (choose.equals("y")) {
                return true;
            } else if (choose.equals("n")) {
                return false;
            }
        }
    }

    /**
     * Ask users to confirm their choose in optimize
     * 
     * @param fixNode fix node of language file
     * @return true if confirm done, false if canceled
     */
    public static boolean optimizeConfirm(Node fixNode) {
        String choose;
        while (true) {
            System.out.println(XmlFile.navigate(fixNode, "optimizeConfirm").getTextContent());
            choose = FileUtil.sc.nextLine();
            if (choose.equals("y")) {
                return true;
            } else if (choose.equals("n")) {
                return false;
            }
        }
    }
}