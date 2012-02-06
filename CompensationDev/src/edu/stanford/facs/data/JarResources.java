/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.stanford.facs.data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.*;

/**
 * $Id: JarResources.java,v 1.2 2011/06/17 00:38:01 beauheim Exp $
 * @author cate
 */
public class JarResources {
    // external debug flag
   public boolean debugOn=true;

   // jar resource mapping tables
   private HashMap <String, Integer> htSizes=new HashMap<String, Integer>();
   private HashMap htJarContents=new HashMap();

   private String jarFileName; 
    JarResources (String file){
        this.jarFileName = file;
        init();
    }
    /**
    * initializes internal hash tables with Jar file resources.
    */
   private void init() {
      try {
          // extracts just sizes only.
          File zipfile = new File (jarFileName);
          if (zipfile != null && zipfile.exists() ){
              System.out.println ("zipfile exists");
              if (zipfile.canRead())
                  System.out.println (" zipfile can be read ");
              else
                  System.out.println (" cannot read the zip file");
          }
          ZipFile zf = new ZipFile (zipfile);
//          ZipFile zf=new ZipFile(jarFileName);
          Enumeration e=zf.entries();
          while (e.hasMoreElements()) {
              ZipEntry ze=(ZipEntry)e.nextElement();
              if (debugOn) {
                 System.out.println(dumpZipEntry(ze));
              }
              htSizes.put(ze.getName(),new Integer((int)ze.getSize()));
          }
          zf.close();

          // extract resources and put them into the hashMap.
          FileInputStream fis=new FileInputStream(jarFileName);
          BufferedInputStream bis=new BufferedInputStream(fis);
          ZipInputStream zis=new ZipInputStream(bis);
          ZipEntry ze=null;
          while ((ze=zis.getNextEntry())!=null) {
             if (ze.isDirectory()) {
                continue;
             }
             if (debugOn) {
                System.out.println(
                   "ze.getName()="+ze.getName()+","+"getSize()="+ze.getSize()
                   );
             }
             int size=(int)ze.getSize();
             // -1 means unknown size. 
             if (size==-1) {
                size=((Integer)htSizes.get(ze.getName())).intValue();
             }
             byte[] b=new byte[(int)size];
             int rb=0;
             int chunk=0;
             while (((int)size - rb) > 0) {
                 chunk=zis.read(b,rb,(int)size - rb);
                 if (chunk==-1) {
                    break;
                 }
                 rb+=chunk;
             }
             // add to internal resource hashMap
             htJarContents.put(ze.getName(),b);
             if (debugOn) {
                System.out.println(ze.getName()+"  rb="+rb+",size="+size+
                   ",csize="+ze.getCompressedSize());
             }
          }
       } catch (NullPointerException e) {
          System.out.println("done.");
       } catch (FileNotFoundException e) {
            System.out.println (e.getMessage());
       } catch (IOException e) {
           System.out.println (e.getMessage());
         
       }
   }

   /**
    * Dumps a zip entry into a string.
    * @param ze a ZipEntry
    */
   private String dumpZipEntry(ZipEntry ze) {
       StringBuilder sb=new StringBuilder();
       if (ze.isDirectory()) {
          sb.append("d "); 
       } else {
          sb.append("f "); 
       }
       if (ze.getMethod()==ZipEntry.STORED) {
          sb.append("stored   "); 
       } else {
          sb.append("defalted ");
       }
       sb.append(ze.getName()).append("\t").append(ze.getSize());
       
       if (ze.getMethod()==ZipEntry.DEFLATED) {
            sb.append("/").append(ze.getCompressedSize());
       }
       return (sb.toString());
   }

    
    public byte[] getResource (String s){
        byte[] res = null;
        res = (byte[])htJarContents.get(s);
        String eol = System.getProperty ("line.separator");
        byte[] eolb = new byte[eol.length()];
        eolb = eol.getBytes();
        System.out.println (" eolb " + eolb.length + "  "+ eolb[0]);
        System.out.println ("line separator " + eol.length());
        String s1 = new String (eolb);
        System.out.println ("byte  to string("+ s1+")" );
        return (res);
    }
    private String addNewLines (byte[] bytes){
        StringBuilder sbuf = new StringBuilder();
        String eol = System.getProperty("line.separator");
        byte[] eolb= new byte[eol.length()];
        eolb= eol.getBytes();
        for (int i=0; i < bytes.length; i++){
//            System.out.println (bytes[i]);
            if (bytes[i] == eolb[0]){
                sbuf.append (eol);
            }
            else{
                byte[] bb=new byte[1];
                bb[0]=bytes[i];
                sbuf.append (new String(bb));
            }

        }

        return sbuf.toString();
    }

    public static void main(String[] args) throws IOException {
//       if (args.length!=2) {
//          System.err.println(
//             "usage: java JarResources <jar file name> <resource name>"
//             );
//          System.exit(1);
//       }
        String jarfile="lib/Config.jar";
       JarResources jr=new JarResources(jarfile);
       String resource = "LSRII.csv";
       byte[] buff=jr.getResource(resource);
       if (buff==null ) {
          System.out.println("Could not find "+args[1]+".");
       } else {
//           StringBuffer sbuf = new StringBuffer();
//           for (int i=0; i < buff.length; i++){
//               sbuf.append (new String(buff[i]));
//           }
          System.out.println("Found LSRII.2.csv "+ " (length="+buff.length+").");
          String contents = jr.addNewLines (buff);
          System.out.println ( "--- "+ contents);
//          System.out.println (new String(buff));
       }
   }
}   // End of JarResources class.


