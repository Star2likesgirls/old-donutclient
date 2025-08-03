package org.reflections.serializers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.Store;

public class XmlSerializer implements Serializer {
   public Reflections read(InputStream inputStream) {
      try {
         Document document = (new SAXReader()).read(inputStream);
         Map<String, Map<String, Set<String>>> storeMap = (Map)document.getRootElement().elements().stream().collect(Collectors.toMap(Node::getName, (index) -> {
            return (Map)index.elements().stream().collect(Collectors.toMap((entry) -> {
               return entry.element("key").getText();
            }, (entry) -> {
               return (Set)entry.element("values").elements().stream().map(Element::getText).collect(Collectors.toSet());
            }));
         }));
         return new Reflections(new Store(storeMap));
      } catch (Exception var4) {
         throw new ReflectionsException("could not read.", var4);
      }
   }

   public File save(Reflections reflections, String filename) {
      File file = Serializer.prepareFile(filename);

      try {
         FileOutputStream out = new FileOutputStream(file);
         Throwable var5 = null;

         try {
            (new XMLWriter(out, OutputFormat.createPrettyPrint())).write(this.createDocument(reflections.getStore()));
         } catch (Throwable var15) {
            var5 = var15;
            throw var15;
         } finally {
            if (out != null) {
               if (var5 != null) {
                  try {
                     out.close();
                  } catch (Throwable var14) {
                     var5.addSuppressed(var14);
                  }
               } else {
                  out.close();
               }
            }

         }

         return file;
      } catch (Exception var17) {
         throw new ReflectionsException("could not save to file " + filename, var17);
      }
   }

   private Document createDocument(Store store) {
      Document document = DocumentFactory.getInstance().createDocument();
      Element root = document.addElement("Reflections");
      store.forEach((index, map) -> {
         Element indexElement = root.addElement(index);
         map.forEach((key, values) -> {
            Element entryElement = indexElement.addElement("entry");
            entryElement.addElement("key").setText(key);
            Element valuesElement = entryElement.addElement("values");
            values.forEach((value) -> {
               valuesElement.addElement("value").setText(value);
            });
         });
      });
      return document;
   }
}
