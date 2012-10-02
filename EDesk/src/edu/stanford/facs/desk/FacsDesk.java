package edu.stanford.facs.desk;

import java.io.*;
import java.util.*;
//import sun.net.ftp.FtpClient;

/**
 * Identifies FACS.Desk sites and files
 * @version 1.0 -- August 1997
 * @author Wayne A. Moore
 * <BR> &copy; 1997 by The Board of Trustees of Leland Stanford Junior University
 */

public class FacsDesk
{
  public final static String UNKNOWN_USER = "@UNKNOWN@";
  public final static String USERS = "users.dat";
  public final static String AUTHORS = "USERS.ED";
  public final static String ALTERNATE = "ALTUSERS.ED";
  public final static String TAPE_INDEX = "tapes.dat";
  public final static String PENDING_FAULTS = "faults.dat";
  public final static String CHECKPOINT_SEEN = "checkpoint.dat";
  public final static String FAULT_LOG = "faults.log";

  private static Set users;
  private static Map authors;
  private static Map uid_to_user;
  private static Map name_to_uid;
  private static Map alt_to_uid;
  private static Properties userNames;
  private static File faultLog;

  private File edeskRoot;
  private int edeskSite;
  private Properties properties;
  public int tapeIndex[];
  public int tapeVolumes = 0;
  private String site, institution;
//  public FtpClient ftpc;
  private File context_file;
  private DeskContext context;
  private int cache_size = 255;

  public static class User
      implements Serializable
  {
    public final String uid;
    public transient Set longNames;
    int checkpoint;
    int checkpoint_seen;

    User (
        String uid,
        String longName)
    {
      this.uid = uid.toUpperCase();
      longNames = new HashSet();
      if (longName != null)
        longNames.add(longName);
    }

    User (
        String uid)
    {
      this(uid, null);
    }
  }

  public static class WarehouseFault
      implements Serializable
  {
    public int warehouse;
    public long timestamp;
    public String user;
  }

  public void readInvestigators (DeskInputStream dis, Map map)
      throws IOException
  {
    while (true)
    {
      String uid = dis.readWord();
      if (uid == null)
        break;
      uid = uid.toUpperCase();

      String long_name = dis.readLine().trim();
      map.put(long_name, uid);
    }
    dis.readToEnd();
  }

  public void readInvestigators ()
      throws IOException
  {
    if (name_to_uid != null)
      return;

    name_to_uid = new HashMap();
    alt_to_uid = new HashMap();

    File f = getSiteFile(ALTERNATE);
    if (f.exists())
    {
      DeskInputStream dis = new DeskInputStream(f);
      readInvestigators(dis, alt_to_uid);
      dis.close();
    }

    f = getSiteFile(AUTHORS);
    if (f.exists())
    {
      DeskInputStream dis = new DeskInputStream(f);
      readInvestigators(dis, name_to_uid);
      dis.close();
    }
  }

  private void writeInvestigators (String file, Map map)
      throws IOException
  {
    StringBuffer sb = new StringBuffer(80);

    Iterator entries = map.entrySet().iterator();
    List lines = new ArrayList();
    while (entries.hasNext())
    {
      Map.Entry entry = (Map.Entry)entries.next();
      sb.setLength(0);
      sb.append((String)entry.getValue());
      while (sb.length() < 13)
        sb.append(' ');
      sb.append((String)entry.getKey());
      lines.add(sb.toString());
    }
    Collections.sort(lines);

    PrintWriter pw = new PrintWriter(new FileWriter(getSiteFile(file)));
    Iterator line = lines.iterator();
    while (line.hasNext())
    {
      String l = (String)line.next();
      if (l.startsWith(FacsDesk.UNKNOWN_USER))
        System.out.println(l);
      pw.println(l);
    }
    pw.close();
  }

  public void writeInvestigators ()
      throws IOException
  {
    if (name_to_uid == null)
      return;

    writeInvestigators(AUTHORS, name_to_uid);
    writeInvestigators(ALTERNATE, alt_to_uid);
  }

  public void addInvestigator (String uid, String name)
      throws IOException
  {
    readInvestigators();
    if (name_to_uid.get(name) == null && alt_to_uid.get(name) == null)
      alt_to_uid.put(name, uid);
  }

  public Set getUsers ()

  {

    if (users == null)

      users = (HashSet)this.getSiteObject(USERS);

    if (users == null)

      users = new HashSet();

    return users;

  }

  public void saveUsers ()

      throws IOException

  {

    if (users == null)

      return;

    setSiteObject(USERS, users);

  }

  public Map getAuthors ()

  {

    if (authors == null)

    {

      authors = new HashMap();

      try

      {

        File f = getSiteFile("USERS.ED");

        if (f.exists())

        {

          DeskInputStream users = new DeskInputStream(f);

          while (true)

          {

            String uid = users.readWord();

            if (uid == null)

              break;

            String author = users.readLine().trim();

            User user = getUserByUID(uid);

            if (user.longNames == null)

              user.longNames = new HashSet();

            user.longNames.add(author);

            authors.put(author, user);

          }

          users.readToEnd();

        }

      }

      catch (IOException ex)

      {

        ex.printStackTrace();

      }

    }

    return authors;

  }

  public void saveAuthors ()

      throws IOException

  {

    if (authors == null)

      return;

    StringBuffer sb = new StringBuffer(80);

    Iterator entries = authors.entrySet().iterator();

    List lines = new ArrayList();

    while (entries.hasNext())

    {

      Map.Entry entry = (Map.Entry)entries.next();

      sb.setLength(0);

      sb.append(((User)entry.getValue()).uid);

      while (sb.length() < 13)

        sb.append(' ');

      sb.append(entry.getKey());

      lines.add(sb.toString());

    }

    Collections.sort(lines);

    PrintWriter pw = new PrintWriter(new FileWriter(getSiteFile(AUTHORS)));

    Iterator line = lines.iterator();

    while (line.hasNext())

    {

      String l = (String)line.next();

      if (l.startsWith(FacsDesk.UNKNOWN_USER))

        System.out.println(l);

      pw.println(l);

    }

    pw.close();

  }

  public String getUID (
      String author)
      throws IOException
  {
    if (name_to_uid == null)
      readInvestigators();

    String uid = (String)name_to_uid.get(author);
    if (uid == null)
      uid = (String)alt_to_uid.get(author);
    if (uid == null)
    {
      alt_to_uid.put(author, UNKNOWN_USER);
      return UNKNOWN_USER;
    }

    return uid;
  }

  public User getUserByUID (

      String uid)

  {

    User user;

    if (uid_to_user == null)

    {

      uid_to_user = new HashMap();

      Iterator it = getUsers().iterator();

      while (it.hasNext())

      {

        user = (User)it.next();

        uid_to_user.put(user.uid, user);

      }

    }

    user = (User)uid_to_user.get(uid.toUpperCase());

    if (user == null)

    {

      user = new User(uid);

      getUsers().add(user);

      if (uid_to_user != null)

        uid_to_user.put(user.uid, user);

    }

    return user;

  }

  public User getUserByName (

      String longName)

  {

    User user = (User)getAuthors().get(longName);

    if (user == null)

    {

      user = getUserByUID(UNKNOWN_USER);

      if (user.longNames == null)

        user.longNames = new HashSet();

      user.longNames.add(longName);

      authors.put(longName, user);

    }

    return user;

  }

  public File getCacheData (
      int id)
  {
    StringBuffer sb = new StringBuffer(20);
    sb.append(radix36((id / (36 * 35)) % 35));
    sb.append(File.separator);
    sb.append(radix36((id / 36) % 35));
    sb.append(File.separator);
    sb.append(radix36(id));
    sb.append(".ED_WH");
    String path = sb.toString();

    return new File(getSiteFile("CSampl"), path);
  }

  public static String getNinjaURI (

      int id)

  {

    return "/" + ((id / 36) % 245) + "/" + radix36(id) + ".ED_WH";

  }

  public static String getNinjaVMS (

      int id)

  {

    return "[" + ((id / 36) % 245) + "]" + radix36(id) + ".ED_WH";

  }

  public File getCacheArchive (

      int protocol)

  {

    String fn = "CSdata" + File.separator + (protocol % 99) + File.separator + protocol;

    return getSiteFile(fn + ".ED_WH");

  }

  public File getCacheDrawer (

      int protocol)

  {

    String fn = "CSdata" + File.separator + (protocol % 99) + File.separator + protocol;

    return getSiteFile(fn + ".ED_ENV");

  }

  public File getCacheProtocol (

      int protocol)

  {

    String fn = "CSdata" + File.separator + (protocol % 99) + File.separator + protocol;

    File f = getSiteFile(fn + ".ED_WH");

    if (f.exists())

      return f;

    else

      return getSiteFile(fn + ".ED_ENV");

  }

  public File getCacheDesktop (

      String uid)

  {

    String fn = "EDuser" + File.separator + uid;

    return getSiteFile(fn + ".ED_ENV");

  }

  /**
   * Convert int identifier to radix 36 string

   * @param id int identifier value

   * @return  radix 36 string representation of identifier

   */

  public static String radix36 (

      int id)

  {

    StringBuffer radix_36 = new StringBuffer(8);

    while (true)

    {

      char c = (char)(id % 36);

      if (c < 10)

        radix_36.append((char)('0' + c));

      else

        radix_36.append((char)('A' + c - 10));

      id /= 36;

      if (id == 0)

        break;

    }

    return radix_36.toString();

  }

  /**
   * Convert int identifier to radix 36 string

   * @param id int identifier value

   * @return  radix 36 string representation of identifier

   */

  public static int radix36 (

      String value)

  {

    int id = 0;

    for (int i = 0, n = value.length(), m = 1; i < n; ++i, m *= 36)

    {

      char c = Character.toUpperCase(value.charAt(i));

      if (c >= '0' && c <= '9')

        id += m * (c - '0');

      else if (c >= 'A' && c <= 'Z')

        id += m * (c - 'A' + 10);

      else

        break;

    }

    return id;

  }

  public static String hash36 (

      int id,

      int hashSize)

  {

    return radix36((id / 36) % hashSize);

  }

  /**
   * Construct a VMS style file name from a desk envelope id number

   * @param  id  int  envelope identifier

   * @return  String  file name

   */

  public static String envelope (

      int id)

  {

    return "ED_ENVELOPE:[" + hash36(id, 35)

        + "]" + radix36(id) + ".ED_ENV";

  }

  /**
   * Construct a VMS style file name from a desk warehouse id number

   * @param  id  int  warehouse identifier

   * @return  String  file name

   */

  public static String warehouse (

      int id)

  {

    return "ED_WAREHOUSE:[" + hash36(id, 35)

        + "]" + radix36(id) + ".ED_WH;1";

  }

  /**
   * Get Properties

   * @return properties value

   */

  public Properties getProperties ()

  {

    try

    {

      if (properties == null)

      {

        properties = new Properties();

        properties.load(

            new FileInputStream(

                new File(edeskRoot, "edesk.properties")));

      }

    }

    catch (IOException ex)

    {

      ex.printStackTrace();

    }

    return properties;

  }

  /**
   * Determines the String value of the named property

   * @return property value

   */

  public String getProperty (

      String property)

  {

    return getProperties().getProperty("edesk.site." + edeskSite + "." + property);

  }

  public String getHttpHost ()

  {

    return getProperty("desk.site." + edeskSite + ".http_host");

  }

  /**
   * Connect to desk site by FTP

   */

  public void connect ()

      throws DeskException

  {

    String host = getProperty("ftp.host");

    String user = getProperty("ftp.user");

    String pass = getProperty("ftp.password");

    if (host == null || user == null || pass == null)

      throw new DeskException("site not available by FTP");

    try

    {

//      ftpc = new FtpClient(host);
//
//      ftpc.login(user, pass);
//
//      ftpc.binary();

    }

    catch (Exception ex)

    {

      ex.printStackTrace();

      throw new DeskException("site not responding to FTP");

    }

  }

  /**
   * Close the connection to desk site

   */

  public void close ()

      throws DeskException

  {

    try

    {

//      if (ftpc != null)
//
//        ftpc.closeServer();

      if (context != null)

      {

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(context_file));

        oos.writeObject(context);

        oos.close();

      }

    }

    catch (Exception ex)

    {

      ex.printStackTrace();

      throw new DeskException("close error");

    }

  }

  public DeskInputStream get (

      String file_name,

      boolean ascii)

      throws IOException

  {

//    if (ftpc == null)
//
//      connect();
//
//    if (ascii)
//
//      ftpc.ascii();
//
//    else
//
//      ftpc.binary();
//
//    return new DeskInputStream(ftpc.get(file_name));
  	return null;
  }

  public DeskInputStream get (

      String file_name)

      throws IOException

  {

    return get(file_name, false);

  }

  private byte[] buffer = new byte[8192];

  public void copy (

      String source,

      File target,

      boolean ascii)

      throws IOException

  {

//    if (ftpc == null)
//
//      connect();
//
//    if (ascii)
//
//      ftpc.ascii();
//
//    else
//
//      ftpc.binary();

    target.getParentFile().mkdirs();

    File tmp = this.getSiteFile("EDesk-copy.TMP");

    DeskInputStream dis = get(source);

    try

    {

      BufferedOutputStream bos = new BufferedOutputStream(

          new FileOutputStream(target), buffer.length);

      System.out.println(source + " -> " + target);

      while (true)

      {

        int n = dis.read(buffer);

        if (n < 0)

          break;

        bos.write(buffer, 0, n);

      }

      bos.close();

    }

    catch (IOException ex)

    {

      ex.printStackTrace();

      throw ex;

    }

    finally

    {

      dis.readToEnd();

    }

    tmp.renameTo(target);

  }

  public void copy (

      String source,

      File target)

      throws IOException

  {

    copy(source, target, false);

  }

  /**
   * Finalize FacsDesk by closing any open FTP connections

   */

  protected void finalize ()

  {

    try

    {

      close();

    }

    catch (DeskException ex)

    {

      ex.printStackTrace();

    }

  }

  /**
   * Determines the String value for the institution for this site

   * @return property value

   */

  public String institution ()

  {
    return institution;
  }

  /**
   * Determines the integer value for the site's database code

   * @return property value

   */

  int siteId ()

  {
    return edeskSite;
  }

  public File getFileRoot ()

  {

    return edeskRoot;

  }

  public File getSiteRoot ()

  {

    File root = new File(edeskRoot, getProperty("name"));

    if (!root.exists())

      root.mkdirs();

    return root;

  }

  public String getSiteName ()
  {
    return getProperty("name");
  }

  public File getSiteFile (
      String fileName)
  {
    return new File(getSiteRoot(), fileName);
  }

  public Object getSiteObject (
      String fileName)
  {
    Object object;

    try
    {
      ObjectInputStream ois = new ObjectInputStream(
          new FileInputStream(getSiteFile(fileName)));
      object = ois.readObject();
      ois.close();
    }
    catch (Exception ex)
    {
      object = null;
    }

    return object;
  }

  public void setSiteObject (
      String fileName,
      Object object)
      throws IOException
  {
    File file = getSiteFile("object.tmp");

    ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream(file));
    oos.writeObject(object);
    oos.close();

    file.renameTo(getSiteFile(fileName));
  }

  public void warehouseFault (
      int warehouse,
      String user)
  {
    synchronized (faultLog)
    {
      try
      {
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(faultLog, true));
        dos.writeInt(warehouse);
        dos.writeLong(System.currentTimeMillis());
        if (user == null)
          dos.writeUTF("Unknown");
        else
          dos.writeUTF(user);
        dos.close();
      }
      catch (IOException ioe)
      {
        ioe.printStackTrace();
      }
    }
  }

  public List getWarehouseFaults ()
  {
    return getWarehouseFaults(faultLog);
  }

  List getWarehouseFaults (
      File faultLog)
  {
    List faults = new ArrayList();

    if (!faultLog.exists())
      return faults;

    synchronized (faultLog)
    {
      try
      {
        DataInputStream dis = new DataInputStream(new FileInputStream(faultLog));
        for (; ; )
          try
          {
            WarehouseFault wf = new WarehouseFault();
            wf.warehouse = dis.readInt();
            wf.timestamp = dis.readLong();
            wf.user = dis.readUTF();
            faults.add(wf);
          }
          catch (EOFException ex)
          {
            dis.close();
            break;
          }
      }
      catch (IOException ioe)
      {
        ioe.printStackTrace();
      }
    }

    return faults;
  }

  /**
   * Public constructor
   */
  public FacsDesk (
      String edeskRoot,
      String edeskSite)
      throws DeskException
  {
    if (edeskRoot == null)
      edeskRoot = System.getProperty("user.dir");
    this.edeskRoot = new File(edeskRoot);

    for (this.edeskSite = 1; ; ++this.edeskSite)
    {
      String name = getProperty("name");
      if (name == null)
        throw new DeskException("unknown desk site");
      if (name.equalsIgnoreCase(edeskSite))
        break;
    }

    faultLog = getSiteFile(FAULT_LOG);
  }

  public FacsDesk (
      String site)
      throws DeskException
  {
    this(System.getProperty("edesk.root"), site);
  }
}
