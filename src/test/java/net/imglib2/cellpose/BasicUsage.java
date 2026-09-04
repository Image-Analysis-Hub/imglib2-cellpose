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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.appose.util.ApposeTaskListener;
import net.imglib2.appose.util.AxisInfo;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class BasicUsage
{

	public static void main( final String[] args ) throws BuildException, IOException, InterruptedException, TaskException
	{
		basicUsage( args );
		outputType( args );
		cellposeRunner( args );
	}

	public static < T extends RealType< T > & NativeType< T > > void outputType( final String[] args ) throws BuildException, IOException, InterruptedException, TaskException
	{
		final ImagePlus imp = IJ.openImage( "http://imagej.net/images/blobs.gif" );
		final Img< T > img = ImageJFunctions.wrap( imp );

		final RandomAccessibleInterval< T > input = img;
		final AxisInfo inputAxes = AxisInfo.XY;
		final ApposeTaskListener listener = ApposeTaskListener.STD;
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.channels( 1, 0 )
				.computeFlows( true )
				.build();

		final CellposeOutput< UnsignedIntType > output = Cellpose.cellpose3(
				input,
				inputAxes,
				new UnsignedIntType(),
				params,
				listener );

		@SuppressWarnings( "unused" )
		final RandomAccessibleInterval< UnsignedIntType > labels = output.labels;
		@SuppressWarnings( "unused" )
		final RandomAccessibleInterval< UnsignedByteType > flows = output.flows;
	}

	public static < T extends RealType< T > & NativeType< T > > void basicUsage( final String[] args ) throws BuildException, IOException, InterruptedException, TaskException
	{
		// Demo preparation. We use IJ for this one.
		ImageJ.main( args );
		final ImagePlus imp = IJ.openImage( "http://imagej.net/images/blobs.gif" );
		imp.show();
		final Img< T > img = ImageJFunctions.wrap( imp );

		// Input
		final RandomAccessibleInterval< T > input = img;
		// You need to specify the dimensionality of your input
		final AxisInfo inputAxes = AxisInfo.XY;

		// Get messages about installing and processing
		final ApposeTaskListener listener = ApposeTaskListener.STD;

		// Specify the parameters for Cellpose 3
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
		    .model( Cellpose3BuiltinModels.CYTO2 )
		    .channels( 1, 0 )
		    .computeFlows( true )
		    .build();

		final CellposeOutput< UnsignedShortType > output = Cellpose.cellpose3( input, inputAxes, params, listener );

		final RandomAccessibleInterval< UnsignedShortType > labels = output.labels;
		final RandomAccessibleInterval< UnsignedByteType > flows = output.flows;

		ImageJFunctions.show( labels ).setTitle( "Cellpose output" );
		ImageJFunctions.show( flows ).setTitle( "Cellpose flows" );
	}

	public static void cellposeRunner( final String[] args ) throws BuildException, IOException, InterruptedException, TaskException
	{
		// Create fake images. Replace by your own images here.
		// The constraint is that the images need to have the same dimensions
		// and axes.
		final int nImages = 10;
		final int width = 512;
		final int height = width;
		final AxisInfo axes = AxisInfo.XY;

		final List< RandomAccessibleInterval< UnsignedShortType > > outputImages = new ArrayList<>( nImages );
		final List< RandomAccessibleInterval< UnsignedByteType > > inputImages = new ArrayList<>( nImages );
		for ( int i = 0; i < nImages; i++ )
		{
			final RandomAccessibleInterval< UnsignedByteType > img = ArrayImgs.unsignedBytes( width, height );
			inputImages.add( img );
		}

		// Specify the parameters for Cellpose 3. Adjust to your needs.
		final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.channels( 1, 0 )
				.computeFlows( true )
				.build();

		// Time everything.
		long startTime = System.currentTimeMillis();

		// Now we create the Cellpose runner and the shared tmp images in a
		// try-with-resources block. This way we are sure that the shared tmp
		// images are and the Cellpose runner are properly closed and cleaned up
		// after use.
		try (
				final CellposeRunner< UnsignedByteType, UnsignedShortType > runner = CellposeRunner.create(
						params,
						inputImages.get( 0 ),
						axes,
						inputImages.get( 0 ).getType(),
						ApposeTaskListener.VOID ))
		{
			System.out.println( String.format( "Runner and placeholders creation time: %.2f seconds", ( System.currentTimeMillis() - startTime ) / 1000. ) );

			// Initialize the runner. This will deploy the Python environment
			// and script if not already done, and prepare everything for
			// running Cellpose.
			startTime = System.currentTimeMillis();
			runner.init();
			System.out.println( String.format( "Runner initialization time: %.2f seconds", ( System.currentTimeMillis() - startTime ) / 1000. ) );

			// Run Cellpose on each image.
			for ( int i = 0; i < nImages; i++ )
			{
				System.out.println( String.format( "\nProcessing image %d/%d", i + 1, nImages ) );
				final RandomAccessibleInterval< UnsignedByteType > input = inputImages.get( i );

				// Copy the input image to the tmp location.
				startTime = System.currentTimeMillis();
				runner.setInput( input );
				System.out.println( String.format( "Input copy time: %.2f seconds", ( System.currentTimeMillis() - startTime ) / 1000. ) );

				// Run Cellpose. The results will be written in the tmpLabels
				// and tmpFlows images.
				startTime = System.currentTimeMillis();
				runner.run();
				System.out.println( String.format( "Cellpose run time: %.2f seconds", ( System.currentTimeMillis() - startTime ) / 1000. ) );

				// Copy the output to a new image.
				startTime = System.currentTimeMillis();
				final RandomAccessibleInterval< UnsignedShortType > outputLabels = ArrayImgs.unsignedShorts( input.dimensionsAsLongArray() );
				runner.getOutputLabels();
				System.out.println( String.format( "Output copy to a new image time: %.2f seconds", ( System.currentTimeMillis() - startTime ) / 1000. ) );
				outputImages.add( outputLabels );
			}

			startTime = System.currentTimeMillis();
		}
		System.out.println( String.format( "Closing shared resources time: %.2f seconds", ( System.currentTimeMillis() - startTime ) / 1000. ) );

		// At this point the runner and the tmp shared memory images are
		// closed and cleaned up, and you can safely exit or do other things
		// with the output images.
	}

}
