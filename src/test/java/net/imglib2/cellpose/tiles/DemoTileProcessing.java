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
package net.imglib2.cellpose.tiles;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.plugin.RGBStackMerge;
import ij.process.ImageProcessor;
import ij.process.LUT;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.appose.util.ApposeTaskListener;
import net.imglib2.appose.util.AxisInfo;
import net.imglib2.cellpose.Cellpose3BuiltinModels;
import net.imglib2.cellpose.Cellpose3Parameters;
import net.imglib2.cellpose.CellposeRunner;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import net.imglib2.view.fluent.RandomAccessibleIntervalView;

public class DemoTileProcessing
{
	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws BuildException, IOException, InterruptedException, TaskException
	{
		try
		{
			ImageJ.main( args );

			// Input image.
			final String filePath = "samples/IDR-1.tif";
			final ImagePlus imp = IJ.openImage( filePath );
			final Img< T > img = ImageJFunctions.wrap( imp );

			// Output ImagePlus.
			final ImagePlus outputImp = NewImage.createShortImage( "Cellpose output", ( int ) img.dimension( 0 ), ( int ) img.dimension( 1 ), 1, NewImage.FILL_BLACK );

			// Merge channels and set LUT.
			final ImagePlus merged = RGBStackMerge.mergeChannels( new ImagePlus[] { imp, outputImp }, false );
			merged.setTitle( "Cellpose Tile Processing Demo" );
			merged.show();
			merged.setC( 1 );
			useLUT( merged.getChannelProcessor(), LUT.createLutFromColor( Color.WHITE ) );
			merged.setC( 2 );
			useGlasbeyDarkLUT( merged.getChannelProcessor() );
			merged.setDisplayRange( 0, 250 );

			// Wait for the user to click OK before starting the processing.
			IJ.showMessage( "Click OK to start Cellpose tile processing demo." );

			// Cellpose config.
			final Cellpose3Parameters params = Cellpose3Parameters.builder()
					.model( Cellpose3BuiltinModels.NUCLEI )
					.diameter( 60.0 )
					.resample( true )
					.build();
//			final Cellpose4Parameters params = Cellpose4Parameters.builder()
//					.resample( true )
//					.build();

			// Tiles.
			final int blockSize = 256;
			final int overlap = 20;
			final List< Interval > chunks = Grids.padWithOverlap( img, blockSize, overlap );
			Collections.shuffle( chunks ); // Random order.
			final FinalDimensions blockDims = new FinalDimensions( blockSize, blockSize );

			// Label tile merger.
			final double iouThresh = 0.1;
			final RandomAccessibleInterval< UnsignedShortType > canvas = ArrayImgs.unsignedShorts( img.dimensionsAsLongArray() );
			final LabelTileMerger< UnsignedShortType > merger = new LabelTileMerger<>( canvas, iouThresh );

			// Concurrent processing with Cellpose and tile merging.
			final int nThreads = 4;
			// Split the files in groups.
			final List< List< Interval > > groups = Grids.splitIntoGroups( chunks, nThreads );

			// Process concurrently.
			final long start = System.currentTimeMillis();
			final AxisInfo axisInfo = AxisInfo.XY;
			final ExecutorService executor = Executors.newFixedThreadPool( nThreads );
			try
			{
				final List< Future< ? > > futures = new ArrayList<>();
				for ( final List< Interval > group : groups )
				{
					futures.add( executor.submit( () -> {
						try (
								// The runner.
								final CellposeRunner< T, UnsignedShortType > runner = CellposeRunner.create(
										params,
										blockDims,
										axisInfo,
										img.getType(),
										ApposeTaskListener.VOID );)
						{
							runner.init();

							// Process the group of tiles.
							for ( final Interval tileInterval : group )
							{

								// Input tile -> Cellpose input data location.
								copyInput( img, runner, tileInterval, blockDims );

								// Run Cellpose.
								runner.run();
								final Img< UnsignedShortType > cellposeOutputData = runner.getOutputLabels();

								// For display: separate closed label id
								LabelShuffleUtil.shuffleLabelsInPlace( cellposeOutputData );

								// Copy at most the size of the interval from the cellpose output data.
								merger.addTile( cellposeOutputData, tileInterval );

								// Cellpose output tile -> output ImagePlus.
								copyOutput( cellposeOutputData, merged, tileInterval );
								merged.updateAndDraw();
							}
						}
						catch ( final Exception e )
						{
							throw new RuntimeException( e );
						}
					} ) );
				}

				for ( final Future< ? > f : futures )
					f.get(); // wait and propagate exceptions
			}
			finally
			{
				executor.shutdown();
				final long end = System.currentTimeMillis();
				System.out.println( String.format( "Done in: %.2f seconds", ( end - start ) / 1000. ) );
			}

			// Merge all tiles and display results.
			merger.finish();

			// For display: separate closed label id
			LabelShuffleUtil.shuffleLabelsInPlace( canvas );

			// Copy final merged labels to the output ImagePlus.
			copyOutput( canvas, merged, canvas );
			merged.setDisplayRange( 0, 500 );
			merged.updateAndDraw();
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
		}

	}

	/**
	 * Copy the Cellpose output labels contained in the tile interval to the
	 * output ImagePlus. The Cellpose output ShmImg is supposed to be at origin
	 * (0, 0) and of size equal to the tile size.
	 *
	 * @param output
	 *            the Cellpose output ShmImg containing the labels for the tile
	 * @param target
	 *            the output ImagePlus where to copy the labels
	 * @param interval
	 *            the tile interval defining the location of the tile in the
	 *            output ImagePlus
	 */
	private static synchronized < T extends IntegerType< T > > void copyOutput( final RandomAccessibleInterval< T > output, final ImagePlus target, final Interval interval )
	{
		// Basic copy to the output ImagePlus.
		final Cursor< T > c = output.localizingCursor();
		while ( c.hasNext() )
		{
			c.fwd();
			final int x = ( int ) ( c.getIntPosition( 0 ) + interval.min( 0 ) );
			if ( x >= target.getWidth() )
				continue;
			final int y = ( int ) ( c.getIntPosition( 1 ) + interval.min( 1 ) );
			if ( y >= target.getHeight() )
				continue;
			final int val = c.get().getInteger();
			target.getProcessor().set( x, y, val );
		}
	}

	/**
	 * Copy the data of the input image contained in the tile interval to the
	 * Cellpose input ShmImg. The Cellpose image is supposed to be at origin (0,
	 * 0) and of size equal to the tile size. The input image must be defined
	 * over all the tile interval.
	 *
	 * @param <T>
	 *            the pixel type
	 * @param input
	 *            the input image
	 * @param target
	 *            the Cellpose input ShmImg
	 * @param interval
	 *            the tile interval
	 * @param blockDims
	 *            the dimensions of the input we defined the runner with
	 */
	private static < T extends RealType< T > & NativeType< T > > void copyInput(
			final Img< T > input,
			final CellposeRunner< T, UnsignedShortType > runner,
			final Interval interval,
			final Dimensions blockDims )
	{
		final RandomAccessibleIntervalView< T > viewInput = input.view()
				.interval( interval )
				.zeroMin();
		final Interval inputSize = new FinalInterval( blockDims );
		final RandomAccessibleIntervalView< T > viewInputExtended = Views.extendZero( viewInput ).view().interval( inputSize );
		runner.setInput( viewInputExtended );
	}

	private static LUT loadLutFromResource( final String resourcePath )
	{
		try (InputStream is = DemoTileProcessing.class.getResourceAsStream( resourcePath );
				BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ))
		{

			if ( is == null )
			{
				IJ.error( "LUT resource not found: " + resourcePath );
				return null;
			}

			final byte[] reds = new byte[ 256 ];
			final byte[] greens = new byte[ 256 ];
			final byte[] blues = new byte[ 256 ];
			String line;
			int index = 0;

			while ( ( line = reader.readLine() ) != null && index < 256 )
			{
				line = line.trim();
				if ( line.isEmpty() )
					continue; // Skip empty lines

				// Split by whitespace
				final String[] parts = line.split( "\\s+" );
				if ( parts.length >= 3 )
				{
					reds[ index ] = ( byte ) Integer.parseInt( parts[ 0 ] );
					greens[ index ] = ( byte ) Integer.parseInt( parts[ 1 ] );
					blues[ index ] = ( byte ) Integer.parseInt( parts[ 2 ] );
					index++;
				}
			}

			if ( index != 256 )
			{
				IJ.error( "Invalid LUT file: expected 256 entries, found " + index );
				return null;
			}

			return new LUT( reds, greens, blues );
		}
		catch ( final IOException e )
		{
			IJ.error( "Failed to load LUT: " + e.getMessage() );
			return null;
		}
	}

	private static final void useGlasbeyDarkLUT( final ImageProcessor ip )
	{
		final LUT lut = loadLutFromResource( "/glasbey_on_dark.lut" );
		useLUT( ip, lut );
	}

	private static final void useLUT( final ImageProcessor ip, final LUT lut )
	{
		ip.setLut( lut );
	}

}
