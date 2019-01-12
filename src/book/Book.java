package book;

import java.util.*;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import file.XmlFile;
import file.fileContainer.FileContainer;
import file.fileUtil.FileUtil;
import process.mode.TextMode;
import setting.Setting;

public class Book {
    /**
     * The whole menu of personal book
     * 
     * @param langFile language xml file
     * @param fc       file container of user file, backup, and setting
     */
    public static void menu(XmlFile langFile, FileContainer fc) throws InterruptedException {
        Node bookNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "book");
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        String choose;
        while (true) {
            do {
                System.out.println(XmlFile.navigate(bookNode, "selection").getTextContent());
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
        Node addNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "book", "add");
        Node bookFileNode = fc.getUserFileDocument().getElementsByTagName("book").item(0);
        Node[] dataNodes = XmlFile.getChildNodes(bookFileNode, "data");
        String bookNameHint = XmlFile.navigate(addNode, "question", "bookName").getTextContent();
        String authorHint = XmlFile.navigate(addNode, "question", "author").getTextContent();
        String publishHint = XmlFile.navigate(addNode, "question", "publish").getTextContent();
        String labelHint = XmlFile.navigate(addNode, "question", "label").getTextContent();
        String sortHint = XmlFile.navigate(addNode, "question", "sort").getTextContent();
        String yearHint = XmlFile.navigate(addNode, "question", "year").getTextContent();
        String exceedHint = XmlFile.navigate(addNode, "exceed").getTextContent();
        String input;
        if (!TextMode.confirm(fixNode)) {
            System.out.println(state.getAttribute("cancel"));
            return false;
        }
        if (Setting.bookDataLimit && dataNodes.length >= 128) {
            System.out.println(exceedHint);
            return false;
        }
        Element newDataNode = fc.getUserFileDocument().createElement("data");
        // book name
        System.out.println(bookNameHint);
        input = FileUtil.sc.nextLine();
        newDataNode.setAttribute("bookName", input);
        // author
        System.out.println(authorHint);
        input = FileUtil.sc.nextLine();
        newDataNode.setAttribute("author", input);
        // publish
        System.out.println(publishHint);
        input = FileUtil.sc.nextLine();
        newDataNode.setAttribute("publish", input);
        // label
        do{   
            System.out.println(labelHint);
            input = FileUtil.sc.nextLine();
        } while(!input.matches("^[A-Za-z][0-9]{5}$"));
        newDataNode.setAttribute("label", input);
        // sort
        System.out.println(sortHint);
        input = FileUtil.sc.nextLine();
        newDataNode.setAttribute("sort", input);
        // year
        do{
            System.out.println(yearHint);
            input = FileUtil.sc.nextLine();
        }while(!input.matches("^[1][0-9]{3}|20[01][0-9]$"));
        newDataNode.setAttribute("year", input);
        bookFileNode.appendChild(newDataNode);
        bookFileNode.appendChild(fc.getUserFileDocument().createTextNode("\n"));
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
        Node viewNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "book", "view");
        Node bookFileNode = fc.getUserFileDocument().getElementsByTagName("book").item(0);
        Node[] dataNodes = XmlFile.getChildNodes(bookFileNode, "data");
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
        Node viewNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "book", "view");
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
                System.out.print(displayFilters.contains("bookName") ? "" : attributes.getNamedItem("bookName") + "  ");
                System.out
                        .print(displayFilters.contains("author") ? "" : attributes.getNamedItem("author") + "  ");
                System.out.print(
                        displayFilters.contains("publish") ? "" : attributes.getNamedItem("publish") + "  ");
                System.out.print(displayFilters.contains("label") ? ""
                        : attributes.getNamedItem("label") + "  ");
                System.out.print(displayFilters.contains("sort") ? "" : attributes.getNamedItem("sort") + "  ");
                System.out.println(displayFilters.contains("year") ? "" : attributes.getNamedItem("year"));
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
        Node viewNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "book", "view");
        String displayHint = XmlFile.navigate(viewNode, "order", "display").getTextContent();
        Element state = (Element) XmlFile.navigate(langFile.getFileNode("userFile"), "state");
        Set<String> originFilters = new HashSet<>(filters);
        String choose;
        while (true) {
            do {
                System.out.println("1 : bookName  " + (filters.contains("bookName") ? "[x]" : "[v]"));
                System.out.println("2 : author    " + (filters.contains("author") ? "[x]" : "[v]"));
                System.out.println("3 : publish   " + (filters.contains("publish") ? "[x]" : "[v]"));
                System.out.println("4 : label     " + (filters.contains("label") ? "[x]" : "[v]"));
                System.out.println("5 : sort      " + (filters.contains("sort") ? "[x]" : "[v]"));
                System.out.println("6 : year      " + (filters.contains("year") ? "[x]" : "[v]"));
                System.out.println(displayHint);
                choose = FileUtil.sc.nextLine();
                if (choose.equals("-1")) {
                    System.out.println(state.getAttribute("cancel"));
                    return originFilters;
                } else if (choose.equals("finish")) {
                    return filters;
                }
            } while (!choose.matches("^[1-6]$"));
            // book name
            if (choose.equals("1")) {
                if (!filters.add("bookName")) {
                    filters.remove("bookName");
                }
            // author
            } else if (choose.equals("2")) {
                if (!filters.add("author")) {
                    filters.remove("author");
                }
            // publish
            } else if (choose.equals("3")) {
                if (!filters.add("publish")) {
                    filters.remove("publish");
                }
            // label
            } else if (choose.equals("4")) {
                if (!filters.add("label")) {
                    filters.remove("label");
                }
            // sort
            } else if (choose.equals("5")) {
                if (!filters.add("sort")) {
                    filters.remove("sort");
                }
            // year
            } else if (choose.equals("6")) {
                if (!filters.add("year")) {
                    filters.remove("year");
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
        Node viewNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "book", "view");
        Node addNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "book", "add");
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
        } while (!choose.matches("^[1-6]$") || !TextMode.confirm(fixNode));
        // book name
        if (choose.equals("1")) {
            String bookNameHint = XmlFile.navigate(addNode, "question", "bookName").getTextContent();
            String chooseSearch;
            do {
                System.out.println(bookNameHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("bookName").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // author
        } else if (choose.equals("2")) {
            String authorHint = XmlFile.navigate(addNode, "question", "author").getTextContent();
            String chooseSearch;
            do {
                System.out.println(authorHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("author").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // publish
        } else if (choose.equals("3")) {
            String publishHint = XmlFile.navigate(addNode, "question", "publish").getTextContent();
            String chooseSearch;
            do {
                System.out.println(publishHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("publish").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // label
        } else if (choose.equals("4")) {
            String labelHint = XmlFile.navigate(addNode, "question", "label").getTextContent();
            String chooseSearch;
            do {
                System.out.println(labelHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!chooseSearch.matches("^[a-zA-Z][0-9]{5}$")
                || !TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("label").equals(chooseSearch)) {
                    targetDataNodes.add(dataNode);
                }
            }
        // sort
        } else if (choose.equals("5")) {
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
        // year
        } else {
            String yearHint = XmlFile.navigate(addNode, "question", "year").getTextContent();
            String chooseSearch;
            do {
                System.out.println(yearHint);
                chooseSearch = FileUtil.sc.nextLine();
            } while (!chooseSearch.matches("^[1][0-9]{3}|20[01][0-9]$") || !TextMode.confirm(fixNode));
            for (Node dataNode : dataNodes) {
                if (((Element) dataNode).getAttribute("year").equals(chooseSearch)) {
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
        Node editNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "book", "edit");
        Node bookFileNode = fc.getUserFileDocument().getElementsByTagName("book").item(0);
        Node[] dataNodes = XmlFile.getChildNodes(bookFileNode, "data");
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
        Node editNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "book", "edit");
        String removeHint = XmlFile.navigate(editNode, "order", "remove").getTextContent();
        String choosedHint = XmlFile.navigate(editNode, "order", "choosed").getTextContent();
        Set<Integer> removeIndexes = new HashSet<>();
        String choose;
        do {
            for (int i = 0; i < dataNodes.length; ++i) {
                boolean click = removeIndexes.contains(i);
                NamedNodeMap attributes = dataNodes[i].getAttributes();
                System.out.print((i + 1) + " : ");
                System.out.print(attributes.getNamedItem("bookName") + "  ");
                System.out.print(attributes.getNamedItem("author") + "  ");
                System.out.print(attributes.getNamedItem("publish") + "  ");
                System.out.print(attributes.getNamedItem("label") + "  ");
                System.out.print(attributes.getNamedItem("sort") + "  ");
                System.out.println(attributes.getNamedItem("year") + (click ? "   [v]" : ""));
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
        Node addNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "book", "add");
        Node fixNode = XmlFile.navigate(langFile.getFileNode("userFile"), "fix");
        Node editNode = XmlFile.navigate(langFile.getFileNode("userFile"), "function", "book", "edit");
        String bookNameHint = XmlFile.navigate(addNode, "question", "bookName").getTextContent();
        String authorHint = XmlFile.navigate(addNode, "question", "author").getTextContent();
        String publishHint = XmlFile.navigate(addNode, "question", "publish").getTextContent();
        String labelHint = XmlFile.navigate(addNode, "question", "label").getTextContent();
        String sortHint = XmlFile.navigate(addNode, "question", "sort").getTextContent();
        String yearHint = XmlFile.navigate(addNode, "question", "year").getTextContent();
        String modifyHint = XmlFile.navigate(editNode, "order", "modify").getTextContent();
        String choose;
        do {
            for (int i = 0; i < dataNodes.length; ++i) {
                NamedNodeMap attributes = dataNodes[i].getAttributes();
                System.out.print((i + 1) + " : ");
                System.out.print(attributes.getNamedItem("bookName") + "  ");
                System.out.print(attributes.getNamedItem("author") + "  ");
                System.out.print(attributes.getNamedItem("publish") + "  ");
                System.out.print(attributes.getNamedItem("label") + "  ");
                System.out.print(attributes.getNamedItem("sort") + "  ");
                System.out.println(attributes.getNamedItem("year"));
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
        // book name
        System.out.println(bookNameHint + " --("
                + dataNodes[index].getAttributes().getNamedItem("bookName").getNodeValue() + ")--");
        choose = FileUtil.sc.nextLine();
        dataNodes[index].getAttributes().getNamedItem("bookName").setNodeValue(choose);
        // author
        System.out.println(authorHint + " --("
                    + dataNodes[index].getAttributes().getNamedItem("author").getNodeValue() + ")--");
        choose = FileUtil.sc.nextLine();
        dataNodes[index].getAttributes().getNamedItem("startTime").setNodeValue(choose);

        // publish
        System.out.println(publishHint + " --("
                    + dataNodes[index].getAttributes().getNamedItem("publish").getNodeValue() + ")--");
        choose = FileUtil.sc.nextLine();
        dataNodes[index].getAttributes().getNamedItem("publish").setNodeValue(choose);
        // label
        do {
            System.out.println(
                    labelHint + " --(" + dataNodes[index].getAttributes().getNamedItem("label").getNodeValue() + ")--");
            choose = FileUtil.sc.nextLine();
        } while (!choose.matches("^[a-zA-Z][0-9]{5}$"));
        dataNodes[index].getAttributes().getNamedItem("label").setNodeValue(choose);
        // sort
        System.out.println(
                sortHint + " --(" + dataNodes[index].getAttributes().getNamedItem("sort").getNodeValue() + ")--");
        choose = FileUtil.sc.nextLine();
        dataNodes[index].getAttributes().getNamedItem("sort").setNodeValue(choose);
        // year
        do{
            System.out.println(yearHint + " --("
                    + dataNodes[index].getAttributes().getNamedItem("year").getNodeValue() + ")--");
            choose = FileUtil.sc.nextLine();
        } while(!choose.matches("^[1][0-9]{3}|20[01][0-9]$"));

        dataNodes[index].getAttributes().getNamedItem("year").setNodeValue(choose);
    }
}