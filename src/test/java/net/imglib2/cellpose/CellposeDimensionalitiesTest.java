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

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.function.BiFunction;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;
import org.junit.Test;

import net.imglib2.Dimensions;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.appose.util.ApposeTaskListener;
import net.imglib2.appose.util.AxisInfo;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * JUnit tests that check that the Cellpose functions can properly harness all
 * dimensionality cases.
 */
public class CellposeDimensionalitiesTest
{

	@Test
	public void testCellpose3_XY()
	{
		skipIfTestThresholdLessThan( 6 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.channels( 1, 0 )
				.computeFlows( true )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XY );
	}

	@Test
	public void testCellpose3_XYC()
	{
		skipIfTestThresholdLessThan( 5 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYC );
	}

	@Test
	public void testCellpose3_XYT()
	{
		skipIfTestThresholdLessThan( 6 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYT );
	}

	@Test
	public void testCellpose3_XYCT()
	{
		skipIfTestThresholdLessThan( 5 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCT );
	}

	@Test
	public void testCellpose3_XYZ()
	{
		skipIfTestThresholdLessThan( 7 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0.4 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYZ );
	}

	@Test
	public void testCellpose3_XYZT()
	{
		skipIfTestThresholdLessThan( 12 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0.4 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYZT );
	}

	@Test //?
	public void testCellpose3_XYCZ()
	{
		skipIfTestThresholdLessThan( 7 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0.4 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCZ );
	}

	@Test //?
	public void testCellpose3_XYCZT()
	{
		skipIfTestThresholdLessThan( 29 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0.4 )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCZT );
	}

	@Test
	public void testCellpose3_XYZ_NoStitch()
	{
		skipIfTestThresholdLessThan( 6 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0. )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYZ );
	}

	@Test
	public void testCellpose3_XYZT_NoStitch()
	{
		skipIfTestThresholdLessThan( 13 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0. )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYZT );
	}

	@Test
	public void testCellpose3_XYZT_NoStitch_Mode3D()
	{
		skipIfTestThresholdLessThan( 31 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0. )
				.do3D( true )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYZT );
	}

	@Test
	public void testCellpose3_XYCZ_NoStitch()
	{
		skipIfTestThresholdLessThan( 6 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0. )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCZ );
	}

	@Test
	public void testCellpose3_XYCZT_NoStitch()
	{
		skipIfTestThresholdLessThan( 15 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.stitchThreshold( 0. )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCZT );
	}

	@Test
	public void testCellpose4_XY()
	{
		skipIfTestThresholdLessThan( 53 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XY );
	}

	@Test
	public void testCellpose4_XYC()
	{
		skipIfTestThresholdLessThan( 53 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYC );
	}

	@Test
	public void testCellpose4_XYT()
	{
		skipIfTestThresholdLessThan( 140 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYT );
	}

	@Test
	public void testCellpose4_XYCT()
	{
		skipIfTestThresholdLessThan( 140 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCT );
	}

	@Test
	public void testCellpose4_XYZ()
	{
		skipIfTestThresholdLessThan( 439 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0.4 )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYZ );
	}

	@Test
	public void testCellpose4_XYZT()
	{
		skipIfTestThresholdLessThan( 2178 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0.4 )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYZT );
	}

	@Test
	public void testCellpose4_XYCZ()
	{
		skipIfTestThresholdLessThan( 454 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0.4 )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCZ );
	}

	@Test
	public void testCellpose4_XYCZT()
	{
		skipIfTestThresholdLessThan( 2164 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0.4 )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCZT );
	}

	@Test
	public void testCellpose4_XYZ_NoStitch()
	{
		skipIfTestThresholdLessThan( 448 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYZ );
	}

	@Test
	public void testCellpose4_XYZT_NoStitch_Mode3D()
	{
		skipIfTestThresholdLessThan( 44942 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.do3D( true )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYZT );
	}

	@Test
	public void testCellpose4_XYZT_NoStitch()
	{
		skipIfTestThresholdLessThan( 2179 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYZT );
	}

	@Test
	public void testCellpose4_XYCZ_NoStitch()
	{
		skipIfTestThresholdLessThan( 498 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCZ );
	}

	@Test
	public void testCellpose3_XYCZ_NoStitch_Mode3D()
	{
		skipIfTestThresholdLessThan( 9 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.stitchThreshold( 0. )
				.do3D( true )
				.computeFlows( true )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCZ );
	}

	@Test
	public void testCellpose3_XYCZT_NoStitch_Mode3D()
	{
		skipIfTestThresholdLessThan( 31 );
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.stitchThreshold( 0. )
				.do3D( true )
				.computeFlows( true )
				.build();
		test( cellpose3Runner( params ), CellposeTestDims.XYCZT );
	}

	@Test
	public void testCellpose4_XYCZ_NoStitch_Mode3D()
	{
		skipIfTestThresholdLessThan( 8132 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.do3D( true )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCZ );
	}

	@Test
	public void testCellpose4_XYCZT_NoStitch_Mode3D()
	{
		skipIfTestThresholdLessThan( 40309 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.do3D( true )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCZT );
	}

	@Test
	public void testCellpose4_XYCZT_NoStitch()
	{
		skipIfTestThresholdLessThan( 2166 );
		final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.stitchThreshold( 0. )
				.computeFlows( true )
				.build();
		test( cellpose4Runner( params ), CellposeTestDims.XYCZT );
	}

	private static final BiFunction< RandomAccessibleInterval< UnsignedByteType >, AxisInfo, CellposeOutput< UnsignedShortType > > cellpose3Runner( final Cellpose3Parameters params )
	{
		return ( img, axes ) -> {
			try
			{
				return Cellpose.cellpose3( img, axes, new UnsignedShortType(), params, ApposeTaskListener.STD );
			}
			catch ( BuildException | IOException | InterruptedException | TaskException e )
			{
				e.printStackTrace();
			}
			return null;
		};
	}

	private static final BiFunction< RandomAccessibleInterval< UnsignedByteType >, AxisInfo, CellposeOutput< UnsignedShortType > > cellpose4Runner( final Cellpose4Parameters params )
	{
		return ( img, axes ) -> {
			try
			{
				return Cellpose.cellpose4( img, axes, new UnsignedShortType(), params, ApposeTaskListener.STD );
			}
			catch ( BuildException | IOException | InterruptedException | TaskException e )
			{
				e.printStackTrace();
			}
			return null;
		};
	}

	private void test( final BiFunction< RandomAccessibleInterval< UnsignedByteType >, AxisInfo, CellposeOutput< UnsignedShortType > > runner, final CellposeTestDims dims )
	{
		final RandomAccessibleInterval< UnsignedByteType > input = createTestImgForDims( dims );
		final AxisInfo inputAxes = dims.axes;
		final CellposeOutput< UnsignedShortType > outputs = runner.apply( input, dims.axes );

		// Labels.
		final RandomAccessibleInterval< UnsignedShortType > labels = outputs.labels;
		final AxisInfo outputAxes = outputs.axesLabels;
		testDimSize( input, inputAxes, 1, labels, outputAxes, dims.name(), "Labels" );

		// Flows/
		final RandomAccessibleInterval< UnsignedByteType > flows = outputs.flows;
		final AxisInfo axesFlows = outputs.axesFlows;
		testDimSize( input, inputAxes, 3, flows, axesFlows, dims.name(), "Flows" );
	}

	private static void testDimSize(
			final Dimensions expectedDim,
			final AxisInfo expectedAxes,
			final long expectedNChannels,
			final Dimensions actualDim,
			final AxisInfo actualAxes,
			final String caseName,
			final String imageName )
	{
		final long inputWidth = expectedAxes.nX( expectedDim );
		final long outputWidth = actualAxes.nX( actualDim );
		assertEquals( "For case " + caseName + ": " + imageName + " output and input must have the same X size.", inputWidth, outputWidth );

		// Y
		final long inputHeight = expectedAxes.nY( expectedDim );
		final long outputHeight = actualAxes.nY( actualDim );
		assertEquals( "For case " + caseName + ":" + imageName + " output and input must have the same Y size.", inputHeight, outputHeight );

		// C
		final long outputChannels = actualAxes.nChannels( actualDim );
		assertEquals( "For case " + caseName + ": " + imageName + " output must have " + expectedNChannels + " channel.", expectedNChannels, outputChannels );

		// Z
		final long inputDepth = expectedAxes.nZ( expectedDim );
		final long outputDepth = actualAxes.nZ( actualDim );
		assertEquals( "For case " + caseName + ": " + imageName + " output and input must have the same Z size.", inputDepth, outputDepth );

		// T
		final long inputTimepoints = expectedAxes.nTimePoints( expectedDim );
		final long outputTimepoints = actualAxes.nTimePoints( actualDim );
		assertEquals( "For case " + caseName + ": " + imageName + " output and input must have the same T size.", inputTimepoints, outputTimepoints );
	}

	private static final long X_SIZE = 80;

	private static final long Y_SIZE = 96;

	private static final long Z_SIZE = 11;

	private static final long C_SIZE = 2;

	private static final long T_SIZE = 5;

	private static enum CellposeTestDims
	{
		XY( new long[] { X_SIZE, Y_SIZE }, AxisInfo.XY ),
		XYC( new long[] { X_SIZE, Y_SIZE, C_SIZE }, AxisInfo.XYC ),

		XYT( new long[] { X_SIZE, Y_SIZE, T_SIZE }, AxisInfo.XYT ),
		XYCT( new long[] { X_SIZE, Y_SIZE, C_SIZE, T_SIZE }, AxisInfo.XYCT ),

		XYZ( new long[] { X_SIZE, Y_SIZE, Z_SIZE }, AxisInfo.XYZ ),
		XYCZ( new long[] { X_SIZE, Y_SIZE, C_SIZE, Z_SIZE }, AxisInfo.XYCZ ),

		XYZT( new long[] { X_SIZE, Y_SIZE, Z_SIZE, T_SIZE }, AxisInfo.XYZT ),
		XYCZT( new long[] { X_SIZE, Y_SIZE, C_SIZE, Z_SIZE, T_SIZE }, AxisInfo.XYCZT ),
		;

		private final long[] dims;

		private final AxisInfo axes;

		CellposeTestDims( final long[] dims, final AxisInfo axes )
		{
			this.dims = dims;
			this.axes = axes;
		}
	}

	public static RandomAccessibleInterval< UnsignedByteType > createTestImgForDims( final CellposeTestDims dims )
	{
		final Img< UnsignedByteType > img = ArrayImgs.unsignedBytes( dims.dims );
		// Write a circle at every timepoint.
		writeBlob( img, dims.axes );
		return img;
	}

	private static void writeBlob( final RandomAccessibleInterval< UnsignedByteType > img, final AxisInfo axes )
	{
		RandomAccessibleInterval< UnsignedByteType > view = img;
		if ( axes.T() >= 0 )
		{
			for ( long t = 0; t < img.dimension( axes.T() ); t++ )
			{

				view = Views.hyperSlice( img, axes.T(), t );
				processTimepoint( view, axes.removeTimeDim(), ( int ) t );
			}
		}
		else
		{
			processTimepoint( img, axes, 0 );
		}
	}

	private static void processTimepoint( final RandomAccessibleInterval< UnsignedByteType > img, final AxisInfo axes, final int tp )
	{
		if ( axes.C() < 0 )
		{
			processChannel( img, axes, tp );
		}
		else
		{
			// Write only in channel 0
			final RandomAccessibleInterval< UnsignedByteType > view = Views.hyperSlice( img, axes.C(), 0 );
			processChannel( view, axes.removeChannelDim(), tp );
		}
	}

	private static void processChannel( final RandomAccessibleInterval< UnsignedByteType > imgPlus, final AxisInfo axes, final int tp )
	{
		if ( axes.Z() < 0 )
		{
			writeCircle( imgPlus, tp );
		}
		else
		{
			// Write circle at the middle +/- 3
			for ( long z = Z_SIZE / 2 - 3; z <= Z_SIZE / 2 + 3; z++ )
			{
				final RandomAccessibleInterval< UnsignedByteType > view = Views.hyperSlice( imgPlus, axes.Z(), z );
				writeCircle( view, tp );
			}
		}
	}

	private static void writeCircle( final RandomAccessibleInterval< UnsignedByteType > img, final int tp )
	{
		assert img.numDimensions() == 2;

		final int radius = 30;
		// Slight shift with time.
		final int[] center = new int[] { ( int ) ( Y_SIZE / 2. + 5. * tp ), ( int ) ( Y_SIZE / 2. ) };
		final FinalInterval blob = Intervals.createMinMax( center[ 0 ] - radius, center[ 1 ] - radius, center[ 0 ] + radius, center[ 1 ] + radius );
		Views.interval( img, blob ).forEach( p -> p.set( 200 ) );
	}

	private static void skipIfTestThresholdLessThan( final int minimum )
	{
		final String thresholdValue = System.getenv( "TEST_THRESHOLD" );
		final int threshold = thresholdValue == null ? 0 : Integer.parseInt( thresholdValue );
		assumeTrue( threshold >= minimum );
  }

	// Used for debugging only.
	public static void main( final String[] args )
	{
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 1, 0 )
				.build();

		try
		{
//			final CellposeTestDims[] toTest = new CellposeTestDims[] { CellposeTestDims.XY, CellposeTestDims.XYC, CellposeTestDims.XYT, CellposeTestDims.XYCT };
			final CellposeTestDims[] toTest = new CellposeTestDims[] { CellposeTestDims.XY };
//			final CellposeTestDims[] toTest = CellposeTestDims.values();
			for ( final CellposeTestDims dims : toTest )
			{
				final RandomAccessibleInterval< UnsignedByteType > img = createTestImgForDims( dims );
				System.out.println( '\n' + dims.axes.toString() );
				System.out.println( "Testing case " + dims.name() );

				final CellposeOutput< UnsignedShortType > output = Cellpose.cellpose3( img, dims.axes, new UnsignedShortType(), params, ApposeTaskListener.STD );

				System.out.println( "\nInput shape:           " + Util.printInterval( img ) );
				System.out.println( "Get Labels with shape: " + Util.printInterval( output.labels ) );
				System.out.println( " and flows with shape: " + Util.printInterval( output.flows ) );
			}
			System.out.println( "\nDone." );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}
	}
}
