package chen.yyds.py.util;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;


public class XmlFormatter {
    private static Document parseXmlFile(String in) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(in));
            return db.parse(is);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String format(String unformattedXml) {
        try {
            final Document document = parseXmlFile(unformattedXml);
            OutputFormat format = new OutputFormat(document);
            format.setLineWidth(120);
            format.setIndent(2);
            format.setAllowJavaNames(true);
            Writer out = new StringWriter();
            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.serialize(document);
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String formatNoError(String unformattedXml) {
        try {
            return format(unformattedXml);
        } catch (Exception e) {
            return unformattedXml;
        }
    }

//    public static String aa(String xml) {
//        try {
//            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//            Document doc = db.parse(xml);
//
//            StringWriter output = new StringWriter();
//            TransformerFactory tf = TransformerFactory.newInstance();
//            Transformer transformer = tf.newTransformer(new StreamSource(xslt));
//            transformer.transform(new DOMSource(doc), new StreamResult(output));
//
//            String html = output.toString();
//
//            // JEditorPane doesn't like the META tag...
//            html = html.replace("<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">", "");
//            return html;
//        } catch (IOException | ParserConfigurationException | SAXException | TransformerException e) {
//           return xml;
//        }
//    }
}


