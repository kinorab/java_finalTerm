package file;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import java.util.*;
import org.w3c.dom.*;

import setting.*;

public class XmlFile{
    private File file = null;
    private Document doc = null;

    /**
     * Construct xml file with file name
     * 
     * @param fileName    xml file name
     * @param isNormalize is need to normalized
     */
    public XmlFile(String fileName, boolean isNormalize){
        file = new File(fileName);
        if(isNormalize){
            normalizeXml();
        }
    }

    /**
     * Construct xml file with file
     * 
     * @param file        xml file
     * @param isNormalize is need to normalized
     */
    public XmlFile(File file, boolean isNormalize){
        this.file = file;
        if(isNormalize){
            normalizeXml();
        }
    }

    /**
     * Construct xml file with file
     * 
     * @param input       xml file input stream
     * @param isNormalize is need to normalized
     */
    public XmlFile(InputStream input, String fileName, boolean isNormalize) throws IOException {
        File resourceDir = new File(Setting.resourceDir);
        if(!resourceDir.exists()){
            resourceDir.mkdirs();
        }
        Files.copy(input, Paths.get(Setting.resourceDir + fileName), StandardCopyOption.REPLACE_EXISTING);
        file = new File(Setting.resourceDir + fileName);
        if (isNormalize) {
            normalizeXml();
        }
    }

    /**
     * Get the xml document
     * 
     * @return document of this xml file
     */
    public Document getDocument(){
        return doc;
    }

    /**
     * @return true if exist
     */
    public boolean exists(){
        return file.exists();
    }

    /**
     * @return file name
     */
    public String getName(){
        return file.getName();
    }

    /**
     * @return file path
     */
    public String getPath(){
        return file.getPath();
    }

    /**
     * Create a new xml file
     * 
     * @param isNormalize is need to normalize
     * @return true if success create a new, false if exists
     */
    public boolean createNewFile(boolean isNeedNormalize) throws IOException{
        if(!file.createNewFile()){
            return false;
        }
        if(isNeedNormalize){
            normalizeXml();
        }
        return true;
    }

    /**
     * Create a new xml file with template
     * 
     * @param input            input stream from template file
     * @param templateFileName use the template file
     * @param isNeedNormalize  is need to be normalized
     * @return true if success create a new, false if no exists
     */
    public boolean createNewFile(InputStream input, boolean isNeedNormalize) throws IOException {
        if (!file.createNewFile()) {
            return false;
        }
        replace(input);
        if (isNeedNormalize) {
            normalizeXml();
        }
        return true;
    }
    
    /**
     * Get entry tag with corresponding language setting(default is en_US)
     * 
     * @return node of entry tag
     */
    public Node getEntryNode(){
        NodeList list = doc.getElementsByTagName("entry");
        for(int i = 0; i < list.getLength(); ++i){
            if(list.item(i) == null) continue;
            if(((Element)list.item(i)).getAttribute("lang").equals(Setting.lang)){
                return list.item(i);
            }
        }
        return null;
    }

    /**
     * Get account tag in enter tag
     * 
     * @return node of account tag
     */
    public Node getAccountNode(){
        NodeList list = getEntryNode().getChildNodes();
        for(int i = 0; i < list.getLength(); ++i){
            if(list.item(i) == null || !list.item(i).getNodeName().equals("account")) continue;
            return list.item(i);
        }
        return null;
    }

    /**
     * Get account tag in global enter
     * 
     * @return array of account tag nodes
     */
    public Node[] getAccountGlobal(){
        NodeList list = doc.getElementsByTagName("account");
        ArrayList<Node> children = new ArrayList<>();
        for(int i = 0; i < list.getLength(); ++i){
            children.add(list.item(i));
        }
        return children.toArray(new Node[0]);
    }

    /**
     * Get file tag in enter tag
     * 
     * @param fileName (.*)setting(.*), (.*)userFile(.*), (.*)backup(.*)
     * @return node of corresponding fileName tag(no match will return null)
     */
    public Node getFileNode(String fileName){
        NodeList list = getEntryNode().getChildNodes();
        String checkStr;
        int lastIndex = fileName.lastIndexOf(".");
        if (lastIndex == -1) {
            checkStr = "(.*)" + fileName + "(.*)";
        } 
        else {
            // erase file extension
            checkStr = "(.*)" + fileName.substring(0, lastIndex) + "(.*)";
        }
        for (int i = 0; i < list.getLength(); ++i) {
            // avoid cast [#text] node and null node to Element
            if(list.item(i) == null || !list.item(i).getNodeName().equals("file")) continue;
            if (((Element)list.item(i)).getAttribute("name").matches(checkStr)) {
                return list.item(i);
            }
        }
        return null;
    }
    
    /**
     * Get dir tag in enter tag
     * 
     * @return node of dir tag
     */
    public Node getDir(){
        NodeList list = getEntryNode().getChildNodes();
        for (int i = 0; i < list.getLength(); ++i) {
            if (list.item(i) == null || !list.item(i).getNodeName().equals("dir")) continue;
            return list.item(i);
        }
        return null;
    }

    /**
     * Replace current with target xml file, and not change current file directory,
     * if current not exist, will auto create a new file to be replaced
     * 
     * @param targetFile         target xml file
     * @param isDeleteTargetFile after replaced, is need to delete the target xml
     *                           file
     * @return true if replace success, false if target file no exist
     */
    public boolean replace(XmlFile targetFile, boolean isDeleteTargetFile) throws IOException{
        if(!targetFile.exists()){
            return false;
        }
        if(isDeleteTargetFile){
            Files.move(targetFile.file.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else{
            Files.copy(targetFile.file.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        file = new File(Setting.enterUserFileDir + Setting.enterUserFileName);
        if(!file.exists()){
            return false;
        }
        return true;
    }

    /**
     * Replace current with target xml file, and not change current file directory,
     * if current not exist, will auto create a new file to be replaced
     * 
     * @param targetInput        target xml file
     * @param isDeleteTargetFile after replaced, is need to delete the target xml
     *                           file
     * @return true if replace success, false if target file no exist
     */
    public boolean replace(InputStream targetInput) throws IOException {
        Files.copy(targetInput, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        file = new File(Setting.enterUserFileDir + Setting.enterUserFileName);
        if (!file.exists()) {
            return false;
        }
        return true;
    }

    /**
     * Normalize the file to xml file
     * 
     * @return true if normalize successfully, false if file not exist
     */
    public boolean normalizeXml(){
        if(!file.exists()){
            return false;
        }
        try{
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            FileInputStream input = new FileInputStream(file.getPath());
            try{
                builder.setErrorHandler(null);
                doc = builder.parse(input);
                // normalization deal with typesetting problem
                doc.getDocumentElement().normalize();
                return true;
            } catch(Exception ex){
                System.out.println(ex.getMessage());
                input.close();
                doc = null;
            }
        } catch(Exception ex){
            ex.printStackTrace();          
        }
        return false;
    }

    /**
     * Delete the xml file
     * 
     * @return if file is deleted successfully
     */
    public boolean delete(){
        doc = null;
        return file.delete();
    }

    /**
     * Use transformer to reflash the current .xml file
     */
    public void reflash(){
        try{
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(file);
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.transform(source, result);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    //
    // ---------------------------below are static members------------------------------
    //

    /**
     * Navigate destination with source and tags
     * 
     * @param source source to begin
     * @param tags   from outer to inner tags
     * @return node where last tag in (if any tag dismatch return null)
     */
    public static Node navigate(Node source, String ...tags){
        NodeList list = source.getChildNodes();
        Node target = null;
        for(int i = 0; i < tags.length; ++i){
            for(int j = 0; j < list.getLength(); ++j){
                if(list.item(j) == null) continue;
                if(list.item(j).getNodeName().equals(tags[i])){
                    target = list.item(j);
                    list = target.getChildNodes();
                    break;
                }
                // once any tag no match return null
                else if(j == list.getLength() - 1){
                    return null;
                }
            }
        }
        return target;
    }

    /**
     * Navigate destination with source and tags at multiple global sources
     * 
     * @param sources sources to begin
     * @param tags    from outer to inner tags
     * @return array of node(if any of source is null, will return null)
     */
    public static Node[] navigateGlobal(Node[] sources, String ...tags){
        ArrayList<Node> targets = new ArrayList<>();
        for(Node source : sources){
            Node target = navigate(source, tags);
            if(target == null){
                return null;
            }
            targets.add(target);
        }
        return targets.toArray(new Node[0]);
    }

    /**
     * Get the tag node's child node(s) by child name
     * 
     * @param tagNode   tag node to get
     * @param childName the target child name
     * @return an array of tag node's child node(s)
     */
    public static Node[] getChildNodes(Node tagNode, String childName){
        NodeList childNodes = tagNode.getChildNodes();
        ArrayList<Node> targetChildNodes = new ArrayList<>();
        for(int i = 0; i < childNodes.getLength(); ++i){
            if(childName.equals(childNodes.item(i).getNodeName())){
                targetChildNodes.add(childNodes.item(i));
            }
        }
        return targetChildNodes.toArray(new Node[0]);
    }
}