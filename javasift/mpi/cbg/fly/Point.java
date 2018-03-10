package mpi.cbg.fly;
/**
 * a generic n-dimensional point location
 * 
 * keeps local coordinates final, application of a model changes the world
 * coordinates of the point
 * 
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * NOTE:
 * The SIFT-method is protected by U.S. Patent 6,711,293: "Method and
 * apparatus for identifying scale invariant features in an image and use of
 * same for locating an object in an image" by the University of British
 * Columbia.  That is, for commercial applications the permission of the author
 * is required.
 *
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.1b
 */


public class Point
{
	/**
	 * world coordinates
	 */
	private float[] w;
	final public float[] getW() { return w; }
	
	/**
	 * local coordinates
	 */
	final private float[] l;
	final public float[] getL() { return l; }
	
	/**
	 * Constructor
	 *          
	 * sets this.l to the given float[] reference
	 * 
	 * @param l reference to the local coordinates of the point
	 */
	public Point( float[] l )
	{
		this.l = l;
//		new float[ l.length ];
		w = l.clone();		
	}
	
	/**
	 * apply a model to the point
	 * 
	 * transfers the local coordinates to new world coordinates
	 */
	final public void apply( Model model )
	{
		w = model.apply( l );
	}
	
	/**
	 * estimate the Euclidean distance of two points in the world
	 *  
	 * @param p1
	 * @param p2
	 * @return Euclidean distance
	 */
	final public static float distance( Point p1, Point p2 )
	{
		double sum = 0.0;
		for ( int i = 0; i < p1.w.length; ++i )
		{
			double d = p1.w[ i ] - p2.w[ i ];
			sum += d * d;
		}
		return ( float )Math.sqrt( sum );
	}
}
