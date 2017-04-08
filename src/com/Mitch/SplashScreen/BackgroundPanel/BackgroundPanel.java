 package com.Mitch.SplashScreen.BackgroundPanel;
 
 import java.awt.BorderLayout;
 import java.awt.Component;
 import java.awt.Dimension;
 import java.awt.Graphics;
 import java.awt.Graphics2D;
 import java.awt.Image;
 import java.awt.Insets;
 import java.awt.Paint;
 import java.awt.Rectangle;
 import javax.swing.JComponent;
 import javax.swing.JLabel;
 import javax.swing.JScrollPane;
 import javax.swing.JViewport;
 
 public class BackgroundPanel extends JLabel
 {
   public static final int SCALED = 0;
   public static final int TILED = 1;
   public static final int ACTUAL = 2;
   private Paint painter;
   private Image image;
   private int style = SCALED;
   private float alignmentX = 0.5F;
   private float alignmentY = 0.5F;
   private boolean isTransparentAdd = true;
 
   public BackgroundPanel(Image image)
   {
     this(image, SCALED);
   }
 
   public BackgroundPanel(Image image, int style)
   {
     setImage(image);
     setStyle(style);
     setLayout(new BorderLayout());
   }
 
   public BackgroundPanel(Image image, int style, float alignmentX, float alignmentY)
   {
     setImage(image);
     setStyle(style);
     setImageAlignmentX(alignmentX);
     setImageAlignmentY(alignmentY);
     setLayout(new BorderLayout());
   }
 
   public BackgroundPanel(Paint painter)
   {
     setPaint(painter);
     setLayout(new BorderLayout());
   }
 
   public void setImage(Image image)
   {
     this.image = image;
     repaint();
   }
 
   public void setStyle(int style)
   {
     this.style = style;
     repaint();
   }
 
   public void setPaint(Paint painter)
   {
     this.painter = painter;
     repaint();
   }
 
   public void setImageAlignmentX(float alignmentX)
   {
     this.alignmentX = (alignmentX < 0.0F ? 0.0F : alignmentX > 1.0F ? 1.0F : alignmentX);
     repaint();
   }
 
   public void setImageAlignmentY(float alignmentY)
   {
     this.alignmentY = (alignmentY < 0.0F ? 0.0F : alignmentY > 1.0F ? 1.0F : alignmentY);
     repaint();
   }
 
   public void add(JComponent component)
   {
     add(component, null);
   }
 
   public Dimension getPreferredSize()
   {
     if (this.image == null) {
       return super.getPreferredSize();
     }
     return new Dimension(this.image.getWidth(null), this.image.getHeight(null));
   }
 
   public void add(JComponent component, Object constraints)
   {
     if (this.isTransparentAdd)
     {
       makeComponentTransparent(component);
     }
 
     super.add(component, constraints);
   }
 
   public void setTransparentAdd(boolean isTransparentAdd)
   {
     this.isTransparentAdd = isTransparentAdd;
   }
 
   private void makeComponentTransparent(JComponent component)
   {
     component.setOpaque(false);
 
     if ((component instanceof JScrollPane))
     {
       JScrollPane scrollPane = (JScrollPane)component;
       JViewport viewport = scrollPane.getViewport();
       viewport.setOpaque(false);
       Component c = viewport.getView();
 
       if ((c instanceof JComponent))
       {
         ((JComponent)c).setOpaque(false);
       }
     }
   }
 
   protected void paintComponent(Graphics g)
   {
     super.paintComponent(g);
 
     if (this.painter != null)
     {
       Dimension d = getSize();
       Graphics2D g2 = (Graphics2D)g;
       g2.setPaint(this.painter);
       g2.fill(new Rectangle(0, 0, d.width, d.height));
     }
 
     if (this.image == null) return;
 
     switch (this.style)
     {
     case SCALED:
       drawScaled(g);
       break;
     case TILED:
       drawTiled(g);
       break;
     case ACTUAL:
       drawActual(g);
       break;
     default:
       drawScaled(g);
     }
   }
 
   private void drawScaled(Graphics g)
   {
     Dimension d = getSize();
     g.drawImage(this.image, 0, 0, d.width, d.height, null);
   }
 
   private void drawTiled(Graphics g)
   {
     Dimension d = getSize();
     int width = this.image.getWidth(null);
     int height = this.image.getHeight(null);
 
     for (int x = 0; x < d.width; x += width)
     {
       for (int y = 0; y < d.height; y += height)
       {
         g.drawImage(this.image, x, y, null, null);
       }
     }
   }
 
   private void drawActual(Graphics g)
   {
     Dimension d = getSize();
     Insets insets = getInsets();
     int width = d.width - insets.left - insets.right;
     int height = d.height - insets.top - insets.left;
     float x = (width - this.image.getWidth(null)) * this.alignmentX;
     float y = (height - this.image.getHeight(null)) * this.alignmentY;
     g.drawImage(this.image, (int)x + insets.left, (int)y + insets.top, this);
   }
 }
