import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class PasswordDialog
  extends JDialog
{
  public static final int SWING_DIALOG = 1;
  public static final int OPTION_DIALOG = 2;
  public final int DIALOG_TYPE;

  JPanel message;
  JLabel hostnameLabel;
  JLabel promptStringLabel;
  JTextField usernameField;
  JPasswordField passwordField;
  String host;
  String prompt;
  String username;
  char[] password;
  boolean waiting;
  int result;
  
  private void optionsDialog ()
  {
    hostnameLabel.setText(host);
    promptStringLabel.setText(prompt);
    usernameField.setText("");
    passwordField.setText("");
    result = JOptionPane.showOptionDialog(getParent(), message,
      "Authentication Required", JOptionPane.OK_CANCEL_OPTION,
      JOptionPane.QUESTION_MESSAGE, null, null, null);
    username = usernameField.getText();
    usernameField.setText("");
    password = passwordField.getPassword();
    passwordField.setText("");
  }

  Runnable startDialog = new Runnable()
  {

    public void run ()
    {
      switch (DIALOG_TYPE)
      {
      case OPTION_DIALOG:

        optionsDialog();

        synchronized (auth)
        {
          waiting = false;
          auth.notify();
        }
        break;

      case SWING_DIALOG:
        hostnameLabel.setText(host);
        promptStringLabel.setText(prompt);
        usernameField.setText("");
        passwordField.setText("");

        pack();
        
        getParent();
        setLocationRelativeTo(getParent());
        setVisible(true);
        break;
      }
    }
  };

  Runnable finishDialog = new Runnable()
  {

    public void run ()
    {
      username = usernameField.getText();
      usernameField.setText("");
      password = passwordField.getPassword();
      passwordField.setText("");

      synchronized (auth)
      {
        waiting = false;
        auth.notify();
      }
    }
  };

  Authenticator auth = new Authenticator()
  {
    Map<String, PasswordAuthentication> savedCredentials = new HashMap<String, PasswordAuthentication>();

    protected PasswordAuthentication getPasswordAuthentication ()
    {
      host = getRequestingHost();
      if (host == null)
        host = getRequestingSite().getHostAddress();
      if (host==null)
        host = "";
      int port = getRequestingPort();
      String protocol = getRequestingProtocol();
      if (port > 0)
        if (protocol.equalsIgnoreCase("http") && port != 80)
          host += ":" + port;
        else if (protocol.equalsIgnoreCase("https") && port != 443)
          host += ":" + port;

      PasswordAuthentication credentials = savedCredentials.get(host);
      if (credentials!=null)
        return credentials;
      
      prompt = getRequestingPrompt();
      if (prompt == null)
        prompt = "";

      if (SwingUtilities.isEventDispatchThread())
      {
        optionsDialog();
      }
      else
      {
        waiting = true;
        SwingUtilities.invokeLater(startDialog);
        synchronized (auth)
        {
          while (waiting)
            try
            {
              auth.wait();
            }
            catch (InterruptedException e)
            {
              return null;
            }
        }
      }

      if (result==JOptionPane.OK_OPTION)
      {
        credentials = new PasswordAuthentication(username, password);
        savedCredentials.put(host, credentials);
      }
      username = null;
      password = null;

      return credentials;
    }
  };
  
  public PasswordDialog ()
  {
    this(null);
  }

  public PasswordDialog (JFrame parent)
  {
    super(parent);
    
    DIALOG_TYPE = SWING_DIALOG;

    JLabel hostLabel = new JLabel("Host: ", JLabel.TRAILING);
    hostnameLabel = new JLabel();
    JLabel promptLabel = new JLabel("Prompt: ", JLabel.TRAILING);
    promptStringLabel = new JLabel();
    JLabel usernameLabel = new JLabel("Username: ", JLabel.TRAILING);
    usernameField = new JTextField();
    usernameField.setColumns(32);
    usernameField.setRequestFocusEnabled(true);
    JLabel passwordLabel = new JLabel("Password: ", JLabel.TRAILING);
    passwordField = new JPasswordField();
    passwordField.setColumns(32);

    message = new JPanel(new GridBagLayout());
    message.setBorder(new EmptyBorder(6, 10, 6, 10));

    Insets insets = new Insets(6, 10, 6, 10);
    GridBagConstraints labelCons = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
      GridBagConstraints.LINE_END, GridBagConstraints.NONE, insets, 10, 6);
    GridBagConstraints fieldCons = new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
      GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, insets, 10,
      6);

    message.add(hostLabel, labelCons);
    message.add(hostnameLabel, fieldCons);

    ++labelCons.gridy;
    ++fieldCons.gridy;
    message.add(promptLabel, labelCons);
    message.add(promptStringLabel, fieldCons);

    ++labelCons.gridy;
    ++fieldCons.gridy;
    message.add(usernameLabel, labelCons);
    message.add(usernameField, fieldCons);

    ++labelCons.gridy;
    ++fieldCons.gridy;
    message.add(passwordLabel, labelCons);
    message.add(passwordField, fieldCons);

    switch (DIALOG_TYPE)
    {
    case OPTION_DIALOG:

      break;

    case SWING_DIALOG:
      setTitle("Authentication Required");
      setModal(true);

      JButton okButton = new JButton("OK");
      okButton.setSelected(true);
      getRootPane().setDefaultButton(okButton);
      okButton.addActionListener(new ActionListener()
      {
        public void actionPerformed (ActionEvent e)
        {
          result = JOptionPane.OK_OPTION;
          PasswordDialog.this.setVisible(false);
          SwingUtilities.invokeLater(finishDialog);
        }
      });

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(new ActionListener()
      {
        public void actionPerformed (ActionEvent e)
        {
          result = JOptionPane.CANCEL_OPTION;
          PasswordDialog.this.setVisible(false);
          SwingUtilities.invokeLater(finishDialog);
        }
      });

      JPanel buttons = new JPanel();
      buttons.setLayout(new GridLayout(1, 2, 10, 6));
      buttons.add(okButton);
      buttons.add(cancelButton);

      JPanel buttonPanel = new JPanel();
      buttonPanel.setBorder(new EmptyBorder(6, 10, 6, 10));
      buttonPanel.add(buttons);

      Container content = getContentPane();
      content.add(message, BorderLayout.CENTER);
      content.add(buttonPanel, BorderLayout.SOUTH);

      pack();
      break;
    }

    Authenticator.setDefault(auth);
  }
}
