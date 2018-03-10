import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import java.util.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.geom.AffineTransform;
import org.apache.commons.math3.linear.*;
import mpi.cbg.fly.*;

class ImageDrawer extends JComponent implements MouseListener,ActionListener
{
	public BufferedImage img = null, img2=null, img2bis=null, img3bis=null;
	public AffineTransform transform = null;
	java.util.List<Point2D.Double> cor1,cor2,hom1,hom2;
	Color[] c = {Color.RED,Color.GREEN,Color.BLUE,Color.ORANGE,Color.GRAY,Color.RED,Color.GREEN,Color.BLUE,Color.ORANGE,Color.GRAY};
	int mode=0;
	Vector<Feature> features1=null,features2=null;
	float scale = 0;
	RealMatrix result=null;
	public java.util.List<Match> inliers = null;
	
	public ImageDrawer()
	{
	super();
	addMouseListener(this);
	cor1 = new LinkedList<Point2D.Double>();
	cor2 = new LinkedList<Point2D.Double>();
	hom1 = new LinkedList<Point2D.Double>();
	hom2 = new LinkedList<Point2D.Double>();
	}
	
	public  void    actionPerformed(ActionEvent e)
    {
	Vector<Match> matches = new Vector<Match>();
	if(cor1.size()>=4 && cor2.size()>=4) // MODE MANUEL
	{
		for(int i=0;i<Math.min(cor1.size(),cor2.size());i++)
			matches.add(new Match(cor1.get(i),cor2.get(i)));
		result = computeHomography(matches);
	}
	else //MODE AUTO (SIFT)
	{
	int[] pixels = toPixelsTab(img);
		int[] pixels2 = toPixelsTab(img2);
		Vector<Feature> f1 = SIFT.getFeatures(
						img.getWidth(), img.getHeight(), pixels);
		Vector<Feature> f2 = SIFT.getFeatures(
						img2.getWidth(), img2.getHeight(), pixels2);
		features1=f1;
		features2=f2;
		Vector<PointMatch> sift_matches = SIFT.createMatches(f1,f2,Float.MAX_VALUE,null,10);
		for(PointMatch pm:sift_matches)
			matches.add(new Match(new Point2D.Double(pm.getP1().getL()[0],pm.getP1().getL()[1]),new Point2D.Double(pm.getP2().getL()[0],pm.getP2().getL()[1])));
		System.out.println(matches.size());
		result = computeBestHomography(matches);
	}
	this.inliers = matches;
		System.out.println("Calcul des images interpolées en cours (0/2)");
		img2bis = bilinearInterpolation(img,MatrixUtils.inverse(result));
		System.out.println("Calcul des images interpolées en cours (1/2)");
		img3bis = bilinearInterpolation(img2,result);
		
		System.out.println("Calcul des images interpolées en cours (2/2)");
		try{
					File outputfile = new File("image.jpg");
ImageIO.write(img2bis, "jpg", outputfile);
					outputfile = new File("image2.jpg");
ImageIO.write(img3bis, "jpg", outputfile);
}
catch(Exception ex){}
		mode=1;
    }
	
	public int[] differentRandomInts(int nb, int max)
	{
		int[] res = new int[nb];
		int found=0;
		while(found<nb)
		{
			int new_rand = (int) Math.floor(Math.random() *max);
			boolean notAlready=true;
			for(int j=0;j<found;j++)
				if(new_rand==res[j])
					notAlready=false;
			if(notAlready)
			{
				res[found++]=new_rand;
			}
		}
		return res;
	}
	
	protected RealMatrix computeBestHomography(Vector<Match> matches)
	{	
		int nb=matches.size();
		int max_inliers=0;
		double distance_threshold = 100; //A MODIFIER
		double best_mean_error=0;
		RealMatrix best = null;
		Vector<Match> best_inliers = null;
		/* RANSAC Algorithm*/
		//Modifier nombre d'itérations
		for(int iter=0;iter<300000;iter++)
		{
		if(iter%1000 ==0)
		System.out.println("Iteration "+iter);
		int[] randoms = differentRandomInts(6,nb);
		//System.out.println(Arrays.toString(randoms));
		/* Select 4 pairs in matches */
			Vector<Match> estimates = new Vector<Match>();
			for(Integer i: randoms)
				estimates.add(matches.get(i));
			//Compute homography
			RealMatrix hom = computeHomography(estimates);
			//Count inliers
			Vector<Match> inliers=new Vector<Match>();
			for(Match pm : matches)
			{
				Point2D.Double p1 = pm.p1;
				Point2D.Double p2 = pm.p2;
				Point2D.Double projected = computeCorrespondingPoint(hom,p1.getX(),p1.getY());
				double dist = distance(projected.getX(),projected.getY(),p2.getX(),p2.getY());
				//System.out.println(dist);
				if(dist<=distance_threshold)
				{
					inliers.add(pm);
				}
			}
			//System.out.println("Number of inliers before refinement :"+inliers.size());
			if(inliers.size()<4)
			continue;
			//REfine homography
			hom = computeHomography(inliers);
			//Recount inliers
			inliers=new Vector<Match>();
			double mean_error = 0;
			for(Match pm : matches)
			{
				Point2D.Double p1 = pm.p1;
				Point2D.Double p2 = pm.p2;
				Point2D.Double projected = computeCorrespondingPoint(hom,p1.getX(),p1.getY());
				double dist = distance(projected.getX(),projected.getY(),p2.getX(),p2.getY());
				if(dist<=distance_threshold)
				{
					inliers.add(pm);
					mean_error+=dist;
				}
			}
			mean_error = mean_error/inliers.size();
			//System.out.println("Number of inliers after refinement :"+inliers.size());
			//System.out.println(hom.toString());
			if(inliers.size()>max_inliers || (inliers.size()==max_inliers && mean_error<best_mean_error))
			{
				max_inliers = inliers.size();
				best_inliers = inliers;
				best = hom;
				best_mean_error = mean_error;
			}
		}
		System.out.println("Maximum inliers : "+max_inliers);
		this.inliers = best_inliers;
		return best;
	}
	
	protected void paintComponent(Graphics g) {
        paintComponent((Graphics2D) g);
    }
	
	   public void mousePressed(MouseEvent e) {}   

    public void mouseReleased(MouseEvent e) { }

    public void mouseEntered(MouseEvent e) {    }

    public void mouseExited(MouseEvent e) {   }

    public void mouseClicked(MouseEvent e) {
	java.awt.Point p = e.getPoint();
	if(mode==0)
	{
	int w = img.getWidth(null);
                int h = img.getHeight(null);
                int w2 = img2.getWidth(null);
                int h2 = img2.getHeight(null);
				/* Il faudrait vérifier qu'on clique bien sur une puis l'autre*/
				System.out.println(p.getX()+ " "+w*scale);
	if(p.getX()<w*scale) // Click sur image1
		cor1.add(new Point2D.Double(p.getX()/scale,p.getY()/scale));
	else
		cor2.add(new Point2D.Double((p.getX()-w*scale)/scale,p.getY()/scale));
		
	System.out.println(cor1.size()+" "+cor2.size());
	/*if(cor1.size()>=4 && cor2.size()>=4)
		computeHomography();*/
	}
	if(mode==1)
	{
		Point2D.Double pp = new Point2D.Double(p.getX()/scale,p.getY()/scale);
		hom1.add(pp);
		hom2.add(computeCorrespondingPoint(result,pp.getX(),pp.getY()));
	}
	repaint();
    }
	
	public Point2D.Double computeCorrespondingPoint(RealMatrix homography,int x1,int y1)
	{
		return computeCorrespondingPoint(homography,(double)x1,(double)y1);
						
	}
	
	public Point2D.Double computeCorrespondingPoint(RealMatrix homography, double x1,double y1)
	{
		double[] coord = {x1,y1,1};
						RealMatrix nc = homography.multiply(new Array2DRowRealMatrix(coord));
						/*System.out.println(nc.toString());
						System.out.println(nc.getRowDimension());
						System.out.println(nc.getColumnDimension());*/
						nc = nc.scalarMultiply(1/nc.getEntry(2,0));
						return new Point2D.Double(nc.getEntry(0,0),nc.getEntry(1,0));
						
	}
	
	public Point2D.Double getMean(java.util.List<Point2D.Double> l)
	{
		double mean_x=0,mean_y=0;
		int n = l.size();
		for(Point2D.Double p : l)
		{
			mean_x+=p.getX()/n;
			mean_y+=p.getY()/n;
		}
		return new Point2D.Double(mean_x,mean_y);
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
	
	public double getVariance(java.util.List<Point2D.Double> l, Point2D.Double mean)
	{
		double v=0;
		int n = l.size();
		for(Point2D.Double p : l)
		{
			v+= 1f/n*Math.sqrt(Math.pow(p.getX()-mean.getX(),2)+Math.pow(p.getY()-mean.getY(),2));
		}
		return v;
	}
	
	public Array2DRowRealMatrix getNormMatrix(Point2D.Double mean,double var)
	{
		Array2DRowRealMatrix T = new Array2DRowRealMatrix(3,3);
		for(int i=0;i<3;i++)
			for(int j=0;j<3;j++)
				T.setEntry(i,j,0);
		double s = Math.sqrt(2)/var;
		T.setEntry(0,0,s);
		T.setEntry(1,1,s);
		T.setEntry(2,2,1);
		T.setEntry(0,2,-s*mean.getX());
		T.setEntry(1,2,-s*mean.getY());
		//System.out.println(T.toString());
		return T;
	}
	
	public Color interpolate(Color c1, Color c2, double delta)
	{
		double[] interpolated = new double[3];
							interpolated[0] = (1-delta)*c1.getRed()+delta*c2.getRed();
							interpolated[1] = (1-delta)*c1.getGreen()+delta*c2.getGreen();
							interpolated[2] = (1-delta)*c1.getBlue()+delta*c2.getBlue();
		return new Color((int) interpolated[0],(int) interpolated[1],(int) interpolated[2]);
	}
	
	public BufferedImage bilinearInterpolation(BufferedImage src, RealMatrix homInv)
	{
	int nw = src.getWidth(), nh = src.getHeight();
				 BufferedImage res = new BufferedImage(nw,nh,BufferedImage.TYPE_INT_RGB);
	for(int i=0;i<nw;i++)
		for(int j=0;j<nh;j++)
					{
						Color c = Color.BLACK;
						double[] coord={i,j,1};
						RealMatrix nc = homInv.multiply(new Array2DRowRealMatrix(coord));
						nc = nc.scalarMultiply(1/nc.getEntry(2,0));
						double ni = nc.getEntry(0,0), nj = nc.getEntry(1,0);
						int bx = (int)Math.floor(ni), by = (int)Math.floor(nj);
						double dx = ni-bx, dy=nj-by;
						// Compute interpolation value
						if(ni>=0 && ni<nw-1 && nj>=0 && nj<nh-1)
						{
							Color c1 = interpolate(new Color(src.getRGB(bx,by)),new Color(src.getRGB(bx+1,by)),dx);
							Color c2 = interpolate(new Color(src.getRGB(bx,by+1)),new Color(src.getRGB(bx+1,by+1)),dx);
							c = interpolate(c1,c2,dy);
						}
							res.setRGB(i,j,c.getRGB());
					}
	return res;
	}
	
	public Map.Entry<Point2D.Double,Point2D.Double> getMean(Vector<Match> l)
	{
	double mean1_x=0,mean1_y=0,mean2_x=0,mean2_y=0;
		int n = l.size();
		for(Match p : l)
		{
			mean1_x+=p.p1.getX()/n;
			mean1_y+=p.p1.getY()/n;
			mean2_x+=p.p2.getX()/n;
			mean2_y+=p.p2.getY()/n;
		}
		return new AbstractMap.SimpleEntry<Point2D.Double,Point2D.Double>(new Point2D.Double(mean1_x,mean1_y),new Point2D.Double(mean2_x,mean2_y));
	}
	
	public Map.Entry<Double,Double> getVariance(Vector<Match> l,Map.Entry<Point2D.Double,Point2D.Double> means)
	{
		double v1=0,v2=0;
		int n = l.size();
		for(Match p : l)
		{
			v1+= 1f/n*Math.sqrt(Math.pow(p.p1.getX()-means.getKey().getX(),2)+Math.pow(p.p1.getY()-means.getKey().getY(),2));
			v2+= 1f/n*Math.sqrt(Math.pow(p.p2.getX()-means.getValue().getX(),2)+Math.pow(p.p2.getY()-means.getValue().getY(),2));
		}
		return new AbstractMap.SimpleEntry<Double,Double>(v1,v2);
	}
	
	public double distance(float[] p1,float[] p2)
	{
		return Math.sqrt((p1[0]-p2[0])*(p1[0]-p2[0])+(p1[1]-p2[1])*(p1[1]-p2[1]));
	}
	
	public double distance(double x1, double y1, double x2, double y2)
	{
		return Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
	}
	
	protected RealMatrix computeHomography(Vector<Match> matches)
	{
		int nb = matches.size();
		//System.out.println(nb);
		if(nb==0)
		return null;
		Array2DRowRealMatrix matrix = new Array2DRowRealMatrix(2*nb,9);
		
		/* Normalize */
		Map.Entry<Point2D.Double,Point2D.Double> means = getMean(matches);
		Map.Entry<Double,Double> vars = getVariance(matches,means);
		Array2DRowRealMatrix T1 = getNormMatrix(means.getKey(),vars.getKey());
		Array2DRowRealMatrix T2 = getNormMatrix(means.getValue(),vars.getValue());
		//Point2D.Double m1 = getMean(cor1);
		//Point2D.Double m2 = getMean(cor2);
		//double v1 = getVariance(cor1,m1);
		//double v2 = getVariance(cor2,m2);
		//Array2DRowRealMatrix T1 = getNormMatrix(m1,v1);
		//Array2DRowRealMatrix T2 = getNormMatrix(m2,v2);
		
		for(int i=0;i<nb;i++)
		{
		double x1 = matches.get(i).p1.getX();
		double y1 = matches.get(i).p1.getY();
		double x2 = matches.get(i).p2.getX();
		double y2 = matches.get(i).p2.getY();
		//double x1 = cor1.get(i).getX(),y1 = cor1.get(i).getY(),x2 = cor2.get(i).getX(),y2 = cor2.get(i).getY();
		x1 = T1.getEntry(0,0)*x1+T1.getEntry(0,2);
		x2 = T2.getEntry(0,0)*x2+T2.getEntry(0,2);
		y1 = T1.getEntry(0,0)*y1+T1.getEntry(1,2);
		y2 = T2.getEntry(0,0)*y2+T2.getEntry(1,2);
		double[] eq0 = {x1,y1,1,0,0,0,-x1*x2,-y1*x2,-x2};
		double[] eq1 = {0,0,0,x1,y1,1,-x1*y2,-y1*y2,-y2};
		matrix.setRow(i*2,eq0);
		matrix.setRow(i*2+1,eq1);
		}
		//System.out.println(matrix.toString());
		
		SingularValueDecomposition svd = new SingularValueDecomposition(matrix);
		//System.out.println(Arrays.toString(svd.getSingularValues()));
		double[] res = svd.getV().getColumn(svd.getV().getColumnDimension()-1);
		result = new Array2DRowRealMatrix(3,3);
		result.setEntry(0,0,res[0]);
		result.setEntry(0,1,res[1]);
		result.setEntry(0,2,res[2]);
		result.setEntry(1,0,res[3]);
		result.setEntry(1,1,res[4]);
		result.setEntry(1,2,res[5]);
		result.setEntry(2,0,res[6]);
		result.setEntry(2,1,res[7]);
		result.setEntry(2,2,res[8]);
		//result = result.scalarMultiply(1/result.getEntry(2,2));
		result = MatrixUtils.inverse(T2).multiply(result).multiply(T1);
		/* Calculer img2 bis*/
		/*A modifier, interpolation dégue*/
		/*img2bis = bilinearInterpolation(img,MatrixUtils.inverse(result));
		img3bis = bilinearInterpolation(img2,result);
					try{
					File outputfile = new File("image.jpg");
ImageIO.write(img2bis, "jpg", outputfile);
					outputfile = new File("image2.jpg");
ImageIO.write(img3bis, "jpg", outputfile);
}
catch(Exception e){}

					System.out.println("OK, on a computé l'image");
		mode = 1;*/
		return result;
	}
	
	protected void paintComponent(Graphics2D g) {
				scale=0;
                if(img!=null && img2!=null)
				{
				//System.out.println("OK ICI");
                int w = img.getWidth(null);
                int h = img.getHeight(null);
                int w2 = img2.getWidth(null);
                int h2 = img2.getHeight(null);
				scale = Math.min((float)this.getWidth()/2/w,(float)this.getHeight()/h);
				
				g.drawImage(img, 0, 0, (int)(w*scale), (int)(h*scale), null);
				g.drawImage(img2, (int)(w*scale), 0, (int)(w2*scale), (int)(h2*scale), null);
				g.setColor(Color.RED); 
				
				/*if(inliers!=null)
				{
				for(Match inlier : inliers)
				{
				g.drawLine((int)(inlier.p1.getX()*scale),(int)(inlier.p1.getY()*scale),(int)(w*scale + inlier.p2.getX()*scale),(int)(inlier.p2.getY()*scale));
				}
				}*/
				
				for(int i=0;i<cor1.size();i++)
				{
					g.setColor(c[i]);
					g.draw(new Ellipse2D.Double(cor1.get(i).getX()*scale,cor1.get(i).getY()*scale,10,10));
				}
				
				
				for(int i=0;i<cor2.size();i++)
				{
					g.setColor(c[i]);
					g.draw(new Ellipse2D.Double(cor2.get(i).getX()*scale+w*scale,cor2.get(i).getY()*scale,10,10));
				}
				
				for(int i=0;i<hom1.size();i++)
				{
					g.setColor(c[i]);
					g.draw(new Line2D.Double(hom1.get(i).getX()*scale,hom1.get(i).getY()*scale,hom2.get(i).getX()*scale+w*scale,hom2.get(i).getY()*scale));
				}
				
				/*if(features1!=null)
				{
					for(Feature f : features1)
					{
					
					g.fillOval((int)(f.location[0]*scale)-3, (int)(f.location[1]*scale)-3,6,6);
					}
				}
				
				if(features2!=null)
				{
					for(Feature f : features2)
					{
					
					g.fillOval((int)(f.location[0]*scale+w*scale)-3, (int)(f.location[1]*scale)-3,6,6);
					}
				}*/
				if(mode==1)
				{
				AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
				g.setComposite(ac);
				g.drawImage(img2bis, (int)(w*scale), 0, (int)(w*scale), (int)(h*scale), null);
				g.drawImage(img3bis, 0, 0, (int)(w2*scale), (int)(h2*scale), null);
				}
                 }
        }
}