package contact;

import java.util.*;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import file.XmlFile;
import file.fileContainer.FileContainer;
import file.fileUtil.FileUtil;
import process.mode.TextMode;
import setting.Setting;

public class Contact {
    /**
     * The whole menu of personal contact
     * 
     * @param langFile language xml file
     * @param fc       file container of user file, backup, and setting
     */
    public static void menu(XmlFile langFile, FileContainer fc) throws InterruptedException {
        Node contactNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "contact");
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        String choose;
        while (true) {
            do {
                System.out.println(XmlFile.navigate(contactNode, "selection").getTextContent());
                choose = FileUtil.sc.nextLine();
                if (choose.equals("-1")) {
                    return;
                }
            } while (!choose.matches("^[1-3]$"));
            fc.normalizeUserFile(langFile);
            // add data
            if (choose.equals("1") && !addData(langFile, fc)) {
                continue;
            }
            // view data
            else if (choose.equals("2") && !viewData(langFile, fc)) {
                continue;
            }
            // edit data
            else if (choose.equals("3") && !editData(langFile, fc)) {
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
    private static boolean addData(XmlFile langFile, FileContainer fc) {
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Node addNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "contact", "add");
        Node contactFileNode = fc.getUserFileDocument().getElementsByTagName("contact").item(0);
        Node[] dataNodes = XmlFile.getChildNodes(contactFileNode, "data");
        String nameHint = XmlFile.navigate(addNode, "question", "name").getTextContent();
        String birthHint = XmlFile.navigate(addNode, "question", "birth").getTextContent();
        String phoneHint = XmlFile.navigate(addNode, "question", "phone").getTextContent();
        String sortHint = XmlFile.navigate(addNode, "question", "sort").getTextContent();
        String emailHint = XmlFile.navigate(addNode, "question", "email").getTextContent();
        String exceedHint = XmlFile.navigate(addNode, "exceed").getTextContent();
        String input;
        if (!TextMode.confirm(fixNode)) {
            System.out.println(state.getAttribute("cancel"));
            return false;
        }
        if (Setting.contactDataLimit && dataNodes.length >= 128) {
            System.out.println(exceedHint);
            return false;
        }
        Element newDataNode = fc.getUserFileDocument().createElement("data");
        // name
        System.out.println(nameHint);
        input = FileUtil.sc.nextLine();
        newDataNode.setAttribute("name", input);
        // birth
        while(true){
            do {
                System.out.println(birthHint);
                input = FileUtil.sc.nextLine();
            } while (!input.matches("^(0[1-9]|1[012])[-/.]?(0[1-9]|[12][0-9]|3[01])$"));
            if(input.matches("^02[-/.]?3[0-1]|0[469][-/.]?31$|11[-/.]?31$")){
                continue;
            }
            break;
        }
        newDataNode.setAttribute("birth", input);
        // phone
        do {
            System.out.println(phoneHint);
            input = FileUtil.sc.nextLine();
        } while (!input.matches("^\\(0[1-9]\\)[0-9]{7,8}$"));
        newDataNode.setAttribute("phone", input);
        // sort
        System.out.println(sortHint);
        input = FileUtil.sc.nextLine();
        newDataNode.setAttribute("sort", input);
        // email
        do {
            System.out.println(emailHint);
            input = FileUtil.sc.nextLine();
        } while (!input.matches("^[a-zA-Z0-9]+@[a-zA-Z]+(\\.[a-zA-Z]+)+$"));
        newDataNode.setAttribute("email", input);
        contactFileNode.appendChild(newDataNode);
        contactFileNode.appendChild(fc.getUserFileDocument().createTextNode("\n"));
        return true;
    }

    /**
     * View the data and search data in user file
     * 
     * @param langFile language xml file
     * @param fc       file container of user file, backup, and setting
     * @return true if view success, false if canceled
     */
    private static boolean viewData(XmlFile langFile, FileContainer fc) {
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Node viewNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "contact", "view");
        Node contactFileNode = fc.getUserFileDocument().getElementsByTagName("contact").item(0);
        Node[] dataNodes = XmlFile.getChildNodes(contactFileNode, "data");
        String questionHint = XmlFile.navigate(viewNode, "question").getTextContent();
        String chooseHint = XmlFile.navigate(viewNode, "choose").getTextContent();
        String choose;
        System.out.println(questionHint);
        do {
            System.out.println(chooseHint);
            choose = FileUtil.sc.nextLine();
            if (choose.equals("-1")) {
                System.out.println(state.getAttribute("cancel"));
                return false;
            }
        } while (!choose.matches("^[12]$") || !TextMode.confirm(fixNode));
        // view all datas
        if (choose.equals("1")) {
            viewAllDatas(langFile, dataNodes);
        // search datas
        } else {
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
    private static void viewAllDatas(XmlFile langFile, Node[] dataNodes) {
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node viewNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "contact", "view");
        String hint = XmlFile.navigate(viewNode, "order", "hint").getTextContent();
        String pageHint = XmlFile.navigate(viewNode, "order", "page").getTextContent();
        String noFoundHint = XmlFile.navigate(viewNode, "noFound").getTextContent();
        Set<String> displayFilters = new HashSet<>();
        String perLine = "128";
        int currentPage = 1;
        String choose;
        while (true) {
            int maxPage = (int) (Math.ceil(dataNodes.length / Double.valueOf(perLine)));
            if(maxPage == 0){
                System.out.println(noFoundHint);
                return;
            }
            for (int i = (currentPage - 1) * Integer.valueOf(perLine); i < Math
                    .min(currentPage * Integer.valueOf(perLine), dataNodes.length); ++i) {
                NamedNodeMap attributes = dataNodes[i].getAttributes();
                System.out.print((i + 1) + " : ");
                System.out.print(displayFilters.contains("name") ? "" : attributes.getNamedItem("name") + "  ");
                System.out
                        .print(displayFilters.contains("birth") ? "" : attributes.getNamedItem("birth") + "  ");
                System.out.print(
                        displayFilters.contains("phone") ? "" : attributes.getNamedItem("phone") + "  ");
                System.out.print(displayFilters.contains("sort") ? ""
                        : attributes.getNamedItem("sort") + "  ");
                System.out.println(displayFilters.contains("email") ? "" : attributes.getNamedItem("email"));
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
    private static Set<String> displayFilter(XmlFile langFile, Set<String> filters) {
        Node viewNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "contact", "view");
        String displayHint = XmlFile.navigate(viewNode, "order", "display").getTextContent();
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Set<String> originFilters = new HashSet<>(filters);
        String choose;
        while (true) {
            do {
                System.out.println("1 : name   " + (filters.contains("name") ? "[x]" : "[v]"));
                System.out.println("2 : birth  " + (filters.contains("birth") ? "[x]" : "[v]"));
                System.out.println("3 : phone  " + (filters.contains("phone") ? "[x]" : "[v]"));
                System.out.println("4 : sort   " + (filters.contains("sort") ? "[x]" : "[v]"));
                System.out.println("5 : email  " + (filters.contains("email") ? "[x]" : "[v]"));
                System.out.println(displayHint);
                choose = FileUtil.sc.nextLine();
                if (choose.equals("-1")) {
                    System.out.println(state.getAttribute("cancel"));
                    return originFilters;
                } else if (choose.equals("finish")) {
                    return filters;
                }
            } while (!choose.matches("^[1-5]$"));
            // name
            if (choose.equals("1")) {
                if (!filters.add("name")) {
                    filters.remove("name");
                }
            // birth
            } else if (choose.equals("2")) {
                if (!filters.add("birth")) {
                    filters.remove("birth");
                }
            // phone
            } else if (choose.equals("3")) {
                if (!filters.add("phone")) {
                    filters.remove("phone");
                }
            // sort
            } else if (choose.equals("4")) {
                if (!filters.add("sort")) {
                    filters.remove("sort");
                }
            // email
            } else if (choose.equals("5")) {
                if (!filters.add("email")) {
                    filters.remove("email");
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
    private static void searchData(XmlFile langFile, Node[] dataNodes) {
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Node viewNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "contact", "view");
        Node addNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "contact", "add");
        String searchHint = XmlFile.navigate(viewNode, "order", "search").getTextContent();
        String noFoundHint = XmlFile.navigate(viewNode, "noFound").getTextContent();
        ArrayList<Node> targetDataNodes = new ArrayList<>();
        String choose;
        do {
            System.out.println(searchHint);
            choose = FileUtil.sc.nextLine();
            if (choose.equals("-1")) {
                System.out.println(state.getAttribute("cancel"));
                return;
            }
        } while (!choose.matches("^[1-5]$") || !TextMode.confirm(fixNode));
        // name
        if (choose.equals("1")) {
            String nameHint = XmlFile.navigate(addNode, "question", "name").getTextContent();
            String chooseSearch;
            do {
                System.out.println(nameHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("name").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // birth
        } else if (choose.equals("2")) {
            String birthHint = XmlFile.navigate(addNode, "question", "birth").getTextContent();
            String chooseSearch;
            while(true){
                do {
                    System.out.println(birthHint);
                    chooseSearch = FileUtil.sc.nextLine();
                } while (!chooseSearch.matches("^(0[1-9]|1[012])[-/.]?(0[1-9]|[12][0-9]|3[01])$")
                    || !TextMode.confirm(fixNode));
                if (chooseSearch.matches("^02[-/.]?3[0-1]|0[469][-/.]?31$|11[-/.]?31$")) {
                    continue;
                }
                break;
            }
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("birth").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // phone
        } else if (choose.equals("3")) {
            String phoneHint = XmlFile.navigate(addNode, "question", "phone").getTextContent();
            String chooseSearch;
            do {
                System.out.println(phoneHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!chooseSearch.matches("^\\(0[1-9]\\)[0-9]{7,8}$")
                    || !TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("phone").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // sort
        } else if (choose.equals("4")) {
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
        // email
        } else {
            String emailHint = XmlFile.navigate(addNode, "question", "email").getTextContent();
            String chooseSearch;
            do {
                System.out.println(emailHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!chooseSearch.matches("^[a-zA-Z0-9]+@[a-zA-Z]+(\\.[a-zA-Z]+)+$") || !TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("email").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        }
        if (targetDataNodes.size() == 0) {
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
    private static boolean editData(XmlFile langFile, FileContainer fc) {
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node editNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "contact", "edit");
        Node contactFileNode = fc.getUserFileDocument().getElementsByTagName("contact").item(0);
        Node[] dataNodes = XmlFile.getChildNodes(contactFileNode, "data");
        String questionHint = XmlFile.navigate(editNode, "question").getTextContent();
        String chooseHint = XmlFile.navigate(editNode, "choose").getTextContent();
        String choose;
        System.out.println(questionHint);
        do {
            System.out.println(chooseHint);
            choose = FileUtil.sc.nextLine();
            if (choose.equals("-1")) {
                System.out.println(state.getAttribute("cancel"));
                return false;
            }
        } while (!choose.matches("^[12]$"));
        // remove data
        if (choose.equals("1")) {
            removeData(langFile, dataNodes);
        // modify data
        } else {
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
    private static void removeData(XmlFile langFile, Node[] dataNodes) {
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Node editNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "contact", "edit");
        String removeHint = XmlFile.navigate(editNode, "order", "remove").getTextContent();
        String choosedHint = XmlFile.navigate(editNode, "order", "choosed").getTextContent();
        Set<Integer> removeIndexes = new HashSet<>();
        String choose;
        do {
            for (int i = 0; i < dataNodes.length; ++i) {
                boolean click = removeIndexes.contains(i);
                NamedNodeMap attributes = dataNodes[i].getAttributes();
                System.out.print((i + 1) + " : ");
                System.out.print(attributes.getNamedItem("name") + "  ");
                System.out.print(attributes.getNamedItem("birth") + "  ");
                System.out.print(attributes.getNamedItem("phone") + "  ");
                System.out.print(attributes.getNamedItem("sort") + "  ");
                System.out.println(attributes.getNamedItem("email") + (click ? "   [v]" : ""));
            }
            System.out.println(removeHint);
            System.out.println(choosedHint + removeIndexes.size());
            choose = FileUtil.sc.nextLine();
            if (choose.matches("^[0-9]+$")) {
                int removeIndex = Integer.valueOf(choose) - 1;
                if (removeIndex < 0 || removeIndex >= dataNodes.length) {
                    continue;
                }
                if (!removeIndexes.add(removeIndex)) {
                    removeIndexes.remove(removeIndex);
                }
            } else if (choose.equals("-1")) {
                System.out.println(state.getAttribute("cancel"));
                return;
            }
        } while (!choose.equals("finish") || !TextMode.confirm(fixNode));

        for (int removeIndex : removeIndexes) {
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
    private static void modify(XmlFile langFile, Node[] dataNodes) {
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Node addNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "contact", "add");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Node editNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "contact", "edit");
        String nameHint = XmlFile.navigate(addNode, "question", "name").getTextContent();
        String birthHint = XmlFile.navigate(addNode, "question", "birth").getTextContent();
        String phoneHint = XmlFile.navigate(addNode, "question", "phone").getTextContent();
        String sortHint = XmlFile.navigate(addNode, "question", "sort").getTextContent();
        String emailHint = XmlFile.navigate(addNode, "question", "email").getTextContent();
        String modifyHint = XmlFile.navigate(editNode, "order", "modify").getTextContent();
        String choose;
        do {
            for (int i = 0; i < dataNodes.length; ++i) {
                NamedNodeMap attributes = dataNodes[i].getAttributes();
                System.out.print((i + 1) + " : ");
                System.out.print(attributes.getNamedItem("name") + "  ");
                System.out.print(attributes.getNamedItem("birth") + "  ");
                System.out.print(attributes.getNamedItem("phone") + "  ");
                System.out.print(attributes.getNamedItem("sort") + "  ");
                System.out.println(attributes.getNamedItem("email"));
            }
            System.out.println(modifyHint);
            choose = FileUtil.sc.nextLine();
            if (choose.matches("^[0-9]+$")) {
                int modIndex = Integer.valueOf(choose) - 1;
                if (modIndex >= 0 && modIndex < dataNodes.length && TextMode.confirm(fixNode)) {
                    break;
                }
            } else if (choose.equals("-1")) {
                System.out.println(state.getAttribute("cancel"));
                return;
            }
        } while (true);
        int index = Integer.valueOf(choose);
        // contact name
        System.out.println(nameHint + " --("
                + dataNodes[index].getAttributes().getNamedItem("name").getNodeValue() + ")--");
        choose = FileUtil.sc.nextLine();
        dataNodes[index].getAttributes().getNamedItem("name").setNodeValue(choose);
        // birth
        while(true){
            do {
                System.out.println(birthHint + " --("
                        + dataNodes[index].getAttributes().getNamedItem("birth").getNodeValue() + ")--");
                choose = FileUtil.sc.nextLine();
            } while (!choose.matches("^(0[1-9]|1[012])[-/.]?(0[1-9]|[12][0-9]|3[01])$"));
            if (choose.matches("^02[-/.]?3[0-1]|0[469][-/.]?31$|11[-/.]?31$")) {
                continue;
            }
            break;
        }
        dataNodes[index].getAttributes().getNamedItem("birth").setNodeValue(choose);
        // phone
        do {
            System.out.println(phoneHint + " --("
                    + dataNodes[index].getAttributes().getNamedItem("phone").getNodeValue() + ")--");
            choose = FileUtil.sc.nextLine();
        } while (!choose.matches("^(0[1-9])[0-9]{7,8}$"));
        dataNodes[index].getAttributes().getNamedItem("phone").setNodeValue(choose);
        // sort
        System.out.println(sortHint + " --("
                    + dataNodes[index].getAttributes().getNamedItem("sort").getNodeValue() + ")--");
        choose = FileUtil.sc.nextLine();
        dataNodes[index].getAttributes().getNamedItem("sort").setNodeValue(choose);
        // email
        do {
            System.out.println(
                    emailHint + " --(" + dataNodes[index].getAttributes().getNamedItem("email").getNodeValue() + ")--");
            choose = FileUtil.sc.nextLine();
        } while (!choose.matches("^[a-zA-Z0-9]+@[a-zA-Z]+(\\.[a-zA-Z]+)+$"));
        dataNodes[index].getAttributes().getNamedItem("email").setNodeValue(choose);
    }
}