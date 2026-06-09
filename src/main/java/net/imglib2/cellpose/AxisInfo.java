/*-
 * #%L
 * Running Cellpose 3 and 4 from Java with Appose, using ImgLib2 data structure.
 * %%
 * Copyright (C) 2026 Appose developpers
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the ImgLib2 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imglib2.cellpose;

import java.util.List;

import net.imglib2.Dimensions;

/**
 * Class representing the dimensionality of an image.
 * <p>
 * It is used to specify which dimensions of an image correspond to the X, Y, Z,
 * channel and time axes. The values of the axes are the indices of the
 * dimensions in the image, or -1 if the image does not have that axis. For
 * example, an AxisInfo with X=0, Y=1, C=2, Z=-1, T=-1 represents an image with
 * dimensions (X,Y,C), while an AxisInfo with X=0 Y=1, Z=2, C=-1, T=-1
 * represents an image with dimensions (X,Y,Z).
 * 
 * @param X
 *            the index of the X axis, or -1 if the image does not have an X
 *            axis.
 * @param Y
 *            the index of the Y axis, or -1 if the image does not have a Y
 *            axis.
 * @param C
 *            the index of the channel axis, or -1 if the image does not have a
 *            channel axis.
 * @param Z
 *            the index of the Z axis, or -1 if the image does not have a Z
 *            axis.
 * @param T
 *            the index of the time axis, or -1 if the image does not have a
 *            time axis.
 */
public record AxisInfo( int X, int Y, int C, int Z, int T )
{

	public static final AxisInfo XY = new AxisInfo( 0, 1, -1, -1, -1 );

	public static final AxisInfo XYC = new AxisInfo( 0, 1, 2, -1, -1 );

	public static final AxisInfo XYZ = new AxisInfo( 0, 1, -1, 2, -1 );

	public static final AxisInfo XYT = new AxisInfo( 0, 1, -1, -1, 2 );

	public static final AxisInfo XYCZ = new AxisInfo( 0, 1, 2, 3, -1 );

	public static final AxisInfo XYCT = new AxisInfo( 0, 1, 2, -1, 3 );

	public static final AxisInfo XYZT = new AxisInfo( 0, 1, -1, 2, 3 );

	public static final AxisInfo XYCZT = new AxisInfo( 0, 1, 2, 3, 4 );

	/**
	 * Returns a new AxisInfo with the same values as this one, but with
	 * dimensionality swapped the Python order. T
	 * 
	 * @return a new AxisInfo.
	 */
	public AxisInfo toPython()
	{
		final int nDims = nDims();
		final int nX = X < 0 ? -1 : nDims - X - 1;
		final int nY = Y < 0 ? -1 : nDims - Y - 1;
		final int nZ = Z < 0 ? -1 : nDims - Z - 1;
		final int nC = C < 0 ? -1 : nDims - C - 1;
		final int nT = T < 0 ? -1 : nDims - T - 1;
		return new AxisInfo( nX, nY, nC, nZ, nT );
	}

	/**
	 * Returns the number of non singleton dimensions, i.e. the number of
	 * dimensions with size > 1.
	 * 
	 * @return the number of non singleton dimensions.
	 */
	public int nDims()
	{
		int nd = 0;
		for ( final int d : new int[] { X, Y, C, Z, T } )
			if ( d >= 0 )
				nd++;
		return nd;
	}

	/**
	 * Returns the number of channels in the specified image, provided this
	 * AxisInfo properly represents the axes of the image. If the image does not
	 * have a channel axis, this method returns 1 (there is always at least one
	 * channel).
	 * 
	 * @param img
	 *            the image.
	 * @return the number of channels in the image.
	 */
	public long nChannels( final Dimensions img )
	{
		if ( C < 0 )
			return 1l;
		return img.dimension( C );
	}

	/**
	 * Returns the number of time points in the specified image, provided this
	 * AxisInfo properly represents the axes of the image. If the image does not
	 * have a time axis, this method returns 1 (there is always at least one
	 * time point).
	 * 
	 * @param img
	 *            the image.
	 * @return the number of time points in the image.
	 */
	public long nTimePoints( final Dimensions img )
	{
		if ( T < 0 )
			return 1l;
		return img.dimension( T );
	}

	/**
	 * Returns the number of pixels in the X dimension of the specified image,
	 * provided this AxisInfo properly represents the axes of the image. If the
	 * image does not have an X axis, this method throws an
	 * IllegalStateException.
	 * 
	 * @param input
	 *            the image.
	 * @return the number of pixels in the X dimension of the image.
	 */
	public long nX( final Dimensions input )
	{
		if ( X < 0 )
			throw new IllegalStateException( "This AxisInfo does not have an X axis" );
		return input.dimension( X );
	}

	/**
	 * Returns the number of pixels in the Y dimension of the specified image,
	 * provided this AxisInfo properly represents the axes of the image. If the
	 * image does not have a Y axis, this method throws an
	 * IllegalStateException.
	 * 
	 * @param input
	 *            the image.
	 * @return the number of pixels in the Y dimension of the image.
	 */
	public long nY( final Dimensions input )
	{
		if ( Y < 0 )
			throw new IllegalStateException( "This AxisInfo does not have a Y axis" );
		return input.dimension( Y );
	}

	/**
	 * Returns the number of Z slices in the specified image, provided this
	 * AxisInfo properly represents the axes of the image. If the image does not
	 * have a Z axis, this method returns 1 (there is always at least one Z
	 * slice).
	 * 
	 * @param input
	 *            the image.
	 * @return the number of Z slices in the image.
	 */
	public long nZ( final Dimensions input )
	{
		if ( Z < 0 )
			return 1l;
		return input.dimension( Z );
	}

	/**
	 * Returns a new AxisInfo with the same values as this one, but with the
	 * time axis removed, i.e. with the same values for X,Y,Z,C <b>possibly
	 * shifted if the time axis was before them in the order of dimensions</b>
	 * and and with T set to -1.
	 * 
	 * @return a new AxisInfo with the time axis removed.
	 */
	public AxisInfo removeTimeDim()
	{
		if ( T < 0 )
			return this;

		final int nX = ( T < X ) ? X - 1 : X;
		final int nY = ( T < Y ) ? Y - 1 : Y;
		final int nC = ( T < C ) ? C - 1 : C;
		final int nZ = ( T < Z ) ? Z - 1 : Z;
		return new AxisInfo( nX, nY, nC, nZ, -1 );
	}

	/**
	 * Returns a new AxisInfo with the same values as this one, but with the Z
	 * axis removed, i.e. with the same values for X,Y,C,T <b>possibly shifted
	 * if the Z axis was before them in the order of dimensions</b> and with Z
	 * set to -1.
	 * 
	 * @return a new AxisInfo with the Z axis removed.
	 */
	public AxisInfo removeZDim()
	{
		if ( Z < 0 )
			return this;

		final int nX = ( Z < X ) ? X - 1 : X;
		final int nY = ( Z < Y ) ? Y - 1 : Y;
		final int nC = ( Z < C ) ? C - 1 : C;
		final int nT = ( Z < T ) ? T - 1 : T;
		return new AxisInfo( nX, nY, nC, -1, nT );
	}

	/**
	 * Returns a new AxisInfo with the same values as this one, but with the
	 * channel axis removed, i.e. with the same values for X,Y,Z,T <b>possibly
	 * shifted if the channel axis was before them in the order of
	 * dimensions</b> and with C set to -1.
	 * 
	 * @return a new AxisInfo with the channel axis removed.
	 */
	public AxisInfo removeChannelDim()
	{
		if ( C < 0 )
			return this;

		final int nX = ( C < X ) ? X - 1 : X;
		final int nY = ( C < Y ) ? Y - 1 : Y;
		final int nZ = ( C < Z ) ? Z - 1 : Z;
		final int nT = ( C < T ) ? T - 1 : T;
		return new AxisInfo( nX, nY, -1, nZ, nT );
	}

	/**
	 * Returns a new AxisInfo with the same values as this one, but with a
	 * channel axis inserted at the specified position, i.e. with the same
	 * values for X,Y Z,T <b>possibly shifted if the channel axis is inserted
	 * before them in the order of dimensions</b> and with C set to the
	 * specified position.
	 * 
	 * @param pos
	 *            the position at which to insert the channel axis.
	 * @return a new AxisInfo.
	 * @throws IllegalStateException
	 *             if this AxisInfo already has a channel axis.
	 */
	public AxisInfo insertChannelDim( final int pos )
	{
		if ( C >= 0 )
			throw new IllegalStateException( "This AxisInfo already has a channel axis" );

		final int nX = ( pos <= X ) ? X + 1 : X;
		final int nY = ( pos <= Y ) ? Y + 1 : Y;
		final int nZ = ( pos <= Z ) ? Z + 1 : Z;
		final int nT = ( pos <= T ) ? T + 1 : T;
		return new AxisInfo( nX, nY, pos, nZ, nT );
	}

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		final String str = "XYCZT";
		final List< Integer > list = List.of( X, Y, C, Z, T );
		for ( int i = 0; i < 5; i++ )
		{
			final int d = list.get( i );
			if ( d >= 0 )
				sb.append( str.charAt( i ) );
		}
		return sb.toString();
	}
}
