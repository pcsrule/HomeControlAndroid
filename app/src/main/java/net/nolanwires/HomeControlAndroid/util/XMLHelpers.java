package net.nolanwires.HomeControlAndroid.util;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by nolan on 2/18/16.
 */
public class XMLHelpers {
    private static final String TAG = "XMLHelper";

    public static String getContentForTagName(NodeList nodes, String tagName) {
        int len = nodes.getLength();
        for(int i = 0; i < len; ++i) {
            Node curNode = nodes.item(i);
            if(curNode.getNodeName().equals(tagName)) {
                return curNode.getTextContent();
            }
        }
        return null;
    }

    public static Document getDocumentFromString(String xml) {
        Document doc;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = db.parse(is);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }

        return doc;
    }
}
