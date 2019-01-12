import java.io.File;
import java.io.InputStream;
import java.util.function.Supplier;
import file.XmlFile;
import file.fileContainer.FileContainer;
import setting.Setting;

public class FinalTerm{
    private static XmlFile langFile = ((Supplier<XmlFile>)() -> { 
        XmlFile supplyFile = null;
        try{
            File temp = new File(Setting.resourceDir + "lang.xml");
            if(temp.exists()){
                supplyFile = new XmlFile(temp, true);
            } else{
                InputStream input = FinalTerm.class.getClassLoader().getResourceAsStream(Setting.sourceDir + temp.getName());
                supplyFile = new XmlFile(input, temp.getName(), true);
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
            System.exit(1);
        }
        return supplyFile;
    }).get();
    
    public static void main(String []args){
        FileContainer fc = new FileContainer(langFile);
        try{
            // run the process
            process.Process.exec(langFile, fc);
        } catch (RuntimeException rex){
            rex.printStackTrace();
        }
    }
}
