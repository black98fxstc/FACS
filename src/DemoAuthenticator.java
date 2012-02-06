import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class DemoAuthenticator
extends JFrame
{
  JTextPane text;
  public DemoAuthenticator ()
  {
    Container content = getContentPane();
    JScrollPane scroll = new JScrollPane();
    content.add(scroll, BorderLayout.CENTER);
    text = new JTextPane();
    scroll.getViewport().add(text);
    text.setContentType("text/html");
    text.setEditable(false);
    text.setText("<html><body>Blah Blah Blah</body></html>");
    
    setSize(500, 750);
    validate();
    
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = getSize();
    if (frameSize.height>screenSize.height)
    {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width>screenSize.width)
    {
      frameSize.width = screenSize.width;
    }
    setLocation(
      (screenSize.width-frameSize.width)/2,
      (screenSize.height-frameSize.height)/2);
    
    addWindowListener(new WindowAdapter()
    {
      public void windowClosing (WindowEvent e)
      {
        System.exit(0);
      }
    });
    
    // create a password dialog that will center itself on this window
    
    PasswordDialog pass = new PasswordDialog(this);
  }
  
  public static void main (String[] args)
  {
    final String url;
    if (args.length > 0)
      url = args[0];
    else
      url = "http://facsdata.stanford.edu:8080/ERS/data/o%3DStanford%20University/ou%3DFACS%20Facility/uid%3Dwmoore/journal%3DLSR%20II/session%3D2753/index%3DBead%20Test%202.html";

    final DemoAuthenticator demo = new DemoAuthenticator();

    final Thread test = new Thread()
    {
      public void run ()
      {
        try
        {
          HttpURLConnection http = (HttpURLConnection)new URL(url).openConnection();
          InputStream is = http.getInputStream();
          InputStreamReader isr = new InputStreamReader(is);
          CharArrayWriter caw = new CharArrayWriter();
          char[] buffer = new char[8192];
          for (;;)
          {
            int n = isr.read(buffer);
            if (n < 0)
              break;
            caw.write(buffer, 0, n);
          }
          isr.close();
          caw.close();
          
          demo.text.setText(caw.toString());
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    };
    test.setPriority(Thread.MIN_PRIORITY);
    
    SwingUtilities.invokeLater(new Runnable ()
    {
      public void run ()
      {
        try
        {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          demo.setVisible(true);
//          demo.text.setPage(url);

        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    });
    
    try
    {
      Thread.sleep(2000);
      test.start();
      test.join();
    }
    catch (InterruptedException e1)
    {
      e1.printStackTrace();
    }
  }
}
