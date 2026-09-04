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

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;

import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.appose.util.ApposeTaskListener;
import net.imglib2.appose.util.AxisInfo;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/**
 * Main class to run Cellpose 3 or Cellpose-SAM from Java, using Appose to
 * manage Python environments and processes, and using ImgLib2 data structures
 * as input and output.
 */
public class Cellpose
{

	/**
	 * Core method to run Cellpose 3 or Cellpose-SAM, depending on the
	 * specification of the script and environment to use. To be used by other
	 * methods in this class.
	 *
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param input
	 *            the input image. X and Y axes must be at positions 0 and 1
	 *            respectively.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param pythonScriptPath
	 *            the path to the Python script to run (e.g. "/cp3.py" or
	 *            "/cp4.py").
	 * @return a list containing the label image, and optionally the flows
	 *         image. If flows are not computed, the list will contain only the
	 *         label image.
	 */
	private static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeOutput< R > run(
			final RandomAccessibleInterval< T > input,
			final AxisInfo axisInfo,
			final R outputType,
			final CellposeParameters params,
			final ApposeTaskListener listener ) throws BuildException, IOException, InterruptedException, TaskException
	{
		if ( axisInfo.X() != 0 || axisInfo.Y() != 1 )
			throw new IllegalArgumentException( "X and Y axes must be at positions 0 and 1 respectively." );

		// Do we have a 5D image? If yes we process timepoint by timepoint.
		final long nt = axisInfo.nTimePoints( input );
		final long nz = axisInfo.nZ( input );

		Dimensions inputImgDims;
		AxisInfo inputAxisInfo;

		if ( nt > 1 && nz > 1 )
		{
			// Drop time.
			inputAxisInfo = axisInfo.removeTimeDim();
			inputImgDims = Views.hyperSlice( input, axisInfo.T(), 0 );
		}
		else
		{
			inputAxisInfo = axisInfo;
			inputImgDims = input;
		}

		// Create the runner, depending on CP3 or CP4 parameters.
		final CellposeRunner< T, R > runner;
		if ( params instanceof Cellpose3Parameters )
			runner = CellposeRunner.create( ( Cellpose3Parameters ) params, inputImgDims, inputAxisInfo, input.getType(), outputType, listener );
		else if ( params instanceof Cellpose4Parameters )
			runner = CellposeRunner.create( ( Cellpose4Parameters ) params, inputImgDims, inputAxisInfo, input.getType(), outputType, listener );
		else
			throw new IllegalArgumentException( "Unknown CellposeParameters type: " + params.getClass().getName() );

		try (runner)
		{
			runner.init();

			if ( nt > 1 && nz > 1 )
			{
				// Placeholder for full labels output: XYZT.
				final long[] inputDims = input.dimensionsAsLongArray();
				final long[] ldims = new long[] {
						inputDims[ axisInfo.X() ],
						inputDims[ axisInfo.Y() ],
						inputDims[ axisInfo.Z() ],
						inputDims[ axisInfo.T() ] };
				final Dimensions labelsDim = FinalDimensions.wrap( ldims );
				final Img< R > outputLabels = Util.getArrayOrCellImgFactory( labelsDim, outputType ).create( ldims );

				// Placeholder for flows output if needed.
				final Img< UnsignedByteType > outputFlows;
				if ( params.computeFlows )
				{
					// XYCZT, with nC = 3 for the 3 flows.
					final long[] fdims = new long[] {
							ldims[ 0 ],
							ldims[ 1 ],
							3,
							ldims[ 2 ],
							ldims[ 3 ] };
					// 3 channels in the flows output
					outputFlows = Util.getArrayOrCellImgFactory( labelsDim, new UnsignedByteType() ).create( fdims );
				}
				else
				{
					outputFlows = null;
				}

				/*
				 * Process time point by time point.
				 */

				for ( int t = 0; t < nt; t++ )
				{
					// Input reslice.
					runner.setInput( Views.hyperSlice( input, axisInfo.T(), t ) );

					// Execute
					runner.run();

					// Labels output reslice.
					runner.getOutputLabels( Views.hyperSlice( outputLabels, 3, t ) );

					// Flows output reslice.
					if ( params.computeFlows )
						runner.getOutputFlows( Views.hyperSlice( outputFlows, 4, t ) );
				}

				// Return all time-points.
				@SuppressWarnings( { "rawtypes", "unchecked" } )
				final CellposeOutput< R > out = new CellposeOutput(
						outputLabels,
						axisInfo.removeChannelDim(),
						outputFlows,
						( axisInfo.C() < 0 ) ? axisInfo.insertChannelDim( 2 ) : axisInfo );
				return out;
			}
			else
			{
				// Otherwise process in one go.
				runner.setInput( input );
				runner.run();
				return runner.getOutput();
			}
		}
	}

	/**
	 * Run Cellpose 3 with the given parameters on the given image, and return
	 * the resulting label image, and optionally the flows. This method uses
	 * UnsignedShortType for the output labels, which is suitable for images
	 * with up to 65k labels. If you expect more than 65k labels in one image,
	 * please use the other cellpose3 method where you can specify the output
	 * label type (UnsignedIntType).
	 *
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param img
	 *            the input image. X and Y must be the first dimensions.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 * @return a {@link CellposeOutput} object containing the label image, and
	 *         optionally the flows image.
	 * @throws BuildException
	 *             if installing and building the Python environment fails.
	 * @throws IOException
	 *             if reading the Python scripts or environment specifications
	 *             fails.
	 * @throws InterruptedException
	 *             if the Python process is interrupted while running.
	 * @throws TaskException
	 *             if executing the Python script fails.
	 */
	public static < T extends RealType< T > & NativeType< T > > CellposeOutput< UnsignedShortType > cellpose3(
			final RandomAccessibleInterval< T > img,
			final AxisInfo axisInfo,
			final Cellpose3Parameters params,
			final ApposeTaskListener listener ) throws BuildException, IOException, InterruptedException, TaskException
	{
		return cellpose3( img, axisInfo, new UnsignedShortType(), params, listener );
	}

	/**
	 * Run Cellpose 3 with the given parameters on the given image, and return
	 * the resulting label image, and optionally the flows.
	 *
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param <R>
	 *            the pixel type of the output label image. It can be either
	 *            UnsignedShortType or UnsignedIntType (if the number of labels
	 *            in one image is larger than 65k).
	 * @param img
	 *            the input image. X and Y axes must be at positions 0 and 1
	 *            respectively. If not, a {@link IllegalArgumentException} is
	 *            thrown.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param outputType
	 *            the desired pixel type for the output labels image. It can be
	 *            either UnsignedShortType or UnsignedIntType (if the number of
	 *            labels in one image is larger than 65k).
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 *
	 * @return a {@link CellposeOutput} object containing the label image, and
	 *         optionally the flows image.
	 *
	 * @throws BuildException
	 *             if installing and building the Python environment fails.
	 * @throws IOException
	 *             if reading the Python scripts or environment specifications
	 *             fails.
	 * @throws InterruptedException
	 *             if the Python process is interrupted while running.
	 * @throws TaskException
	 *             if executing the Python script fails.
	 */
	public static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeOutput< R > cellpose3(
			final RandomAccessibleInterval< T > img,
			final AxisInfo axisInfo,
			final R outputType,
			final Cellpose3Parameters params,
			final ApposeTaskListener listener ) throws BuildException, IOException, InterruptedException, TaskException
	{
		return run( img, axisInfo, outputType, params, listener );
	}

	/**
	 * Run Cellpose-SAM with the given parameters on the given image, and return
	 * the resulting label image, and optionally the flows. This method uses
	 * UnsignedShortType for the output labels, which is suitable for images
	 * with up to 65k labels. If you expect more than 65k labels in one image,
	 * please use the other cellpose4 method where you can specify the output
	 * label type (UnsignedIntType).
	 *
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param img
	 *            the input image. X and Y must be the first dimensions.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 * @return a {@link CellposeOutput} object containing the label image, and
	 *         optionally the flows image.
	 * @throws BuildException
	 *             if installing and building the Python environment fails.
	 * @throws IOException
	 *             if reading the Python scripts or environment specifications
	 *             fails.
	 * @throws InterruptedException
	 *             if the Python process is interrupted while running.
	 * @throws TaskException
	 *             if executing the Python script fails.
	 */
	public static < T extends RealType< T > & NativeType< T > > CellposeOutput< UnsignedShortType > cellpose4(
			final RandomAccessibleInterval< T > img,
			final AxisInfo axisInfo,
			final Cellpose4Parameters params,
			final ApposeTaskListener listener ) throws BuildException, IOException, InterruptedException, TaskException
	{
		return cellpose4( img, axisInfo, new UnsignedShortType(), params, listener );
	}

	/**
	 * Run Cellpose-SAM with the given parameters on the given image, and return
	 * the resulting label image, and optionally the flows.
	 *
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param <R>
	 *            the pixel type of the output label image. It can be either
	 *            UnsignedShortType or UnsignedIntType (if the number of labels
	 *            in one image is larger than 65k).
	 * @param img
	 *            the input image. X and Y axes must be at positions 0 and 1
	 *            respectively. If not, a {@link IllegalArgumentException} is
	 *            thrown.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param outputType
	 *            the desired pixel type for the output labels image. It can be
	 *            either UnsignedShortType or UnsignedIntType (if the number of
	 *            labels in one image is larger than 65k).
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 *
	 * @return a {@link CellposeOutput} object containing the label image, and
	 *         optionally the flows image.
	 *
	 * @throws BuildException
	 *             if installing and building the Python environment fails.
	 * @throws IOException
	 *             if reading the Python scripts or environment specifications
	 *             fails.
	 * @throws InterruptedException
	 *             if the Python process is interrupted while running.
	 * @throws TaskException
	 *             if executing the Python script fails.
	 */
	public static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeOutput< R > cellpose4(
			final RandomAccessibleInterval< T > img,
			final AxisInfo axisInfo,
			final R outputType,
			final Cellpose4Parameters params,
			final ApposeTaskListener listener ) throws BuildException, IOException, InterruptedException, TaskException
	{
		return run( img, axisInfo, outputType, params, listener );
	}

	/** Prevent instantiation of this utility class. */
	private Cellpose()
	{}
}
