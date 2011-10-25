package edu.stanford.facs.desk;

import java.io.*;
import java.security.*;
import java.text.*;
import java.util.*;

import javax.naming.*;
import javax.naming.directory.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

//import netscape.ldap.util.ByteBuf;
//import netscape.ldap.util.MimeBase64Encoder;
//import libris.catalog.*;
//import org.apache.xerces.dom.*;
//import org.apache.xml.serialize.*;
import org.w3c.dom.*;

//import  edu.stanford.facs.dir.CatalogSession;

/**
 * Utility class to import data from Electric Desk
 *
 * @author Wayne A. Moore
 * @version 1.0
 */

public class Update
{
  private static FacsDesk eDesk;
  private static DeskContext desk_context;
  private static DirContext archive, institution;

//  private static Hashtable  desk_users;
  static List user_list;

//  private static Hashtable  uids = new Hashtable();
  private static long new_modify, last_modify;

//  private static BitSet  already_seen, already_cataloged, warehouse_reference, warehouse_request;
  private static boolean time_out = false;
  private static BitSet already_cataloged;
  private static Calendar stop;
  private static SimpleDateFormat timestamp = new SimpleDateFormat("yyyy-MM-dd");
  private static final String[] at_uid =
      {"uid"};
  private static final SearchControls sc_uid = new SearchControls(
      SearchControls.SUBTREE_SCOPE,
      0, 0,
      at_uid,
      false, false);

//  private static Hashtable  desktop = new Hashtable();
//  private static int[]  tapes = new int[500];
//  private static PrintWriter out;
  private static String xml_host = "FACS.Stanford.EDU";

//  private static String  xml_host = "Genet.Stanford.EDU";

  private Hashtable doctype_to_html = new Hashtable();
  private Hashtable style_sheets = new Hashtable();

//  private static CatalogAgent  catalog_agent;

  static boolean time_up()
  {
    if (time_out)
      return true;

    if (stop != null && Calendar.getInstance().after(stop))
      time_out = true;

    return time_out;
  }

  static void update_users()
  {
    try
    {
      Map users = new HashMap();
      eDesk.readInvestigators(eDesk.get("ED_CONTEXT:USERS.ED", true), users);
      Iterator i = users.entrySet().iterator();
      while (i.hasNext())
      {
        Map.Entry entry = (Map.Entry)i.next();
        String name = (String)entry.getKey();
        String uid = (String)entry.getValue();
        eDesk.addInvestigator(uid, name);
      }
      eDesk.writeInvestigators();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  static void update_user(
      String uid,
      String long_name)
  {
    Attributes newAt = null;
    String dn = "uid=" + uid + "," + eDesk.institution();

    try
    {
      NamingEnumeration ne = institution.search(
          "",
          "(&(uid=" + uid + ")(objectClass=Investigator))",
          sc_uid);
      if (ne.hasMore())
        return;

      newAt = new BasicAttributes(true);

      BasicAttribute ba = new BasicAttribute("objectClass");
      ba.add("FacsUser");
      ba.add("Investigator");
      ba.add("inetOrgPerson");
      ba.add("organizationalPerson");
      ba.add("person");
      ba.add("top");
      newAt.put(ba);

      newAt.put(new BasicAttribute("aci", "(targetattr != \"aci\")"
          +
          "(version 3.0; acl \"Self Read and Search\"; allow (compare,read,search) userdn = \"ldap:///"
          + dn + "\"; )"));

      newAt.put(new BasicAttribute("uid", uid));

      newAt.put(new BasicAttribute("EDeskLongName", long_name));

      int n = long_name.indexOf(",");
      if (n < 0)
      {
        newAt.put(new BasicAttribute("surname", long_name));
        newAt.put(new BasicAttribute("commonName", long_name));
      }
      else
      {
        String surname = long_name.substring(0, n).trim();
        String given_name = long_name.substring(n + 1, long_name.length()).trim();
        newAt.put(new BasicAttribute("surname", surname));
        newAt.put(new BasicAttribute("givenName", given_name));
        newAt.put(new BasicAttribute("commonName", given_name + " " + surname));
      }

      System.out.println("Adding user: " + uid + "    " + long_name);
      institution.createSubcontext("uid=" + uid, newAt);
    }
    catch (NamingException ex)
    {
      ex.printStackTrace();
      System.out.println(newAt);
    }
  }

  static boolean fetch_data(
      File envelope)
  {
    DeskInputStream drawer = null;
    String line, token;
    String title = null, cytometer = null, longName = null;
    String coord, sample, reagent;
    int warehouse, rank, count, protocol = 0, archive_id = 0;
    int number_of_samples = 0;
    Stack toDo = new Stack();
    File data_file;

    System.out.println("fetch: " + envelope);
    try
    {
      drawer = new DeskInputStream(envelope);
      line = drawer.readLine();
      if (!line.equals("CSdata V1"))
        throw new DeskException("bad format");

      while (true)
      {
        token = drawer.readWord();
        if (token == null)
          break;
        else
          line = drawer.readLine();
      }
      drawer.readLine();

      while (true)
      {
        token = drawer.readWord();
        if (token == null)
          break;
        if (!token.equals("CSampl"))
          throw new DeskException("bad format");
        drawer.readLine();

        coord = drawer.readWord();
        int cp = coord.indexOf('<');
        if (cp > 0)
          coord = coord.substring(0, cp);
        warehouse = Integer.parseInt(drawer.readWord());
        rank = Integer.parseInt(drawer.readWord());
        count = Integer.parseInt(drawer.readWord());
        drawer.readLine();

        sample = drawer.readLine();

        ++number_of_samples;

        StringBuffer fn = new StringBuffer(100);
        fn.append("ED_FTP:[");
        fn.append((warehouse / 36) % 245);
        fn.append("]");
        for (int w = warehouse; w > 0; w /= 36)
          fn.append("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(w % 36));
        fn.append(".ED_WH;1");

        System.out.println(fn);

        data_file = new File(envelope.getParentFile(), coord + ".ED_WH");
        /*
         if (!data_file.exists()
         || data_file.length() == 0)
         {
           try
           {
             if (eDesk.ftpc == null)
               eDesk.connect();
             if (!eDesk.ftpc.status( fn.toString() ))
               return  false;
           }
           catch (FileNotFoundException  fnf)
           {
             return  false;
           }
           toDo.push( new copy_data( fn.toString(), data_file ) );
         }
         */
        for (byte i = 0; i < rank; ++i)
        {
          line = drawer.readLine();
          reagent = drawer.readLine();
        }

        while (true)
        {
          token = drawer.readWord();
          if (token == null)
            break;
          if (token.equals("CScond"))
          {
            line = drawer.readLine();
            line = drawer.readLine();
            line = drawer.readLine();
            line = drawer.readLine();
          }
          else
            throw new DeskException("bad format");
        }
        drawer.readLine();
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      drawer.readToEnd();
      return false;
    }

    try
    {
      while (!toDo.empty())
      {
        copy_data one = (copy_data)toDo.pop();

        if (!one.file.exists()
            || one.file.length() == 0)
        {
          eDesk.copy(one.warehouse, one.file);
          System.out.println("copy " + one.warehouse + " to " + one.file);
        }
        if (time_up())
          return false;
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      return false;
    }
    return true;
  }


  static int count_data(
      File envelope)
  {
    DeskInputStream drawer = null;
    String line, token;
    String title = null, cytometer = null, longName = null;
    String coord, sample, reagent;
    int warehouse, rank, count, protocol = 0, archive_id = 0;
    int number_of_samples = 0;
    int data_files = 0;

    try
    {
      drawer = new DeskInputStream(envelope);
      line = drawer.readLine();
      if (!line.equals("CSdata V1"))
      {
        System.out.println(envelope.getName() + " bad format");
        return 1;
      }
//        throw new DeskException("bad format");

      while (true)
      {
        token = drawer.readWord();
        if (token == null)
          break;
        else
          line = drawer.readLine();
      }
      drawer.readLine();

      while (true)
      {
        token = drawer.readWord();
        if (token == null)
          break;
        if (!token.equals("CSampl"))
          throw new DeskException("bad format");
        drawer.readLine();
        ++data_files;

        coord = drawer.readWord();
        int cp = coord.indexOf('<');
        if (cp > 0)
          coord = coord.substring(0, cp);
        warehouse = Integer.parseInt(drawer.readWord());
        rank = Integer.parseInt(drawer.readWord());
        count = Integer.parseInt(drawer.readWord());
        drawer.readLine();

        sample = drawer.readLine();

        ++number_of_samples;

        StringBuffer fn = new StringBuffer(100);
        fn.append("ED_FTP:[");
        fn.append((warehouse / 36) % 245);
        fn.append("]");
        for (int w = warehouse; w > 0; w /= 36)
          fn.append("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(w % 36));
        fn.append(".ED_WH;1");

        for (byte i = 0; i < rank; ++i)
        {
          line = drawer.readLine();
          reagent = drawer.readLine();
        }

        while (true)
        {
          token = drawer.readWord();
          if (token == null)
            break;
          if (token.equals("CScond"))
          {
            line = drawer.readLine();
            line = drawer.readLine();
            line = drawer.readLine();
            line = drawer.readLine();
          }
          else
            throw new DeskException("bad format");
        }
        drawer.readLine();
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      drawer.readToEnd();
    }

    return data_files;
  }

  static void convert_drawer(
      File envelope,
      File xml)
  {
    DeskInputStream drawer = null;
    String line, token, username = null;
    String title = null, cytometer = null, longName = null;
    String coord, sample, reagent, sensor, range, scale;
    Date date = null;
    float low, high;
    int warehouse, rank, count, protocol = 0, archive_id = 0;
    int number_of_samples = 0;
    byte type, bits;

    System.out.println("convert: " + envelope);

    Document doc = null;
    try
    {
      doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().
          newDocument();
    }
    catch (FactoryConfigurationError ex1)
    {
    }
    catch (ParserConfigurationException ex1)
    {
    }

//    DocumentImpl  doc = new org.apache.xerces.dom.DocumentImpl();
//    DocumentType doc_type = doc.createDocumentType(
//      "facs:session", "facs-session.dtd",
//      "http://"+xml_host+"/XML/facs-session.dtd" );
//    doc.appendChild( doc_type );

    List axes = new ArrayList(20);
    List samples = new ArrayList(100);
    Map detectors_seen = new HashMap(20);
    Map labels_seen = new HashMap(20);
    Map formats_seen = new HashMap(20);
    try
    {
      drawer = new DeskInputStream(envelope);
      line = drawer.readLine();
      if (!line.equals("CSdata V1"))
        throw new DeskException("bad format");

      while (true)
      {
        token = drawer.readWord();
        if (token == null)
          break;
        if (token.equals("title"))
          title = drawer.readLine();
        else if (token.equals("protocol"))
        {
          protocol = Integer.parseInt(drawer.readWord());
          cytometer = drawer.readLine();
          if (cytometer.equals("FacStar"))
            cytometer = "FACStar";
        }
        else if (token.equals("author"))
          longName = drawer.readLine();
        else if (token.equals("date"))
        {
          line = drawer.readLine();
          int yr = Integer.parseInt(line.substring(0, 2));
          if (yr <= 50)
            yr += 2000;
          else
            yr += 1900;
          int mo = Integer.parseInt(line.substring(2, 4)) - 1;
          int dy = Integer.parseInt(line.substring(4, 6));
          date = new GregorianCalendar(yr, mo, dy).getTime();
        }
        else if (token.equals("archive"))
          archive_id = Integer.parseInt(drawer.readLine());
        else
          line = drawer.readLine();
      }
      drawer.readLine();

      username = desk_context.getUID(longName);
      if (FacsDesk.UNKNOWN_USER.equals(username))
      {
        System.out.println("User unknown: " + longName);
        drawer.readToEnd();
        return;
      }
      String dn = "session=" + protocol
          + ",instrument=" + cytometer
          + ",uid=" + username + ","
          + eDesk.institution();

      Element root = doc.createElement("facs:session");
      root.setAttribute("xmlns:facs", "http://FACS.Stanford.EDU/XML");
      root.setAttribute("xmlns:XDF", "http://FACS.Stanford.EDU/XML/XDF-Bogus");
      root.setAttribute("facs:sessionID", Integer.toString(protocol));
      root.setAttribute("facs:DN", dn);
      root.setAttribute("facs:start-time", timestamp.format(date));
      doc.appendChild(root);

      Element e = doc.createElement("facs:title");
      e.appendChild(doc.createTextNode(title));
      root.appendChild(e);

      e = doc.createElement("facs:investigator");
      e.appendChild(doc.createTextNode(longName));
      e.setAttribute("facs:DN",
          "uid=" + username + "," + eDesk.institution());
      root.appendChild(e);

      e = doc.createElement("facs:instrument");
      e.setAttribute("facs:DN",
          "instrument=" + cytometer + "," + eDesk.institution());
      e.setAttribute("facs:sequence", Integer.toString(protocol));
      e.appendChild(doc.createTextNode(cytometer));
      root.appendChild(e);

      while (true)
      {
        token = drawer.readWord();
        if (token == null)
          break;
        if (!token.equals("CSampl"))
          throw new DeskException("bad format");
        drawer.readLine();

        coord = drawer.readWord();
        int cp = coord.indexOf('<');
        if (cp > 0)
          coord = coord.substring(0, cp);
        warehouse = Integer.parseInt(drawer.readWord());
        rank = Integer.parseInt(drawer.readWord());
        count = Integer.parseInt(drawer.readWord());
        drawer.readLine();

        sample = drawer.readLine();

        ++number_of_samples;

        Element facs_sample = doc.createElement("facs:sample");
        facs_sample.setAttribute("facs:ID", coord);
        facs_sample.setAttribute("facs:event-count", Integer.toString(count));
        facs_sample.setAttribute("facs:start-time", timestamp.format(date));
        samples.add(facs_sample);

        Element desc = doc.createElement("facs:description");
        desc.appendChild(doc.createTextNode(sample));
        facs_sample.appendChild(desc);

        StringBuffer url = new StringBuffer(100);
        url.append("ftp://cayley.stanford.edu/[");
        url.append((warehouse / 36) % 245);
        url.append("]");
        for (int w = warehouse; w > 0; w /= 36)
          url.append("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(w % 36));
        url.append(".ED_WH;1");
        Element data_url = doc.createElement("facs:data-url");
        data_url.appendChild(doc.createTextNode(url.toString()));
        facs_sample.appendChild(data_url);

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        MessageDigest sha = MessageDigest.getInstance("SHA");
        FileInputStream fis = new FileInputStream(new File(envelope.getParentFile(),
            coord + ".ED_WH"));
        byte[] buffer = new byte[16384];
        while (true)
        {
          int n = fis.read(buffer);
          if (n < 0)
            break;
          md5.update(buffer, 0, n);
          sha.update(buffer, 0, n);
        }
        fis.close();
        /*
         byte[]  digest = md5.digest();
         MimeBase64Encoder  encoder = new MimeBase64Encoder();

         ByteBuf  in = new ByteBuf( digest, 0, digest.length );
         ByteBuf  out = new ByteBuf( (3 * digest.length + 1) / 2 );
         encoder.translate( in, out );
         encoder.eof( out );
         String  base64 = out.toString();

         digest = sha.digest();
         in = new ByteBuf( digest, 0, digest.length );
         out = new ByteBuf( (3 * digest.length + 1) / 2 );
         encoder.translate( in, out );
         encoder.eof( out );
         base64 = out.toString();
         */

        List detectors = new ArrayList(rank);
        List labels = new ArrayList(rank);
        List format = new ArrayList(rank);
        for (byte i = 0; i < rank; ++i)
        {
          Element channel = doc.createElement("facs:channel");

          sensor = drawer.readWord();
          bits = (byte)Integer.valueOf(drawer.readWord()).intValue();
          range = drawer.readWord();
          line = drawer.readLine();
          if (range == null)
          {
            low = (float)0;
            high = (float)(1 << bits);
            range = Float.toString(low) + "," + Float.toString(high);
            scale = "linear";
          }
          else
          {
            StringTokenizer st = new StringTokenizer(range, ",");
            low = Float.valueOf(st.nextToken()).floatValue();
            high = Float.valueOf(st.nextToken()).floatValue();
            if (line.length() == 0)
              scale = "linear";
            else
            {
              scale = "log";
              channel.setAttribute("facs:decades",
                  String.valueOf((Math.log(high) - Math.log(low)) / Math.log(10.0)));
            }
          }
          reagent = drawer.readLine();

          channel.setAttribute("facs:sensor", sensor);
          channel.setAttribute("facs:bits", Integer.toString(bits));
          channel.setAttribute("facs:card", Integer.toString(1 << bits));
          channel.setAttribute("facs:range", range);
          channel.setAttribute("facs:scale", scale);
          facs_sample.appendChild(channel);
          channel.appendChild(doc.createTextNode(reagent));

          detectors.add(sensor);
          String label = eDesk.getProperty(
              "desk.sensor." + sensor + ".label");
          if (reagent != null && reagent.length() > 0)
            label = reagent + " (" + label + ")";
          labels.add(label);
          format.add(new Integer(bits));
        }

        Element xdf_array = doc.createElement("XDF:array");
        facs_sample.appendChild(xdf_array);
        String detector_axis = (String)detectors_seen.get(detectors);
        if (detector_axis == null)
        {
          axes.add(detectors);
          detector_axis = "axis-" + axes.size();
          detectors_seen.put(detectors, detector_axis);
        }
        /*
         e = doc.createElement( "XDF:alternateAxis" );
         e.setAttribute( "XDF:name", "detector" );
         e.setAttribute( "XDF:axisIDRef", detector_axis );
         xdf_axis.appendChild( e );
         */
        String label_axis = (String)labels_seen.get(labels);
        if (label_axis == null)
        {
          axes.add(labels);
          label_axis = "axis-" + axes.size();
          labels_seen.put(labels, label_axis);
        }
        Element xdf_axis = doc.createElement("XDF:axis");
        xdf_axis.setAttribute("XDF:name", "measurement");
        xdf_axis.setAttribute("XDF:axisID", label_axis);
        xdf_array.appendChild(xdf_axis);
        /*
         e = doc.createElement( "XDF:alternateAxis" );
         e.setAttribute( "XDF:name", "label" );
         e.setAttribute( "XDF:axisIDRef", label_axis );
         xdf_axis.appendChild( e );
         */
        String events_axis = "events-" + number_of_samples;
        xdf_axis = doc.createElement("XDF:axis");
        xdf_axis.setAttribute("XDF:name", "event");
        xdf_axis.setAttribute("XDF:axisID", events_axis);
        xdf_array.appendChild(xdf_axis);

        e = doc.createElement("XDF:values");
        e.setAttribute("XDF:size", Integer.toString(count));
        xdf_axis.appendChild(e);

        String format_ref = (String)formats_seen.get(format);
        if (format_ref == null)
        {
          format_ref = "format-" + Integer.toString(formats_seen.size() + 1);
          formats_seen.put(format, format_ref);
        }
        Element xdf_read1 = doc.createElement("XDF:read");
        xdf_read1.setAttribute("XDF:axisIDref", events_axis);
        xdf_array.appendChild(xdf_read1);
        Element xdf_read2 = doc.createElement("XDF:read");
        xdf_read2.setAttribute("XDF:axisIDref", label_axis);
        xdf_read1.appendChild(xdf_read2);
        e = doc.createElement("XDF:binaryFormat");
        e.setAttribute("XDF:formatIDRef", format_ref);
        xdf_read2.appendChild(e);

        e = doc.createElement("XDF:data");
        e.setAttribute("XDF:href", url.toString());
        e.setAttribute("XDF:offset", Integer.toString(432));
        e.setAttribute("XDF:type", "application/org.ISAC-FCS");
        xdf_array.appendChild(e);

        e = doc.createElement("facs:fcs-text");
        e.setAttribute("facs:keyword", "#SITE");
        e.setAttribute("facs:value", "Darwin");
        facs_sample.appendChild(e);
        e = doc.createElement("facs:fcs-text");
        e.setAttribute("facs:keyword", "#EXPID");
        e.setAttribute("facs:value", Integer.toString(protocol));
        facs_sample.appendChild(e);
        e = doc.createElement("facs:fcs-text");
        e.setAttribute("facs:keyword", "#SMPID");
        e.setAttribute("facs:value", Integer.toString(warehouse));
        facs_sample.appendChild(e);

        while (true)
        {
          token = drawer.readWord();
          if (token == null)
            break;
          if (token.equals("CScond"))
          {
            line = drawer.readLine();
            line = drawer.readLine();
            line = drawer.readLine();
            line = drawer.readLine();
          }
          else
            throw new DeskException("bad format");
        }
        drawer.readLine();
      }

      Iterator i = axes.iterator();
      for (int n = 1; i.hasNext(); ++n)
      {
        List list = (List)i.next();
        Element xdf_axis = doc.createElement("XDF:axis");
        xdf_axis.setAttribute("XDF:axisID", "axis-" + n);
        root.appendChild(xdf_axis);

        Element xdf_values = doc.createElement("XDF:values");
        xdf_values.setAttribute("XDF:size", Integer.toString(list.size()));
        xdf_axis.appendChild(xdf_values);
        for (int m = 0; m < list.size(); ++m)
        {
          e = doc.createElement("XDF:component");
          e.appendChild(doc.createTextNode((String)list.get(m)));
          xdf_values.appendChild(e);
        }
      }

      i = formats_seen.keySet().iterator();
      for (int n = 1; i.hasNext(); ++n)
      {
        Element format = doc.createElement("XDF:binaryFormat");
        format.setAttribute("XDF:endian", "LittleEndian");
        format.setAttribute("XDF:formatID", "format-" + n);
        Iterator j = ((List)i.next()).iterator();
        while (j.hasNext())
        {
          e = doc.createElement("XDF:binary");
          e.setAttribute("XDF:bits", ((Integer)j.next()).toString());
          e.setAttribute("XDF:signed", "no");
          e.setAttribute("XDF:type", "integer");
          format.appendChild(e);
        }
        root.appendChild(format);
      }

      i = samples.iterator();
      while (i.hasNext())
        root.appendChild((Element)i.next());

      doc.getDocumentElement().normalize();

//      OutputFormat of = new OutputFormat( doc );
//      of.setIndenting(true);
//      of.setIndent( 2 );
//      FileOutputStream  os = new FileOutputStream( xml );
//      XMLSerializer xmls = new XMLSerializer( os, of );
//      xmls.serialize( doc );
//      os.close();

      Transformer xfrm = TransformerFactory.newInstance().newTransformer();
      DOMSource dsrc = new DOMSource(doc);
      StreamResult sres = new StreamResult(xml);
      xfrm.transform(dsrc, sres);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      drawer.readToEnd();
    }
  }

  static void convert_drawers()
  {
    File remember = new File(eDesk.getFileRoot(), "cataloged.dat");
    if (remember.exists())
    {
      try
      {
        ObjectInputStream ois =
            new ObjectInputStream(
            new FileInputStream(remember));
        already_cataloged = (BitSet)ois.readObject();
        ois.close();
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
        return;
      }
    }
    else
      already_cataloged = new BitSet();

    Stack toDo = new Stack();
    File root = eDesk.getFileRoot();
    Iterator authors = desk_context.getAuthors().iterator();
    while (authors.hasNext())
    {
      String author = (String)authors.next();
      if (user_list != null && !user_list.contains(desk_context.getUID(author)))
        continue;
      File f = new File(root, author);
      if (f.exists() && f.isDirectory())
        toDo.push(f);
    }

    while (!toDo.empty() && !time_up())
    {
      File dir = (File)toDo.pop();
      String files[] = dir.list();
      for (int i = 0; i < files.length; ++i)
      {
        String fn = files[i];
        File f = new File(dir, fn);
        if (fn.endsWith(".ED_ENV"))
        {
          String s = fn.substring(0, fn.length() - 7);
          File xml = new File(dir, s + ".xml");
          if (!xml.exists() || xml.length() == 0 || f.lastModified() > xml.lastModified())
          {
            if (!fetch_data(f))
              continue;
            convert_drawer(f, xml);
            already_cataloged.clear(Integer.parseInt(s));
            if (time_up())
              break;
          }
        }
        else
        {
          if (f.isDirectory())
            toDo.push(f);
        }
      }
    }

    try
    {
      ObjectOutputStream oos =
          new ObjectOutputStream(
          new FileOutputStream(remember));
      oos.writeObject(already_cataloged);
      oos.close();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
  }

  /*
    static void update_html ()
    throws SAXException, IOException
    {
      DOMParser  parser = new DOMParser();
      XercesLiaison  xerces = new XercesLiaison();
      XSLTProcessor  xalan = XSLTProcessorFactory.getProcessor( xerces );
      StylesheetRoot style_sheet = xalan.processStylesheet( "http://"+eDesk.getHttpHost()+"/XML/facs-session.xsl" );

      Stack  toDo = new Stack();
      File  root = eDesk.getFileRoot();

      if (user_list == null)
      {
        String  users[] = root.list();
        for (int  i = 0;  i < users.length;  ++i)
        {
   File f = new File( root, users[i] );
   if (f.exists() && f.isDirectory())
     toDo.push( f );
        }
      }
      else
      {
        Enumeration  e = user_list.elements();
        while (e.hasMoreElements())
        {
   String  user = (String) e.nextElement();
   File f = new File( root, user );
   if (f.exists() && f.isDirectory())
     toDo.push( f );
        }
      }

      while (!toDo.empty() && !time_up())
      {
        File  dir = (File) toDo.pop();
        String  files[] = dir.list();
        for (int  i = 0;  i < files.length;  ++i)
        {
   String  fn = files[i];
   File  f = new File( dir, fn );
   if (fn.endsWith( ".xml" ))
   {
     String  s = fn.substring( 0, fn.length() - 4 );
     File  html = new File( dir, s + ".html" );
     if (!html.exists() || html.length() == 0 || f.lastModified() > html.lastModified())
     {
       if (html.exists())
         html.delete();
       System.out.println( html.toString() );
       style_sheet.process(
         new XSLTInputSource( new FileInputStream( f ) ),
         new XSLTResultTarget( new FileOutputStream( html ) ) );
       if (time_up())
         break;
     }
   }
   else
   {
     if (f.isDirectory())
       toDo.push( f );
   }
        }
      }
    }
   */
  static void update_catalog()
      throws NamingException
  {
    File remember = new File(eDesk.getFileRoot(), "cataloged.dat");
    if (remember.exists())
    {
      try
      {
        ObjectInputStream ois =
            new ObjectInputStream(
            new FileInputStream(remember));
        already_cataloged = (BitSet)ois.readObject();
        ois.close();
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
        return;
      }
    }
    else
      already_cataloged = new BitSet();

      //parser = new org.apache.xerces.parsers.SAXParser();
      //parser.setDocumentHandler( new CatalogSession( archive ) );

//    try
//    {
//      catalog_agent = new CatalogAgentImpl();
//    }
//    catch (Exception  ex)
//    {
//      ex.printStackTrace();
//      return;
//    }

    if (user_list == null)
    {
      String users[] = eDesk.getFileRoot().list();
      for (int i = 0; i < users.length; ++i)
      {
//	catalog_user( users[i] );
        if (time_up())
          break;
      }
    }
    else
    {
      Iterator users = desk_context.getUsers().iterator();
      while (users.hasNext())
      {
//	catalog_user( (String) users.next() );
        if (time_up())
          break;
      }
    }

    try
    {
      ObjectOutputStream oos =
          new ObjectOutputStream(
          new FileOutputStream(remember));
      oos.writeObject(already_cataloged);
      oos.close();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
  }

//  static void catalog_user (
//    String  user )
//  throws NamingException
//  {
//    File user_dir = new File( eDesk.getFileRoot(), user );
//    if (!user_dir.exists() || !user_dir.isDirectory())
//      return;
//
//    String  instruments[] = user_dir.list();
//    for (int  i = 0;  i < instruments.length;  ++i)
//    {
//      String  instrument = instruments[i];
//      File  instrument_dir = new File( user_dir, instrument );
//      if (!instrument_dir.exists() || !instrument_dir.isDirectory())
//	continue;
//      String  sessions[] = instrument_dir.list();
//      for (int  j = 0;  j < sessions.length;  ++j)
//      {
//	String  session_file = sessions[j];
//	if (!session_file.endsWith( ".xml" ))
//	  continue;
//
//	try
//	{
//	  String  session = session_file.substring( 0, session_file.length() - ".xml".length() );
//	  int  p = Integer.parseInt( session );
//	  if (already_cataloged.get( p ))
//	    continue;
//
//	  String  name =
//	    "instrument="+instrument
//	    +",uid="+user+","
//	    +eDesk.institution();
//	  System.out.println( name );
//
//	  String  url = "http://FACS.Stanford.EDU/"+instrument+"/";
//	  System.out.println( url );
//
//	  catalog_agent.addInstrument(
//	    name,
//	    "instrument="+instrument+","+eDesk.institution(),
//	    url );
//
//	  name =
//	    "session="+session
//	    +",instrument="+instrument
//	    +",uid="+user+","
//	    +eDesk.institution();
//	  System.out.println( name );
//
//	  url = "http://"+eDesk.getHttpHost()+"/EDesk/"
//	    +user+"/"+instrument+"/"+session_file;
//	  System.out.println( url );
//
//	  catalog_agent.addDocument( name, url );
//	  if (catalog_agent.validateDocument( name ) )
//	    already_cataloged.set( p );
//	  else
//	    System.out.println( "Document did not validate" );
//	}
//	catch (IOException  ex)
//	{
//	  ex.printStackTrace();
//	}
//	catch (CatalogException  ex)
//	{
//	  ex.printStackTrace();
//	}
//	if (time_up())
//	  return;
//      }
//    }
//  }

  private static class copy_one
  {
    int protocol, envelope;
    boolean collecting;

    copy_one(
        int protocol,
        int envelope,
        boolean collecting)
    {
      this.protocol = protocol;
      this.envelope = envelope;
      this.collecting = collecting;
    }
  }

  private static class copy_data
  {
    String warehouse;
    File file;

    copy_data(
        String warehouse,
        File file)
    {
      this.warehouse = warehouse;
      this.file = file;
    }
  }

  static void update_desktop(
      String user)
  {
    String author = null, cytometer = null, title = null;
    int protocol = 0, archive_id, envelope;
    Date date = null;
    boolean collecting;
    Stack toDo = new Stack();
    DeskInputStream desktop;

    int checkpoint = desk_context.getCheckpoint(user);
    if (checkpoint == 0 || desk_context.isEnvelopeSeen(checkpoint))
      return;
    System.out.println(user);

    try
    {
      desktop = eDesk.get(FacsDesk.envelope(checkpoint));
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      return;
    }

    try
    {
      while (true)
      {
        String token = desktop.readWord();
        if (token == null)
          break;
        if (token.equals("CSdata"))
        {
          archive_id = 0;
          envelope = 0;
          desktop.readWord(); // version
          desktop.readWord(); // place marks
          collecting = (desktop.readLine().length() > 0);
          while (true)
          {
            token = desktop.readWord();
            if (token == null)
              break;
            if (token.equals("title"))
              title = desktop.readLine();
            else if (token.equals("author"))
              author = desktop.readLine();
            else if (token.equals("protocol"))
            {
              protocol = desktop.readInt();
              cytometer = desktop.readLine();
              if (cytometer.equalsIgnoreCase("FACStar"))
                cytometer = "FACStar";
            }
            else if (token.equals("archive"))
              archive_id = Integer.parseInt(
                  desktop.readLine());
            else if (token.equals("date"))
            {
              String line = desktop.readLine();
              int yr = Integer.parseInt(line.substring(0, 2)) + 1900;
              int mo = Integer.parseInt(line.substring(2, 4)) - 1;
              int dy = Integer.parseInt(line.substring(4, 6));
              date = new GregorianCalendar(yr, mo, dy).getTime();
            }
            else if (token.equals("envelope"))
              envelope = Integer.parseInt(
                  desktop.readLine());
            else
              desktop.readLine();
          }

          if (!collecting && envelope != 0 && !desk_context.isEnvelopeSeen(envelope))
          {
            if (!eDesk.getCacheProtocol(protocol).exists())
              toDo.push(new copy_one(protocol, envelope, collecting));
          }
        }
        else
          while (true)
          {
            token = desktop.readWord();
            if (token == null)
              break;
            desktop.readLine();
          }

        desktop.readLine();
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
      return;
    }
    finally
    {
      desktop.readToEnd();
    }

    try
    {
      while (!toDo.empty())
      {
        copy_one one = (copy_one)toDo.pop();

        File cache = eDesk.getCacheProtocol(one.protocol);
        eDesk.copy(eDesk.envelope(one.envelope), cache);

        desk_context.setWarehouseReferences(DataDrawer.parse(cache));
        desk_context.setEnvelopeSeen(one.envelope);

        if (time_up())
          return;
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  static String update_unarchived(
      String user,
      DeskInputStream desktop)
      throws IOException
  {
    String author = null, cytometer = null, title = null;
    int protocol = 0, archive_id, envelope;
    Date date = null;
    boolean collecting;
    Stack toDo = new Stack();
    int unarchived = 0;

    try
    {
      while (true)
      {
        String token = desktop.readWord();
        if (token == null)
          break;
        if (token.equals("CSdata"))
        {
          archive_id = 0;
          envelope = 0;
          desktop.readWord(); // version
          desktop.readWord(); // place marks
          collecting = (desktop.readLine().length() > 0);
          while (true)
          {
            token = desktop.readWord();
            if (token == null)
              break;
            if (token.equals("title"))
              title = desktop.readLine();
            else if (token.equals("author"))
              author = desktop.readLine();
            else if (token.equals("protocol"))
            {
              protocol = desktop.readInt();
              cytometer = desktop.readLine();
              if (cytometer.equalsIgnoreCase("FACStar"))
                cytometer = "FACStar";
            }
            else if (token.equals("archive"))
              archive_id = Integer.parseInt(
                  desktop.readLine());
            else if (token.equals("date"))
            {
              String line = desktop.readLine();
              int yr = Integer.parseInt(line.substring(0, 2)) + 1900;
              int mo = Integer.parseInt(line.substring(2, 4)) - 1;
              int dy = Integer.parseInt(line.substring(4, 6));
              date = new GregorianCalendar(yr, mo, dy).getTime();
            }
            else if (token.equals("envelope"))
              envelope = Integer.parseInt(
                  desktop.readLine());
            else
              desktop.readLine();
          }
          if (archive_id == 0 && envelope != 0)
            toDo.push(new copy_one(protocol, envelope, collecting));
        }
        else
          while (true)
          {
            token = desktop.readWord();
            if (token == null)
              break;
            desktop.readLine();
          }

        desktop.readLine();
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
    finally
    {
      desktop.readToEnd();
    }

    try
    {
      while (!toDo.empty())
      {
        copy_one one = (copy_one)toDo.pop();

        File cache = eDesk.getCacheProtocol(one.protocol);
        eDesk.copy(eDesk.envelope(one.envelope), cache);
        if (count_data(cache) > 0)
          ++unarchived;
        else
          System.out.println(cache.getName() + " has no data");
      }
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }

    if (unarchived == 0)
      return null;

    StringBuffer sb = new StringBuffer();
    sb.append(user);
    while (sb.length() < 20)
      sb.append(' ');
    sb.append(unarchived);

    return sb.toString();
  }

  static void update_unarchived(
      Collection users)
  {
    try
    {
      ArrayList report = new ArrayList();

      Iterator i = users.iterator();
      while (i.hasNext())
      {
        FacsDesk.User user = (FacsDesk.User)i.next();
        File checkpoint = eDesk.getCacheDesktop(user.uid);
        if (user.checkpoint != user.checkpoint_seen)
          eDesk.copy(eDesk.envelope(user.checkpoint), checkpoint, true);
        user.checkpoint_seen = user.checkpoint;

        DeskInputStream desktop = new DeskInputStream(checkpoint);
        String unarchived = update_unarchived(user.uid, desktop);
        desktop.close();
        if (unarchived != null)
          report.add(unarchived);
        if (time_up())
          break;
      }

      Collections.sort(report);
      PrintWriter pw = new PrintWriter( new FileOutputStream(eDesk.getSiteFile("unarchived.txt")));
      i = report.iterator();
      while (i.hasNext())
        pw.println(i.next());
      pw.close();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }

  static void update_desktops(
      Collection users)
  {
    Iterator i = users.iterator();
    while (i.hasNext())
    {
      String user = (String)i.next();
      update_desktop(user);
      if (time_up())
        break;
    }
  }

  static void update_journal()
  {
    try
    {
      DeskInputStream journal = eDesk.get("ED_CONTEXT:MIRROR.ED");
      int[] tapeIndex = new int[3000];
      ;

      while (true)
      {
        String token = journal.readWord();
        if (token == null)
          break;
        if (token.equals("checkpoint"))
        {
          FacsDesk.User user = eDesk.getUserByUID(journal.readWord());
          user.checkpoint = journal.readInt();
        }
        else if (token.equals("tape"))
        {
          String tape = journal.readWord();
          if (tape.startsWith("WH"))
            tape = tape.substring(2, 5);
          else
            tape = tape.substring(1, 5);
          int volume = Integer.parseInt(tape);
          int index = journal.readInt();
          if (volume >= tapeIndex.length)
          {
            int[] new_tapes = new int[2 * volume];
            System.arraycopy(tapeIndex, 0, new_tapes, 0, tapeIndex.length);
            tapeIndex = new_tapes;
          }
          tapeIndex[volume] = index;
//          desk_context.setTapeIndex( volume, index );
        }
        journal.readLine();
      }
      journal.readLine();
      if (!journal.readLine().equals("Journal End"))
        throw new DeskException("bad journal format");
      journal.readLine();

      eDesk.setSiteObject(eDesk.TAPE_INDEX, tapeIndex);
    }
    catch (Exception ex)
    {
      ex.printStackTrace(System.out);
    }
  }

  public static void main(
      String args[])
  {
    DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
    Warehouse warehouse = null;
    String phases;

    try
    {
      eDesk = new FacsDesk(args[0]);
//      desk_context = DeskContext.getContext( args[0] );
//      eDesk = desk_context.getEDesk();

//      archive = new InitialDirContext( eDesk.getProperties() );
//      institution = (DirContext) archive.lookup( eDesk.institution() );

      if (args.length > 3)
      {
        stop = Calendar.getInstance();
        stop.add(Calendar.MINUTE, Integer.parseInt(args[3]));
      }

      if (args.length > 2 && !args[2].equals("*"))
      {
        user_list = new ArrayList();
        StringTokenizer st = new StringTokenizer(args[2], ",");
        while (st.hasMoreTokens())
          user_list.add(eDesk.getUserByUID(st.nextToken().toUpperCase()));
      }

      if (args.length < 2 || args[1].equals("*"))
        phases = "journal,users,desktops,warehouse";
      else
        phases = args[1];
    }
    catch (Exception ex)
    {
      ex.printStackTrace(System.out);
      return;
    }

    StringTokenizer st = new StringTokenizer(phases, ",");
    while (st.hasMoreTokens())
    {
      String phase = st.nextToken().toUpperCase();
      System.out.println("Starting phase: " + phase);
      System.out.println(df.format(new Date()));

      try
      {
        if (phase.equalsIgnoreCase("journal"))
        {
          update_journal();
        }
        else if (phase.equalsIgnoreCase("users"))
        {
          update_users();
        }
        else if (phase.equalsIgnoreCase("desktops"))
        {
          if (user_list == null)
            update_desktops(desk_context.getUsers());
          else
            update_desktops(user_list);
        }
        else if (phase.equalsIgnoreCase("unarchived"))
        {
          if (user_list == null)
            update_unarchived(eDesk.getUsers());
          else
            update_unarchived(user_list);
        }
        else if (phase.equalsIgnoreCase("warehouse"))
        {
          if (warehouse == null)
            warehouse = new Warehouse(eDesk);
          warehouse.update_pending_faults();
        }
        else if (phase.equalsIgnoreCase("tapes"))
        {
          if (warehouse == null)
            warehouse = new Warehouse(eDesk);
          warehouse.import_tapes();
        }
        else if (phase.equalsIgnoreCase("requests"))
        {
          if (warehouse == null)
            warehouse = new Warehouse(eDesk);
          warehouse.update_tape_requests();
        }
        else if (phase.equalsIgnoreCase("convert"))
        {
          convert_drawers();
        }
        else if (phase.equalsIgnoreCase("catalog"))
        {
          update_catalog();
        }
        else if (phase.equalsIgnoreCase("publish"))
        {
          //  update_html();
        }
        else
        {
          throw new DeskException("no such phase: " + phase);
        }
      }
      catch (Exception ex)
      {
        ex.printStackTrace();
        return;
      }
      if (time_up())
      {
        System.out.println("Time is up!");
        break;
      }
    }

    try
    {
      eDesk.saveUsers();
//      eDesk.saveAuthors();
      eDesk.writeInvestigators();
      eDesk.close();
//      desk_context.commit();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }

    System.out.println("Finished: ");
    System.out.println(df.format(new Date()));
  }
}
