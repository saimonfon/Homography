package mpi.cbg.fly;

/**
 * <p>Title: Sift</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2009</p>
 *
 * <p>Company: </p>
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
 * @author Jonathan Odul
 * @link http://www.jidul.com
 * @version 0.1
 */

import java.util.Vector;
import java.util.List;
import java.util.Collections;

import java.awt.Image;

public class SIFT
{
    // steps
    private static int steps = 5;
    public static int steps() { return steps; }
    public static void set_steps(int s) { steps=s; }

    // initial sigma
    private static float initial_sigma = 1.6f;
    public static float initial_sigma() { return initial_sigma; }
    public static void set_initial_sigma(float is) { initial_sigma=is; }

    // feature descriptor size
    private static int fdsize = 4;
    public static int fdsize() { return fdsize; }
    public static void set_fdsize(int fs) { fdsize=fs; }

    // feature descriptor orientation bins
    private static int fdbins = 8;
    public static int fdbins() { return fdbins; }
    public static void fdbins(int fb) { fdbins=fb; }

    // size restrictions for scale octaves, use octaves < max_size and > min_size only
    private static int min_size = 64;
    public static int min_size() { return min_size(); }
    public static void set_min_size(int ms) { min_size=ms; }

    private static int max_size = 1024;
    public static int max_size() { return max_size(); }
    public static void set_max_size(int ms) { max_size=ms; }

    /**
     * @author Jonathan ODUL 2009
     * @link http://www.jidul.com
     * @version 1.0
     * @param w width of the picture
     * @param h height of the picture
     * @param pixels[] tab of pixels rgb color (ex: red 0xff0000)
     * @return vector of features of the picture
     */

    public static Vector< Feature > getFeatures(int w, int h, int pixels[])
    {
        FloatArray2D fa = ImageArrayConverter.ArrayToFloatArray2D( w, h, pixels );

        return getFeatures(fa);
    }
    
    /**
     * @author Jonathan ODUL 2009
     * @link http://www.jidul.com
     * @version 1.0
     * @param imp picture
     * @return vector of features of the picture
     */
	 
    public static Vector< Feature > getFeatures(Image imp)
    {
        if (imp==null)  { System.err.println( "There are no images open" ); return null; }

        FloatArray2D fa = ImageArrayConverter.ImageToFloatArray2D( imp );

        return getFeatures(fa);
    }

    public static Vector< Feature > getFeatures(FloatArray2D fa)
    {
        Vector< Feature > fs1;

        FloatArray2DSIFT sift = new FloatArray2DSIFT( fdsize, fdbins );
        
        Filter.enhance( fa, 1.0f );

        fa = Filter.computeGaussianFastMirror( fa, ( float )Math.sqrt( initial_sigma * initial_sigma - 0.25 ) );

        long start_time = System.currentTimeMillis();
        System.out.print( "processing SIFT ..." );
        sift.init( fa, steps, initial_sigma, min_size, max_size );
        fs1 = sift.run( max_size );
        Collections.sort( fs1 );
        System.out.println( " took " + ( System.currentTimeMillis() - start_time ) + "ms" );

        System.out.println( fs1.size() + " features identified and processed" );

        return fs1;
    }

    /**
	 * identify corresponding features using spatial constraints
	 *
	 * @param fs1 feature collection from set 1 sorted by decreasing size
	 * @param fs2 feature collection from set 2 sorted by decreasing size
	 * @param max_sd maximal difference in size (ratio max/min)
	 * @param model transformation model to be applied to fs2
	 * @param max_id maximal distance in image space ($\sqrt{x^2+y^2}$)
	 *
	 * @return matches
	 *
	 * TODO implement the spatial constraints
	 */
	public static Vector< PointMatch > createMatches(
			List< Feature > fs1,
			List< Feature > fs2,
			float max_sd,
			Model model,
			float max_id )
	{
            Vector< PointMatch > matches = new Vector< PointMatch >();
            float min_sd = 1.0f / max_sd;

            int size = fs2.size();
            int size_1 = size - 1;

            for ( Feature f1 : fs1 )
            {
                Feature best = null;
                float best_d = Float.MAX_VALUE;
                float second_best_d = Float.MAX_VALUE;

                int first = 0;
                int last = size_1;
                int s = size / 2 + size % 2;
                if ( max_sd < Float.MAX_VALUE )
                {
                    while ( s > 1 )
                    {
                        Feature f2 = fs2.get( last );
                        if ( f2.scale / f1.scale < min_sd ) last = Math.max( 0, last - s );
                        else last = Math.min( size_1, last + s );
                        f2 = fs2.get( first );
                        if ( f2.scale / f1.scale < max_sd ) first = Math.max( 0, first - s );
                        else first = Math.min( size_1, first + s );
                        s = s / 2 + s % 2;
                    }
                }

                for ( int i = first; i <= last; ++i )
                {
                    Feature f2 = fs2.get( i );
                    float d = f1.descriptorDistance( f2 );
                    if ( d < best_d )
                    {
                        second_best_d = best_d;
                        best_d = d;
                        best = f2;
                    }
                    else if ( d < second_best_d )
                            second_best_d = d;
                }
                if ( best != null && second_best_d < Float.MAX_VALUE && best_d / second_best_d < max_id )
                        matches.addElement(
                                        new PointMatch(
                                                        new Point(
                                                                        new float[] { f1.location[ 0 ], f1.location[ 1 ] } ),
                                                        new Point(
                                                                        new float[] { best.location[ 0 ], best.location[ 1 ] } ),
                                                        ( f1.scale + best.scale ) / 2.0f ) );
            }

            // now remove ambiguous matches
            for ( int i = 0; i < matches.size(); )
            {
                boolean amb = false;
                PointMatch m = matches.get( i );
                float[] m_p2 = m.getP2().getL();
                for ( int j = i + 1; j < matches.size(); )
                {
                    PointMatch n = matches.get( j );
                    float[] n_p2 = n.getP2().getL();
                    if ( m_p2[ 0 ] == n_p2[ 0 ] && m_p2[ 1 ] == n_p2[ 1 ] )
                    {
                        amb = true;
                        matches.removeElementAt( j );
                    }
                    else ++j;
                }
                if ( amb )
                {
                        matches.removeElementAt( i );
                }
                else ++i;
            }
            return matches;
	}
}