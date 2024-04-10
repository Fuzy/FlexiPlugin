package bundle;

import com.intellij.openapi.diagnostic.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import usage.PsiNavigationDemoAction;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class XmlLoader {

    private static final Logger LOG = Logger.getInstance(PsiNavigationDemoAction.class);

    public static Map<String, String> load(InputStream[] streams, String elementName) throws RuntimeException {
        Map<String, String> map = new HashMap<>();

        for (InputStream is : streams) {
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(is);
                doc.getDocumentElement().normalize();


                NodeList nodeList = doc.getElementsByTagName(elementName);
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        String name = element.getAttribute("name");
                        Node titl = element.getElementsByTagName("titl").item(0);
                        if (titl == null) {
                            LOG.error("Element with key " + name + " has no titl");
                            continue;
                        }
                        String title = titl.getTextContent();
                        map.put(name, title);
                    }
                }
            } catch (ParserConfigurationException | SAXException | IOException e) {
                throw new RuntimeException(e);
            }

            try {
                is.close();
            } catch (IOException e) {
                LOG.error(e);
            }
        }

        return map;
    }


}
