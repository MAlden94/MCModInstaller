 package com.Mitch.SplashScreen;
 
 import com.Mitch.SplashScreen.BackgroundPanel.BackgroundPanel;

import javax.swing.*;
import java.awt.*;
 
 public class SplashScreen extends JWindow
 {
   private static final long serialVersionUID = 1L;
   private static String Markup = null;
 
   Image imageIcon = null;
   BorderLayout borderLayout1 = new BorderLayout();
 
   BackgroundPanel southPanel = new BackgroundPanel(this.imageIcon);
   JLabel InfoText = new JLabel(Markup, JLabel.CENTER);
 
   JProgressBar progressBar = new JProgressBar();
 
   public SplashScreen(Image imageIcon, String markup)
   {
     this.imageIcon = imageIcon;
     Markup    = markup;
     try
     {
       jbInit();
     } catch (Exception ex) {
       ex.printStackTrace();
     }
   }
 
   void jbInit()
     throws Exception
   {
     this.southPanel.setImage(this.imageIcon);
 
     this.InfoText.setOpaque(false);
     this.progressBar.setOpaque(true);

     this.InfoText.setText(Markup);
 
     getContentPane().setLayout(this.borderLayout1);
 
     this.southPanel.add(this.InfoText, null);
     getContentPane().add(this.southPanel, "North");
     getContentPane().add(this.progressBar, null);
 
     getRootPane().setBorder(BorderFactory.createLineBorder(Color.BLACK));
 
     pack();
   }
 
   public void setProgressMax(int maxProgress)
   {
     this.progressBar.setMaximum(maxProgress);
   }
 
   public void setProgress(int progress)
   {
     final int theProgress = progress;
     SwingUtilities.invokeLater(new Runnable() {
       public void run() {
         SplashScreen.this.progressBar.setValue(theProgress);
       }
     });
   }
 
   public void setProgress(String message, int progress)
   {
     final int theProgress = progress;
     final String theMessage = message;
     setProgress(progress);
     SwingUtilities.invokeLater(new Runnable() {
       public void run() {
         SplashScreen.this.progressBar.setValue(theProgress);
         SplashScreen.this.setMessage(theMessage);
       }
     });
   }
 
   public void setScreenVisible(boolean b)
   {
     final boolean boo = b;
     SwingUtilities.invokeLater(new Runnable() {
       public void run() {
         SplashScreen.this.setVisible(boo);
       }
     });
   }
 
   private void setMessage(String message)
   {
     if (message == null) {
       message = "";
       this.progressBar.setStringPainted(false);
     } else {
       this.progressBar.setStringPainted(true);
     }
     this.progressBar.setString(message);
   }
 }
