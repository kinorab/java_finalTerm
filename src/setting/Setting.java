package setting;

public class Setting{
    public static String lang = "en_US";

    public static String enterUserFileDir = "";

    public static String enterUserFileName = "userFile.xml";

    public static String enterBackupDir = "backups/";

    public static boolean autoBackup = false;

    public static int backupKeepNumber = 10;

    // every user manipulate will count interval times
    public static int autoBackupIntervalTimes = 3;

    public static String userInterface = "text";

    public static boolean contactDataLimit = true;

    public static boolean bookDataLimit = true;

    public static boolean workDataLimit = true;

//-------------below are not open to ini file---------------//

    public static String enterSettingDir = "config/";

    public static String enterBackupName = "backup";

    public static String enterSettingName = "setting.ini";

    public static String savedCloneDir = "clones/";

    public static String resourceDir = "res/";

    public static String sourceDir = "src/";

    public static String templateName = "template.xml";
}