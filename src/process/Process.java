package process;

import java.util.*;
import java.io.*;
import java.text.*;
import org.w3c.dom.*;

import book.Book;
import contact.Contact;
import process.mode.*;
import file.*;
import file.fileContainer.*;
import file.fileUtil.FileUtil;
import setting.*;
import work.Work;

public class Process{

    private static boolean finish = false;
    // for synchronized lock
    public static Object lock = new Object();
    public static int intervalTimes = 0;

    /**
     * Checking account login is correct or not, and reflash the xml language file
     * 
     * @param langFile xml language file
     */
    private static void checkAccount(XmlFile langFile) {
        int testTimes = 5;
        String successHint = XmlFile.navigate(langFile.getAccountNode()
        , "state").getAttributes().getNamedItem("done").getNodeValue();
        String loginHint = XmlFile.navigate(langFile.getAccountNode()
        , "save", "user", "lastLogin").getTextContent();
        Console cnsl = System.console();
        String UNHint = XmlFile.navigate(langFile.getAccountNode(), "question", "userName").getTextContent();
        String PWHint = XmlFile.navigate(langFile.getAccountNode(), "question", "password").getTextContent();
        String invalidHint = XmlFile.navigate(langFile.getAccountNode(), "invalid").getTextContent();
        Node user = XmlFile.navigate(langFile.getAccountNode(), "save", "user");
        while (true) {
            System.out.println(UNHint);
            String inputUN = FileUtil.sc.nextLine();
            if (!inputUN.matches("^[a-zA-z]+$"))
                continue;
            System.out.println(PWHint);
            // if console support with hide password
            String inputPW = (cnsl != null) ? String.valueOf(cnsl.readPassword()) : FileUtil.sc.nextLine();
            if (inputUN.equals(user.getAttributes().getNamedItem("name").getNodeValue())
                    && inputPW.equals(user.getAttributes().getNamedItem("pw").getNodeValue())){
                break;
            }
            if(testTimes == 0) {
                System.out.print(XmlFile.navigate(langFile.getAccountNode(), "forceClosed").getTextContent());
                System.exit(0);
            }
            System.out.println(invalidHint + testTimes);
            testTimes--;
        }
        String lastLoginDate = processUserDate(XmlFile.navigateGlobal(langFile.getAccountGlobal()
        , "save", "user"), langFile);
        System.out.println(successHint);
        System.out.println(loginHint + lastLoginDate);
        langFile.reflash();
    }

    /**
     * Add new date node and remove old date node in global
     * 
     * @param userNodes node of the current user
     * @param langFile  xml language file
     * @return last login date of current user
     */
    private static String processUserDate(Node[] userNodes, XmlFile langFile){
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String dateStr = sdf.format(date);
        Node previousDateNode = null;
        for(Node userNode : userNodes)
            previousDateNode = processUserDateHelper(userNode, langFile, dateStr);
        return previousDateNode == null ? dateStr : previousDateNode.getTextContent();
    }

    /**
     * Help add new date node and remove old date node
     * 
     * @param userNode   node of the current user
     * @param langFile   xml language file
     * @param dateString date string
     * @return the node of previous date node, if no exist return null
     */
    private static Node processUserDateHelper(Node userNode, XmlFile langFile, String dateString){
        NodeList list = userNode.getChildNodes();
        Node previousDateNode = null;
        // new node of date
        Element newDateNode = langFile.getDocument().createElement("date");
        newDateNode.setAttribute("order", "0");
        newDateNode.setTextContent(dateString);
        // update all date node
        for(int i = 0, minOrderNum = 5; i < list.getLength(); ++i){
            if(list.item(i).getNodeName().equals("date")){
                Element currentDateNode = (Element) list.item(i);
                int orderNum = Integer.parseInt(currentDateNode.getAttribute("order")) + 1;
                // confirm date tag only below 4
                if (orderNum >= 5 || orderNum <= 0) {
                    userNode.removeChild(currentDateNode);
                    // remove while space before node tag
                    userNode.removeChild(list.item(i - 1));
                    continue;
                }
                currentDateNode.setAttribute("order", String.valueOf(orderNum));
                // get the node which has the min order of attribute value
                if(orderNum >= minOrderNum) continue;
                minOrderNum = orderNum;
                previousDateNode = list.item(i);
            }
        }
        // add new node of date
        if(previousDateNode == null){
            userNode.appendChild(newDateNode);
            // typesetting for date
            userNode.appendChild(langFile.getDocument().createTextNode("\n\t\t"));
            userNode.insertBefore(langFile.getDocument().createTextNode("\t"), newDateNode);
        } else {
            userNode.insertBefore(newDateNode, previousDateNode);
            // typesetting for date
            userNode.insertBefore(langFile.getDocument().createTextNode("\n\t\t"), previousDateNode);
        }
        return previousDateNode;
    }

    /**
     * To execute threads in process
     * 
     * @param langFile language xml file
     * @param fc       file container of user, backup, and setting file
     * @throws RuntimeException if wait mode has exception happen
     */
    public static void exec(XmlFile langFile, FileContainer fc) throws RuntimeException {
        // main thread of interface
        Thread mainThread = new Thread(() -> {
            while(!finish){
                try{
                    System.out.println(XmlFile.navigate(langFile.getAccountNode(), "wait").getTextContent());
                    Thread.sleep(1500);
                    synchronized(lock){
                        if (Setting.userInterface.equals("text")) {
                            finish = !waitExecuteTextMode(langFile, fc);
                        } else if (Setting.userInterface.equals("graphic")) {
                            finish = !waitExecuteGraphicMode(langFile, fc);
                        } else {
                            throw new RuntimeException("Undefined error happened in runtime");
                        }
                    }
                } catch (InterruptedException interEx) {
                    interEx.printStackTrace();
                }
            }
        });
        // sub thread of the update
        Thread updateThread = new Thread(() -> {
            synchronized (lock) {
                while(!finish){
                    try{
                        lock.wait();
                        if(Setting.autoBackup && intervalTimes >= Setting.autoBackupIntervalTimes){
                            fc.execBackup(langFile, true);
                            intervalTimes = 0;
                        }
                    } catch (Exception ex){
                        ex.printStackTrace();
                    }
                    lock.notify();
                }
            }
        });
        // user login
        checkAccount(langFile);
        // threads start
        mainThread.start();
        updateThread.start();
    }

    /**
     * Execute text mode and always wait for user interact
     * 
     * @param langFile language xml file
     * @param fc       file container of user, backup, and setting file
     * @return whether user finish the interact or not
     */
    private static boolean waitExecuteTextMode(XmlFile langFile, FileContainer fc) throws InterruptedException{
        boolean modeFinish = false;
        String select;
        while(!modeFinish){
            String selectHint = XmlFile.navigate(langFile.getAccountNode()
            , "function", "selection").getTextContent();
            Node fixNode = XmlFile.navigate(langFile.getFileNode("setting"), "fix");
            do{
                System.out.println(selectHint);
                select = FileUtil.sc.nextLine();
            } while (!select.matches("^[1-3]|(-[12])$"));
            if (select.equals("1")) {
                textUserFileProcess(langFile, fc);
            } else if (select.equals("2")) {
                textBackupProcess(langFile, fc);
            } else if (select.equals("3")) {
                modeFinish = textSettingProcess(langFile, fc);
            } else if (select.equals("-1") && TextMode.confirm(fixNode)) {
                System.out.println(XmlFile.navigate(langFile.getAccountNode()
                , "state").getAttributes().getNamedItem("finish").getNodeValue());
                checkAccount(langFile);
            } else if (select.equals("-2") && TextMode.confirm(fixNode)) {
                System.out.println(XmlFile.navigate(langFile.getAccountNode(), "exit").getTextContent());
                lock.notifyAll();
                return false;
            }
        }
        return true;
    }

    /**
     * Process the user file in text mode
     * 
     * @param langFile language xml file
     * @param fc       file container of user, backup, and setting file
     */
    private static void textUserFileProcess(XmlFile langFile, FileContainer fc){
        String userFileHint = XmlFile.navigate(langFile.getFileNode("userFile")
        , "function", "selection").getTextContent();
        String cancelHint = ((Element) XmlFile.navigate(langFile.getFileNode("userFile")
        , "state")).getAttribute("cancel");
        String select;
        try{
            while(true){
                fc.checkUserFile(langFile, true);
                do {
                    System.out.println(userFileHint);
                    select = FileUtil.sc.nextLine();
                } while (!select.matches("^[1-3]|(-1)$"));
                if (select.equals("1")) {
                    Contact.menu(langFile, fc);
                } else if (select.equals("2")) {
                    Book.menu(langFile, fc);
                } else if (select.equals("3")) {
                    Work.menu(langFile, fc);
                } else {
                    System.out.println(cancelHint);
                    break;
                }
            }
        } catch(Exception ex){
            System.out.println(ex.getMessage());
            System.out.println(ex.getStackTrace());
        }
    }

    /**
     * Process the backup in text mode
     * 
     * @param langFile language xml file
     * @param fc       file container of user, backup, and setting file
     */
    private static void textBackupProcess(XmlFile langFile, FileContainer fc){
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("backup"), "state");
        String backupHint = XmlFile.navigate(langFile.getFileNode("backup")
        , "function", "selection").getTextContent();
        String select;
        try{
            while(true){
                do {
                    System.out.println(backupHint);
                    select = FileUtil.sc.nextLine();
                } while (!select.matches("^[1-3]|(-1)$"));
                // backup
                if(select.equals("1")
                 && !TextMode.backupOption(langFile, fc)){
                    continue;
                // recover
                } else if(select.equals("2")
                 && !TextMode.recoverOption(langFile, fc)){
                    continue;
                // delete
                } else if(select.equals("3")
                 && !TextMode.deleteBackupOption(langFile)){
                    continue;
                // cancel
                } else if(select.equals("-1")){
                    System.out.println(state.getAttribute("cancel"));
                    break;
                }
                System.out.println(state.getAttribute("done"));
            }
        } catch(Exception ex){
            System.out.println(ex.getMessage());
            System.out.println(ex.getStackTrace());
        }
    }

    /**
     * Process the setting in text mode
     * 
     * @param langfile language xml file
     * @param fc       file container of user, backup, and setting file
     * @return true if need to change mode
     */
    private static boolean textSettingProcess(XmlFile langFile, FileContainer fc){
        String select;
        try{
            while(true){
                String settingHint = XmlFile.navigate(langFile.getFileNode("setting")
                , "function", "selection").getTextContent();
                Element state = (Element) XmlFile.navigate(langFile.getFileNode("setting"), "state");
                do{
                    System.out.println(settingHint);
                    select = FileUtil.sc.nextLine();
                } while(!select.matches("^[1-5]|(-1)$"));
                // set the program language
                if(select.equals("1")
                 && !TextMode.setLanguageOption(langFile)){
                    continue;
                // set backup option
                } else if(select.equals("2")
                 && !TextMode.setBackupOption(langFile)){
                    continue;
                // set user interface option
                } else if(select.equals("3")
                 && !TextMode.setUserInterface(langFile)){
                    continue;
                // optimize
                } else if(select.equals("4")
                 && !TextMode.optimize(langFile, fc)){
                    continue;
                // refetch setting file
                } else if(select.equals("5")
                 && !TextMode.refetchSetting(langFile, fc)){
                    continue;
                // cancel
                } else if(select.equals("-1")){
                    System.out.println(state.getAttribute("cancel"));
                    break;
                }
                fc.iniUpdate();
                System.out.println(((Element)XmlFile.navigate(langFile.getFileNode("setting")
                , "state")).getAttribute("done"));
                if(Setting.userInterface.equals("graphic")){
                    return true;
                }
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return false;
    }


    /**
     * Execute graphic mode and always wait for user interact
     * 
     * @param langFile language xml file
     * @param fc       file container of user, backup, and setting file
     * @return whether user finish the interact or not
     */
    private static boolean waitExecuteGraphicMode(XmlFile langFile, FileContainer fc) throws InterruptedException{
        System.out.println("Under develop...");
        System.out.println("Return to text mode");
        Setting.userInterface = "text";
        try{
            fc.iniUpdate();
        } catch(IOException ioe){
            ioe.printStackTrace();
        }
        return true;
    }
}