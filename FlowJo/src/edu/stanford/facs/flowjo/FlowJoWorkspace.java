package edu.stanford.facs.flowjo;

import java.io.*;
import java.util.*;

import javax.activation.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.isac.fcs.*;
import org.w3c.dom.*;

public class FlowJoWorkspace
{
  public static final String MIME_TYPE = "text/xml";
  public static final String DEFAULT_NAME = "FlowJo Workspace";

  static DocumentBuilder builder;

  String workspace;
  FCSTextSegment[] headers;
  String[] urls;
  File file;
  String name;

  public FlowJoWorkspace(
      String[] urls,
      FCSTextSegment[] headers,
      String name)
      throws ParserConfigurationException, TransformerException
  {
    if (builder == null)
      builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    this.urls = urls;
    this.headers = headers;
    this.name = name;

    workspace = flowjoWorkspace(urls, headers);
  }

  public DataHandler getDataHandler()
  {
    return new DataHandler(workspace, MIME_TYPE);
  }

  String flowjoWorkspace(
      String[] urls,
      FCSTextSegment[] headers)
      throws TransformerException
  {
    CharArrayWriter caw = new CharArrayWriter(2048);

    flowjoWorkspace(urls, headers, caw);
    caw.close();

    return caw.toString();
  }

  void flowjoWorkspace(
      String[] urls,
      FCSTextSegment[] headers,
      Writer out)
      throws TransformerException
  {
    Document doc = builder.newDocument();
    Element workspace = doc.createElement("Workspace");
    workspace.setAttribute("version", "20.0");
    workspace.setAttribute("origin", "Stanford FACS Datastore");
//    workspace.setAttribute("curGroup", "All Samples");
    doc.appendChild(workspace);

//    Element columns = doc.createElement("Columns");
//    columns.setAttribute("wsSortOrder", "$BTIM");
//    columns.setAttribute("wsSortAscending", "1");
//    workspace.appendChild(columns);
//
//    Element column = doc.createElement("Column");
//    column.setAttribute("name", "Name");
//    column.setAttribute("width", "240");
//    columns.appendChild(column);
//
//    column = doc.createElement("Column");
//    column.setAttribute("name", "Statistic");
//    column.setAttribute("width", "80");
//    columns.appendChild(column);
//
//    column = doc.createElement("Column");
//    column.setAttribute("name", "#Cells");
//    column.setAttribute("width", "80");
//    columns.appendChild(column);

//    Node groups = workspace.appendChild(doc.createElement("Groups"));
//
//    Element groupNode = doc.createElement("GroupNode");
//    groupNode.setAttribute("name", "All Samples");
//    groupNode.setAttribute("owningGroup", "All Samples");
//    groupNode.setAttribute("sortPriority", "10");
//    groups.appendChild(groupNode);
//
//    Element group = doc.createElement("Group");
//    group.setAttribute("name", "All Samples");
//    groupNode.appendChild(group);
//
//    group.appendChild(doc.createElement("Criteria"));
//
//    Node sampleRefs = group.appendChild(doc.createElement("SampleRefs"));

    Node sampleList = workspace.appendChild(doc.createElement("SampleList"));
    for (int i = 0; i < headers.length; ++i)
    {
//      Element sampleRef = doc.createElement("SampleRef");
//      sampleRef.setAttribute("sampleID", String.valueOf(i + 1));
//      sampleRefs.appendChild(sampleRef);

      Node sample = sampleList.appendChild(doc.createElement("Sample"));

      Node keywords = sample.appendChild(doc.createElement("Keywords"));
      Iterator<String> k = headers[i].getAttributeNames().iterator();
      while (k.hasNext())
      {
        String attribute = k.next();
        Element keyword = doc.createElement("Keyword");
        keyword.setAttribute("name", attribute);
        keyword.setAttribute("value", headers[i].getAttribute(attribute));
        keywords.appendChild(keyword);
      }

      Element dataSet = doc.createElement("DataSet");
      dataSet.setAttribute("sampleID", String.valueOf(i + 1));
      dataSet.setAttribute("uri", urls[i]);
      sample.appendChild(dataSet);

//      Element sampleNode = doc.createElement("SampleNode");
//      sampleNode.setAttribute("sampleID", String.valueOf(i + 1));
//      sampleNode.setAttribute("name", headers[i].getAttribute("$FIL"));
//      sampleNode.setAttribute("sortPriority", "10");
//      sample.appendChild(sampleNode);
//
//      Element graph = doc.createElement("Graph");
//      graph.setAttribute("type", "Pseudocolor");
//      graph.setAttribute("showAxes", "tnlTNL");
//      graph.setAttribute("backColor", "#FFFFFF");
//      graph.setAttribute("foreColor", "#000000");
//      graph.setAttribute("smoothing", "0");
//      graph.setAttribute("histogramSmoothingCount", "0");
//      graph.setAttribute("showOutliers", "0");
//      graph.setAttribute("drawLargeDots", "0");
//      graph.setAttribute("scientificNotation", "0");
//      graph.setAttribute("showGateNames", "0");
//      graph.setAttribute("showUncompParm", "0");
//      graph.setAttribute("showGates", "0");
//      graph.setAttribute("xLabelOverride", "0");
//      graph.setAttribute("yLabelOverride", "0");
//      graph.setAttribute("fast", "1");
//      graph.setAttribute("smoothingHighResolution", "1");
//      graph.setAttribute("histogramHighResolution", "1");
//      graph.setAttribute("showGateFreqs", "1");
//      graph.setAttribute("fast", "1");
//      sampleNode.appendChild(graph);
    }

    doc.normalize();

    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = factory.newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
//    transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
//    transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "Workspace");
//    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "Workspace");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    DOMSource source = new DOMSource(workspace);
    StreamResult result = new StreamResult(out);
    transformer.transform(source, result);
  }
}
