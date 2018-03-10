import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import java.util.*;
public class Main
{

public static void main(String[] args)
{
JFrame editorFrame = new JFrame("Image Demo");
editorFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        
        BufferedImage image = null, image2=null;
        try
        {
          image = ImageIO.read(new File("1.jpg"));
		  
          image2 = ImageIO.read(new File("2.jpg"));
        }
        catch (Exception e)
        {
          e.printStackTrace();
          System.exit(1);
        }
        ImageDrawer panel = new ImageDrawer();
		//panel.img=HistogramEQ.computeHistogramEQ(image);
		//panel.img2=HistogramEQ.computeHistogramEQ(image2);
		panel.img=image;
		panel.img2=image2;
		JButton bu = new JButton("Calculer");
		bu.addActionListener(panel);
		editorFrame.getContentPane().add(panel, BorderLayout.CENTER);
		editorFrame.getContentPane().add(bu, BorderLayout.SOUTH);
        editorFrame.pack();
        editorFrame.setLocationRelativeTo(null);
        editorFrame.setVisible(true);
		
		
		
						}
						
	  
private static int[] toPixelsTab(BufferedImage picture) {
	int width = picture.getWidth();
	int height = picture.getHeight();
 
	int[] pixels = new int[width * height];
	// copy pixels of picture into the tab
	picture.getRGB(0,0,width,height,pixels,0,width);
 
	// On Android, Color are coded in 4 bytes (argb),
	// whereas SIFT needs color coded in 3 bytes (rgb)
 
	for (int i = 0; i < (width * height); i++)
		pixels[i] &= 0x00ffffff;
 
	return pixels;
}
 

}