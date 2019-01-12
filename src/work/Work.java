package work;

import java.util.*;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import file.XmlFile;
import file.fileContainer.FileContainer;
import file.fileUtil.FileUtil;
import process.mode.TextMode;
import setting.Setting;

public class Work{
    /**
     * The whole menu of personal work
     * 
     * @param langFile language xml file
     * @param fc       file container of user file, backup, and setting
     */
    public static void menu(XmlFile langFile, FileContainer fc) throws InterruptedException{
        Node workNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "work");
        Element state = (Element)XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        String choose;
        while(true){
            do{
                System.out.println(XmlFile.navigate(workNode, "selection").getTextContent());
                choose = FileUtil.sc.nextLine();
                if(choose.equals("-1")){
                    return;
                }
            } while(!choose.matches("^[1-3]$"));
            fc.normalizeUserFile(langFile);
            // add data
            if (choose.equals("1") && !addData(langFile, fc)){
                continue;
            }
            // view data
            else if (choose.equals("2") && !viewData(langFile, fc)){
                continue;
            }
            // edit data
            else if (choose.equals("3") && !editData(langFile, fc)){
                continue;
            }
            fc.reflashUserFile();
            ++process.Process.intervalTimes;
            process.Process.lock.notify();
            process.Process.lock.wait();
            System.out.println(state.getAttribute("done"));
        }
    }

    /**
     * Add data to user file
     * 
     * @param langFile language xml file
     * @param fc       file container of user file, backup, and setting
     * @return true if add success, false if canceled
     */
    private static boolean addData(XmlFile langFile, FileContainer fc){
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Node addNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "work", "add");
        Node workFileNode = fc.getUserFileDocument().getElementsByTagName("work").item(0);
        Node[] dataNodes = XmlFile.getChildNodes(workFileNode, "data");
        String workNameHint = XmlFile.navigate(addNode, "question", "workName").getTextContent();
        String startTimeHint = XmlFile.navigate(addNode, "question", "startTime").getTextContent();
        String finishTimeHint = XmlFile.navigate(addNode, "question", "finishTime").getTextContent();
        String finishPercentHint = XmlFile.navigate(addNode, "question", "finishPercent").getTextContent();
        String labelHint = XmlFile.navigate(addNode, "question", "label").getTextContent();
        String sortHint = XmlFile.navigate(addNode, "question", "sort").getTextContent();
        String workContentHint = XmlFile.navigate(addNode, "question", "workContent").getTextContent();
        String exceedHint = XmlFile.navigate(addNode, "exceed").getTextContent();
        String input;
        if(!TextMode.confirm(fixNode)){
            System.out.println(state.getAttribute("cancel"));
            return false;
        }
        if(Setting.workDataLimit && dataNodes.length >= 128){
            System.out.println(exceedHint);
            return false;
        }
        Element newDataNode = fc.getUserFileDocument().createElement("data");
        // work name
        System.out.println(workNameHint);
        input = FileUtil.sc.nextLine();
        newDataNode.setAttribute("workName", input);
        // start time
        do{
            System.out.println(startTimeHint);
            input = FileUtil.sc.nextLine();
        } while (!input.matches("^(([01][0-9])|(2[0-3])):[0-5][0-9]:[0-5][0-9]$"));
        newDataNode.setAttribute("startTime", input);
        // finish time
        do{
            System.out.println(finishTimeHint);
            input = FileUtil.sc.nextLine();
        } while (!input.matches("^(([01][0-9])|(2[0-3])):[0-5][0-9]:[0-5][0-9]$"));
        newDataNode.setAttribute("finishTime", input);
        // finish percent
        do{
            System.out.println(finishPercentHint);
            input = FileUtil.sc.nextLine();
        }while (!input.matches("^([1-9]?[0-9]){1}(\\.[0-9]{1,2})?[%]?$|^100[%]?$"));
        newDataNode.setAttribute("finishPercent", input.replace("%", "") + "%");
        // status
        double percent = Double.valueOf(input.replace("%", ""));
        if (percent == 100) {
            input = "完成";
        } else if (percent == 0) {
            input = "未開始";
        } else {
            input = "執行中";
        }
        newDataNode.setAttribute("status", input);
        // label
        do{
            System.out.println(labelHint);
            input = FileUtil.sc.nextLine();
        } while (!input.matches("^[a-zA-Z][0-9]{5}$"));
        newDataNode.setAttribute("label", input);
        // sort
        System.out.println(sortHint);
        input = FileUtil.sc.nextLine();
        newDataNode.setAttribute("sort", input);
        // work content
        System.out.println(workContentHint);
        input = FileUtil.sc.nextLine();
        newDataNode.setAttribute("workContent", input);
        workFileNode.appendChild(newDataNode);
        workFileNode.appendChild(fc.getUserFileDocument().createTextNode("\n"));
        return true;
    }

    /**
     * View the data and search data in user file
     * 
     * @param langFile language xml file
     * @param fc       file container of user file, backup, and setting
     * @return true if view success, false if canceled
     */
    private static boolean viewData(XmlFile langFile, FileContainer fc){
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Node viewNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "work", "view");
        Node workFileNode = fc.getUserFileDocument().getElementsByTagName("work").item(0);
        Node[] dataNodes = XmlFile.getChildNodes(workFileNode, "data");
        String questionHint = XmlFile.navigate(viewNode, "question").getTextContent();
        String chooseHint = XmlFile.navigate(viewNode, "choose").getTextContent();
        String choose;
        System.out.println(questionHint);
        do{
            System.out.println(chooseHint);
            choose = FileUtil.sc.nextLine();
            if(choose.equals("-1")){
                System.out.println(state.getAttribute("cancel"));
                return false;
            }
        }while(!choose.matches("^[12]$") || !TextMode.confirm(fixNode));
        // view all datas
        if(choose.equals("1")){
            viewAllDatas(langFile, dataNodes);
        // search datas
        } else{
            searchData(langFile, dataNodes);
        }
        return true;
    }

    /**
     * Use to view all data in pagination or filter column
     * 
     * @param langFile  language xml file
     * @param dataNodes array of user file's current data nodes
     */
    private static void viewAllDatas(XmlFile langFile, Node[] dataNodes){
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node viewNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "work", "view");
        String hint = XmlFile.navigate(viewNode, "order", "hint").getTextContent();
        String pageHint = XmlFile.navigate(viewNode, "order", "page").getTextContent();
        String noFoundHint = XmlFile.navigate(viewNode, "noFound").getTextContent();
        Set<String> displayFilters = new HashSet<>();
        String perLine = "128";
        int currentPage = 1;
        String choose;
        while (true) {
            int maxPage = (int) (Math.ceil(dataNodes.length / Double.valueOf(perLine)));
            if (maxPage == 0) {
                System.out.println(noFoundHint);
                return;
            }
            for (int i = (currentPage - 1) * Integer.valueOf(perLine); i < Math
                    .min(currentPage * Integer.valueOf(perLine), dataNodes.length); ++i) {
                NamedNodeMap attributes = dataNodes[i].getAttributes();
                System.out.print((i + 1) + " : ");
                System.out.print(displayFilters.contains("workName") ? "" 
                : attributes.getNamedItem("workName") + "  ");
                System.out.print(displayFilters.contains("startTime") ? "" 
                : attributes.getNamedItem("startTime") + "  ");
                System.out.print(displayFilters.contains("finishTime") ? "" 
                : attributes.getNamedItem("finishTime") + "  ");
                System.out.print(displayFilters.contains("finishPercent") ? ""
                : attributes.getNamedItem("finishPercent") + "  ");
                System.out.print(displayFilters.contains("status") ? "" 
                : attributes.getNamedItem("status") + "  ");
                System.out.print(displayFilters.contains("label") ? "" 
                : attributes.getNamedItem("label") + "  ");
                System.out.print(displayFilters.contains("sort") ? "" 
                : attributes.getNamedItem("sort") + "  ");
                System.out.println(displayFilters.contains("workContent") ? "" 
                : attributes.getNamedItem("workContent"));
            }
            System.out.printf("%35d / %d page\r\n", currentPage, maxPage);
            System.out.println(hint);
            choose = FileUtil.sc.nextLine();
            if (choose.equals("finish")) {
                break;
            } else if (choose.equals("display")) {
                displayFilters = displayFilter(langFile, displayFilters);
            } else if (choose.equals("page")) {
                do {
                    System.out.println(pageHint);
                    perLine = FileUtil.sc.nextLine();
                    if (perLine.equals("-1")) {
                        System.out.println(state.getAttribute("cancel"));
                        break;
                    }
                    // 1 ~ 9 | 10 ~ 99 | 100 ~ 119 | 120 ~ 128 | -1
                } while (!perLine.matches("^[1-9]|[1-9][0-9]|1[0-1][0-9]|12[0-8]|-1$"));
            } else if (choose.matches("^[0-9]+$")) {
                int page = Integer.valueOf(choose);
                if (page > 0 && page <= maxPage) {
                    currentPage = page;
                }
            }
        }
    }

    /**
     * Help viewAllDatas to choose which filter to display or not
     * 
     * @param langFile language xml file
     * @param filters  filter that to be hide
     * @return if cancel return origin set, if success return changed
     */
    private static Set<String> displayFilter(XmlFile langFile, Set<String> filters){
        Node viewNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "work", "view");
        String displayHint = XmlFile.navigate(viewNode, "order", "display").getTextContent();
        Element state = (Element)XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Set<String> originFilters = new HashSet<>(filters);
        String choose;
        while (true) {
            do {
                System.out.println("1 : workName        " + (filters.contains("workName") ? "[x]" : "[v]"));
                System.out.println("2 : startTime       " + (filters.contains("startTime") ? "[x]" : "[v]"));
                System.out.println("3 : finishTime      " + (filters.contains("finishTime") ? "[x]" : "[v]"));
                System.out.println("4 : finishPercent   " + (filters.contains("finishPercent") ? "[x]" : "[v]"));
                System.out.println("5 : status          " + (filters.contains("status") ? "[x]" : "[v]"));
                System.out.println("6 : label           " + (filters.contains("label") ? "[x]" : "[v]"));
                System.out.println("7 : sort            " + (filters.contains("sort") ? "[x]" : "[v]"));
                System.out.println("8 : workContent     " + (filters.contains("workContent") ? "[x]" : "[v]"));
                System.out.println(displayHint);
                choose = FileUtil.sc.nextLine();
                if (choose.equals("-1")) {
                    System.out.println(state.getAttribute("cancel"));
                    return originFilters;
                } else if(choose.equals("finish")){
                    return filters;
                }
            } while (!choose.matches("^[1-8]$"));
            // work name
            if (choose.equals("1")) {
                if (!filters.add("workName")) {
                    filters.remove("workName");
                }
            // startTime
            } else if(choose.equals("2")){
                if (!filters.add("startTime")) {
                    filters.remove("startTime");
                } 
            // finish time
            } else if (choose.equals("3")) {
                if (!filters.add("finishTime")) {
                    filters.remove("finishTime");
                }
            // finish percent
            } else if (choose.equals("4")) {
                if (!filters.add("finishPercent")) {
                    filters.remove("finishPercent");
                }
            // status
            } else if(choose.equals("5")){
                if (!filters.add("status")) {
                    filters.remove("status");
                }
            // label
            } else if(choose.equals("6")){
                if (!filters.add("label")) {
                    filters.remove("label");
                }
            // sort
            } else if(choose.equals("7")){
                if (!filters.add("sort")) {
                    filters.remove("sort");
                }
            // work content
            } else if(choose.equals("8")){
                if (!filters.add("workContent")) {
                    filters.remove("workContent");
                }
            }
        }
    }

    /**
     * Search data to find with certain attribute
     * 
     * @param langFile  language xml file
     * @param dataNodes array of user file's current data nodes
     */
    private static void searchData(XmlFile langFile, Node[] dataNodes){
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Node viewNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "work", "view");
        Node addNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "work", "add");
        String searchHint = XmlFile.navigate(viewNode, "order", "search").getTextContent();
        String noFoundHint = XmlFile.navigate(viewNode, "noFound").getTextContent();
        ArrayList<Node> targetDataNodes = new ArrayList<>();
        String choose;
        do{
            System.out.println(searchHint);
            choose = FileUtil.sc.nextLine();
            if(choose.equals("-1")){
                System.out.println(state.getAttribute("cancel"));
                return;
            }
        }while(!choose.matches("^[1-8]$") || !TextMode.confirm(fixNode));
        // work name
        if(choose.equals("1")){
            String workNameHint = XmlFile.navigate(addNode, "question", "workName").getTextContent();
            String chooseSearch;
            do{
                System.out.println(workNameHint);
                chooseSearch = FileUtil.sc.nextLine();
            }while(!TextMode.confirm(fixNode));
            for(Node dataNode : dataNodes){
                if(((Element)dataNode).getAttribute("workName").equals(chooseSearch)){
                    targetDataNodes.add(dataNode);
                }
            }
        // start time
        } else if (choose.equals("2")){
            String startTimeHint = XmlFile.navigate(addNode, "question", "startTime").getTextContent();
            String chooseSearch;
            do {
                System.out.println(startTimeHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!chooseSearch.matches("^(([01][0-9])|(2[0-3])):[0-5][0-9]:[0-5][0-9]$")
            || !TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("startTime").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // finish time
        } else if (choose.equals("3")) {
            String finishTimeHint = XmlFile.navigate(addNode, "question", "finishTime").getTextContent();
            String chooseSearch;
            do {
                System.out.println(finishTimeHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!chooseSearch.matches("^(([01][0-9])|(2[0-3])):[0-5][0-9]:[0-5][0-9]$")
                    || !TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("finishTime").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // finish percent
        } else if (choose.equals("4")) {
            String finishPercentHint = XmlFile.navigate(addNode, "question", "finishPercent").getTextContent();
            String chooseSearch;
            do {
                System.out.println(finishPercentHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!chooseSearch.matches("^([1-9]?[0-9]){1}(\\.[0-9]{1,2})?[%]?$|^100[%]?$")
                    || !TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("finishPercent").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // status
        } else if (choose.equals("5")) {
            String labelHint = XmlFile.navigate(addNode, "question", "status").getTextContent();
            String chooseSearch;
            do {
                System.out.println(labelHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!chooseSearch.matches("^完成|未開始|執行中$")
                    || !TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("status").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // label
        } else if (choose.equals("6")) {
            String statusHint = XmlFile.navigate(addNode, "question", "label").getTextContent();
            String chooseSearch;
            do {
                System.out.println(statusHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!chooseSearch.matches("^[a-zA-Z][0-9]{5}$")
             || !TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("label").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // sort
        } else if (choose.equals("7")) {
            String sortHint = XmlFile.navigate(addNode, "question", "sort").getTextContent();
            String chooseSearch;
            do {
                System.out.println(sortHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("sort").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // work content
        } else {
            String workContentHint = XmlFile.navigate(addNode, "question", "workContent").getTextContent();
            String chooseSearch;
            do {
                System.out.println(workContentHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("workContent").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        }
        if(targetDataNodes.size() == 0){
            System.out.println(noFoundHint);
            return;
        }
        viewAllDatas(langFile, targetDataNodes.toArray(new Node[0]));
    }

    /**
     * Edit the datas to remove or modify them
     * 
     * @param langFile language xml file
     * @param fc       file container of user file, backup, and setting
     * @return true if success edit, false if cancel
     */
    private static boolean editData(XmlFile langFile, FileContainer fc){
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node editNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "work", "edit");
        Node workFileNode = fc.getUserFileDocument().getElementsByTagName("work").item(0);
        Node[] dataNodes = XmlFile.getChildNodes(workFileNode, "data");
        String questionHint = XmlFile.navigate(editNode, "question").getTextContent();
        String chooseHint = XmlFile.navigate(editNode, "choose").getTextContent();
        String choose;
        System.out.println(questionHint);
        do{
            System.out.println(chooseHint);
            choose = FileUtil.sc.nextLine();
            if(choose.equals("-1")){
                System.out.println(state.getAttribute("cancel"));
                return false;
            }
        }while(!choose.matches("^[12]$"));
        // remove data
        if(choose.equals("1")){
            removeData(langFile, dataNodes);
        // modify data
        }else{
            modify(langFile, dataNodes);
        }
        return true;
    }

    /**
     * Remove the data in user file
     * 
     * @param langFile  language xml file
     * @param dataNodes array of user file's current data nodes
     */
    private static void removeData(XmlFile langFile, Node[] dataNodes){
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Node editNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "work", "edit");
        String removeHint = XmlFile.navigate(editNode, "order", "remove").getTextContent();
        String choosedHint = XmlFile.navigate(editNode, "order", "choosed").getTextContent();
        Set<Integer> removeIndexes = new HashSet<>();
        String choose;
        do{
            for(int i = 0; i < dataNodes.length; ++i){
                boolean click = removeIndexes.contains(i);
                NamedNodeMap attributes = dataNodes[i].getAttributes();
                System.out.print((i + 1) + " : ");
                System.out.print(attributes.getNamedItem("workName") + "  ");
                System.out.print(attributes.getNamedItem("startTime") + "  ");
                System.out.print(attributes.getNamedItem("finishTime") + "  ");
                System.out.print(attributes.getNamedItem("finishPercent") + "  ");
                System.out.print(attributes.getNamedItem("status") + "  ");
                System.out.print(attributes.getNamedItem("label") + "  ");
                System.out.print(attributes.getNamedItem("sort") + "  ");
                System.out.println(attributes.getNamedItem("workContent") + (click ? "   [v]" : ""));
            }
            System.out.println(removeHint);
            System.out.println(choosedHint + removeIndexes.size());
            choose = FileUtil.sc.nextLine();
            if(choose.matches("^[0-9]+$")){
                int removeIndex = Integer.valueOf(choose)-1;
                if(removeIndex < 0 || removeIndex >= dataNodes.length){
                    continue;
                }
                if(!removeIndexes.add(removeIndex)){
                    removeIndexes.remove(removeIndex);
                }
            } else if(choose.equals("-1")){
                System.out.println(state.getAttribute("cancel"));
                return;
            }
        } while(!choose.equals("finish") || !TextMode.confirm(fixNode));
        
        for(int removeIndex : removeIndexes){
            dataNodes[removeIndex].getParentNode().removeChild(dataNodes[removeIndex].getPreviousSibling());
            dataNodes[removeIndex].getParentNode().removeChild(dataNodes[removeIndex]);
        }
    }

    /**
     * Modify the data in user file
     * 
     * @param langFile  language xml file
     * @param dataNodes array of user file's current data nodes
     */
    private static void modify(XmlFile langFile, Node[] dataNodes){
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node addNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "work", "add");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Node editNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "work", "edit");
        String workNameHint = XmlFile.navigate(addNode, "question", "workName").getTextContent();
        String startTimeHint = XmlFile.navigate(addNode, "question", "startTime").getTextContent();
        String finishTimeHint = XmlFile.navigate(addNode, "question", "finishTime").getTextContent();
        String finishPercentHint = XmlFile.navigate(addNode, "question", "finishPercent").getTextContent();
        String labelHint = XmlFile.navigate(addNode, "question", "label").getTextContent();
        String sortHint = XmlFile.navigate(addNode, "question", "sort").getTextContent();
        String workContentHint = XmlFile.navigate(addNode, "question", "workContent").getTextContent();
        String modifyHint = XmlFile.navigate(editNode, "order", "modify").getTextContent();
        String choose;
        do{
            for (int i = 0; i < dataNodes.length; ++i) {
                NamedNodeMap attributes = dataNodes[i].getAttributes();
                System.out.print((i + 1) + " : ");
                System.out.print(attributes.getNamedItem("workName") + "  ");
                System.out.print(attributes.getNamedItem("startTime") + "  ");
                System.out.print(attributes.getNamedItem("finishTime") + "  ");
                System.out.print(attributes.getNamedItem("finishPercent") + "  ");
                System.out.print(attributes.getNamedItem("status") + "  ");
                System.out.print(attributes.getNamedItem("label") + "  ");
                System.out.print(attributes.getNamedItem("sort") + "  ");
                System.out.println(attributes.getNamedItem("workContent"));
            }
            System.out.println(modifyHint);
            choose = FileUtil.sc.nextLine();
            if(choose.matches("^[0-9]+$")){
                int modIndex = Integer.valueOf(choose)-1;
                if(modIndex >= 0 && modIndex < dataNodes.length && TextMode.confirm(fixNode)){
                    break;
                }
            }else if(choose.equals("-1")){
                System.out.println(state.getAttribute("cancel"));
                return;
            }
        } while(true);
        int index = Integer.valueOf(choose);
        // work name
        System.out.println(workNameHint + " --("
         + dataNodes[index].getAttributes().getNamedItem("workName").getNodeValue() + ")--");
        choose = FileUtil.sc.nextLine();
        dataNodes[index].getAttributes().getNamedItem("workName").setNodeValue(choose);
        // start time
        do {
            System.out.println(startTimeHint + " --("
             + dataNodes[index].getAttributes().getNamedItem("startTime").getNodeValue() + ")--");
            choose = FileUtil.sc.nextLine();
        } while (!choose.matches("^(([01][0-9])|(2[0-3])):[0-5][0-9]:[0-5][0-9]$"));
        dataNodes[index].getAttributes().getNamedItem("startTime").setNodeValue(choose);

        // finish time
        do {
            System.out.println(finishTimeHint + " --("
            + dataNodes[index].getAttributes().getNamedItem("finishTime").getNodeValue() + ")--");
            choose = FileUtil.sc.nextLine();
        } while (!choose.matches("^(([01][0-9])|(2[0-3])):[0-5][0-9]:[0-5][0-9]$"));
        dataNodes[index].getAttributes().getNamedItem("finishTime").setNodeValue(choose);
        // finish percent
        do {
            System.out.println(finishPercentHint + " --("
            + dataNodes[index].getAttributes().getNamedItem("finishPercent").getNodeValue() + ")--");
            choose = FileUtil.sc.nextLine();
        } while (!choose.matches("^([1-9]?[0-9]){1}(\\.[0-9]{1,2})?[%]?$|^100[%]?$"));
        dataNodes[index].getAttributes().getNamedItem("finishPercent").setNodeValue(choose.replace("%", "") + "%");
        // status
        double percent = Double.valueOf(choose.replace("%", ""));
        if (percent == 100) {
            choose = "完成";
        } else if (percent == 0) {
            choose = "未開始";
        } else {
            choose = "執行中";
        }
        dataNodes[index].getAttributes().getNamedItem("status").setNodeValue(choose);
        // label
        do {
            System.out.println(labelHint + " --("
            + dataNodes[index].getAttributes().getNamedItem("label").getNodeValue() + ")--");
            choose = FileUtil.sc.nextLine();
        } while (!choose.matches("^[a-zA-Z][0-9]{5}$"));
        dataNodes[index].getAttributes().getNamedItem("label").setNodeValue(choose);
        // sort
        System.out.println(sortHint + " --("
         + dataNodes[index].getAttributes().getNamedItem("sort").getNodeValue() + ")--");
        choose = FileUtil.sc.nextLine();
        dataNodes[index].getAttributes().getNamedItem("sort").setNodeValue(choose);
        // work content
        System.out.println(workContentHint + " --("
        + dataNodes[index].getAttributes().getNamedItem("workContent").getNodeValue() + ")--");
        choose = FileUtil.sc.nextLine();
        dataNodes[index].getAttributes().getNamedItem("workContent").setNodeValue(choose);
    }
}