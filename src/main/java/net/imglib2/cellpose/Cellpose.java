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
import net.imglib2.appose.ShmImg;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.ImgUtil;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
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
	 * @param img
	 *            the input image. X and Y axes must be at positions 0 and 1
	 *            respectively.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param pythonScriptPath
	 *            the path to the Python script to run (e.g. "/cp3.py" or
	 *            "/cp4.py").
	 * @param envName
	 *            the name of the Python environment to create and use (e.g.
	 *            "cp3" or "cp4").
	 * @return a list containing the label image, and optionally the flows
	 *         image. If flows are not computed, the list will contain only the
	 *         label image.
	 */
	private static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeOutput< R > run(
			final RandomAccessibleInterval< T > input,
			final AxisInfo axisInfo,
			final R outputType,
			final CellposeParameters params,
			final String pythonInitScriptPath,
			final String pythonScriptPath,
			final String envName,
			final ApposeTaskListener listener ) throws BuildException, IOException, InterruptedException, TaskException
	{
		if ( axisInfo.X() != 0 || axisInfo.Y() != 1 )
			throw new IllegalArgumentException( "X and Y axes must be at positions 0 and 1 respectively." );

		// Placeholders declaration.
		final ShmImg< T > inputShm;
		final AxisInfo inputAxisInfo;
		final ShmImg< R > outputLabelsShm;
		final ShmImg< UnsignedByteType > outputFlowsShm;

		// Do we have a 5D image? If yes we process timepoint by timepoint.
		final long nt = axisInfo.nTimePoints( input );
		final long nz = axisInfo.nZ( input );

		if ( nt > 1 && nz > 1 )
		{
			// Temp image won't have time dim.
			inputAxisInfo = axisInfo.removeTimeDim();
			// We create placeholders for a single timepoint.
			final IntervalView< T > singleTP = Views.hyperSlice( input, axisInfo.T(), 0 );
			inputShm = createInputShmImg( singleTP );
			outputLabelsShm = createOutputLabelsShmImg( singleTP, axisInfo.removeTimeDim(), outputType );
			if ( params.computeFlows )
				outputFlowsShm = createOutputFlowsShmImg( singleTP, axisInfo.removeTimeDim() );
			else
				outputFlowsShm = null;
		}
		else
		{
			inputAxisInfo = axisInfo;
			// We create placeholders for the whole image.
			inputShm = createInputShmImg( input );
			outputLabelsShm = createOutputLabelsShmImg( input, axisInfo, outputType );
			if ( params.computeFlows )
				outputFlowsShm = createOutputFlowsShmImg( input, axisInfo );
			else
				outputFlowsShm = null;
		}

		// Create the runner, configured on the ShmImg.
		try (final CellposeRunner< T, R > runner = new CellposeRunner<>(
				params,
				pythonInitScriptPath,
				pythonScriptPath,
				envName,
				listener,
				inputShm,
				inputAxisInfo,
				outputLabelsShm,
				outputFlowsShm ))
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
					final RandomAccessibleInterval< T > inputTp = Views.hyperSlice( input, axisInfo.T(), t );

					// Labels output reslice.
					final RandomAccessibleInterval< R > outputLabelsTp = Views.hyperSlice( outputLabels, 3, t );

					// Flows output reslice.
					final RandomAccessibleInterval< UnsignedByteType > outputFlowsTp;
					if ( params.computeFlows )
						outputFlowsTp = Views.hyperSlice( outputFlows, 4, t );
					else
						outputFlowsTp = null;

					// Write input slice into the shared memory placeholder.
					ImgUtil.copy( inputTp, inputShm );

					// Exec and write output in the right place.
					runner.run();

					// Write output in the resliced output images.
					ImgUtil.copy( outputLabelsShm, outputLabelsTp );
					if ( params.computeFlows )
						ImgUtil.copy( outputFlowsShm, outputFlowsTp );
				}

				// Close placeholder ShmImgs.
				inputShm.close();
				outputLabelsShm.close();
				if ( params.computeFlows )
					outputFlowsShm.close();

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
				// Write input in the shared memory placeholder.
				ImgUtil.copy( input, inputShm );
				runner.run();

				// And return with the shared image we created.
				final AxisInfo axesLabels = axisInfo.removeChannelDim();
				final AxisInfo axesFlows = axesLabels.insertChannelDim( 2 );
				return new CellposeOutput< R >( outputLabelsShm, axesLabels, outputFlowsShm, axesFlows );
			}
		}
	}

	/**
	 * Creates an empty shared memory image with the same dimensions and pixel
	 * type as the input.
	 * 
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param input
	 *            the input image.
	 * @return a new ShmImg.
	 */
	public static < T extends RealType< T > & NativeType< T > > ShmImg< T > createInputShmImg( final RandomAccessibleInterval< T > input )
	{
		return createInputShmImg( input, input.getType().createVariable() );
	}

	/**
	 * Creates an empty shared memory image with the specified dimensions and
	 * pixel type.
	 * 
	 * @param <T>
	 *            the pixel type.
	 * @param input
	 *            the dimensions.
	 * @param type
	 *            the pixel type.
	 * @return a new ShmImg.
	 */
	public static < T extends RealType< T > & NativeType< T > > ShmImg< T > createInputShmImg( final Dimensions input, final T type )
	{
		final long[] dims = input.dimensionsAsLongArray();
		final int[] dims2 = new int[ dims.length ];
		for ( int i = 0; i < dims.length; i++ )
			dims2[ i ] = ( int ) dims[ i ];
		return new ShmImg<>( type, dims2 );
	}

	/**
	 * Creates a shared memory image suitable to hold Cellpose flows output,
	 * with the right dimensions for the specified image input.
	 * 
	 * @param input
	 *            the input image.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @return a new ShmImg.
	 */
	public static ShmImg< UnsignedByteType > createOutputFlowsShmImg( final Dimensions input, final AxisInfo axisInfo )
	{
		final long[] dims = input.dimensionsAsLongArray();
		if ( axisInfo.C() < 0 )
		{
			final int[] dims2 = new int[ dims.length + 1 ];
			dims2[ 0 ] = ( int ) dims[ 0 ];
			dims2[ 1 ] = ( int ) dims[ 1 ];
			dims2[ 2 ] = 3; // 3 channels for the flows.
			for ( int i = 2; i < dims.length; i++ )
				dims2[ i + 1 ] = ( int ) dims[ i ];
			return new ShmImg<>( new UnsignedByteType(), dims2 );
		}
		final int[] dims2 = new int[ dims.length ];
		for ( int i = 0; i < dims.length; i++ )
		{
			if ( i == axisInfo.C() )
				dims2[ i ] = 3; // 3 channels for the flows.
			else
				dims2[ i ] = ( int ) dims[ i ];
		}
		return new ShmImg<>( new UnsignedByteType(), dims2 );
	}

	/**
	 * Creates a shared memory image suitable to hold Cellpose labels output,
	 * with the right dimensions for the specified image input.
	 * 
	 * @param <R>
	 *            the pixel type of the output label image.
	 * @param input
	 *            the input image.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @param outputType
	 *            the desired pixel type for the output labels image. It can be
	 *            either UnsignedShortType or UnsignedIntType (if the number of
	 *            labels in one image is larger than 65k).
	 * @return a new ShmImg.
	 */
	public static < R extends IntegerType< R > & NativeType< R > > ShmImg< R > createOutputLabelsShmImg( final Dimensions input, final AxisInfo axisInfo, final R outputType )
	{
		final long[] dims = input.dimensionsAsLongArray();
		if ( axisInfo.C() < 0 )
		{
			final int[] dims2 = new int[ dims.length ];
			for ( int i = 0; i < dims.length; i++ )
				dims2[ i ] = ( int ) dims[ i ];
			return new ShmImg< R >( outputType, dims2 );
		}
		// We drop the channel dim.
		final int[] dims2 = new int[ dims.length - 1 ];
		int j = 0;
		for ( int i = 0; i < dims.length; i++ )
		{
			if ( i != axisInfo.C() )
			{
				dims2[ j ] = ( int ) dims[ i ];
				j++;
			}
		}
		return new ShmImg< R >( outputType, dims2 );
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
		final String envName = "cp3-" + getTorchInstallSuffix( params.torchVersion );
		final String pythonScriptPath = "/cp3.py";
		final String pythonInitScriptPath = "/cp3_init.py";
		return run( img, axisInfo, outputType, params, pythonInitScriptPath, pythonScriptPath, envName, listener );
	}

	/**
	 * Creates a CellposeRunner configured to run Cellpose 3 with the given
	 * parameters, and read and write intput and output in the specified
	 * ShmImgs.
	 * <p>
	 * The runner is useful if you want to run Cellpose multiple times on
	 * different images, but with the same parameters, as it allows to reuse the
	 * same Python environment and the same shared memory placeholders for input
	 * and output, which can save time. In this case, you can create the runner
	 * once with this method, and then call the {@link CellposeRunner#run()}
	 * method multiple times, after writing new input data in the input ShmImg
	 * and reading the output data from the output ShmImgs.
	 * 
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param <R>
	 *            the pixel type of the output label image. It can be either
	 *            UnsignedShortType or UnsignedIntType (if the number of labels
	 *            in one image is larger than 65k).
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 * @param input
	 *            the shared memory image to use as input for the
	 *            CellposeRunner. Consumers will want to write the input image
	 *            data into this ShmImg before calling the runner's run()
	 *            method.
	 * @param inputAxisInfo
	 *            the AxisInfo of the input image.
	 * @param outputLabels
	 *            the shared memory image to use as output for the labels of the
	 *            CellposeRunner. Consumers will want to read the output label
	 *            image data from this ShmImg after calling the runner's run()
	 *            method.
	 * @param outputLabels
	 *            the shared memory image to use as output for the flows of the
	 *            CellposeRunner. Consumers will want to read the output flows
	 *            image data from this ShmImg after calling the runner's run()
	 *            method.
	 * @param outputFlows
	 *            the shared memory image to use as output for the flows of the
	 *            CellposeRunner. Consumers will want to read the output flows
	 *            image data from this ShmImg after calling the runner's run()
	 *            method. It can be <code>null</code> if flows are not computed.
	 * @return a CellposeRunner configured to run Cellpose 3.
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
	public static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeRunner< T, R > cellposeRunner(
			final Cellpose3Parameters params,
			final ApposeTaskListener listener,
			final ShmImg< T > input,
			final AxisInfo inputAxisInfo,
			final ShmImg< R > outputLabels,
			final ShmImg< UnsignedByteType > outputFlows ) throws BuildException, IOException, InterruptedException, TaskException
	{
		final String envName = "cp3-" + getTorchInstallSuffix( params.torchVersion );
		final String pythonScriptPath = "/cp3.py";
		final String pythonInitScriptPath = "/cp3_init.py";
		
		return new CellposeRunner<>(
				params,
				pythonInitScriptPath,
				pythonScriptPath,
				envName,
				listener,
				input,
				inputAxisInfo,
				outputLabels,
				outputFlows );
	}

	/**
	 * Creates a CellposeRunner configured to run Cellpose SAM with the given
	 * parameters, and read and write intput and output in the specified
	 * ShmImgs.
	 * <p>
	 * The runner is useful if you want to run Cellpose multiple times on
	 * different images, but with the same parameters, as it allows to reuse the
	 * same Python environment and the same shared memory placeholders for input
	 * and output, which can save time. In this case, you can create the runner
	 * once with this method, and then call the {@link CellposeRunner#run()}
	 * method multiple times, after writing new input data in the input ShmImg
	 * and reading the output data from the output ShmImgs.
	 * 
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param <R>
	 *            the pixel type of the output label image. It can be either
	 *            UnsignedShortType or UnsignedIntType (if the number of labels
	 *            in one image is larger than 65k).
	 * @param params
	 *            the parameters to run Cellpose with.
	 * @param listener
	 *            the listener to receive progress updates and messages during
	 *            the execution of the Cellpose task.
	 * @param input
	 *            the shared memory image to use as input for the
	 *            CellposeRunner. Consumers will want to write the input image
	 *            data into this ShmImg before calling the runner's run()
	 *            method.
	 * @param inputAxisInfo
	 *            the AxisInfo of the input image.
	 * @param outputLabels
	 *            the shared memory image to use as output for the labels of the
	 *            CellposeRunner. Consumers will want to read the output label
	 *            image data from this ShmImg after calling the runner's run()
	 *            method.
	 * @param outputLabels
	 *            the shared memory image to use as output for the flows of the
	 *            CellposeRunner. Consumers will want to read the output flows
	 *            image data from this ShmImg after calling the runner's run()
	 *            method.
	 * @param outputFlows
	 *            the shared memory image to use as output for the flows of the
	 *            CellposeRunner. Consumers will want to read the output flows
	 *            image data from this ShmImg after calling the runner's run()
	 *            method. It can be <code>null</code> if flows are not computed.
	 * @return a CellposeRunner configured to run Cellpose SAM.
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
	public static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeRunner< T, R > cellposeRunner(
			final Cellpose4Parameters params,
			final ApposeTaskListener listener,
			final ShmImg< T > input,
			final AxisInfo inputAxisInfo,
			final ShmImg< R > outputLabels,
			final ShmImg< UnsignedByteType > outputFlows ) throws BuildException, IOException, InterruptedException, TaskException
	{
		final String envName = "cp4-" + getTorchInstallSuffix( params.torchVersion );
		final String pythonScriptPath = "/cp4.py";
		final String pythonInitScriptPath = "/cp4_init.py";
		return new CellposeRunner<>(
				params,
				pythonInitScriptPath,
				pythonScriptPath,
				envName,
				listener,
				input,
				inputAxisInfo,
				outputLabels,
				outputFlows );
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
		final String envName = "cp4-" + getTorchInstallSuffix( params.torchVersion );
		final String pythonScriptPath = "/cp4.py";
		final String pythonInitScriptPath = "/cp4_init.py";
		
		return run( img, axisInfo, outputType, params, pythonInitScriptPath, pythonScriptPath, envName, listener );
	}

	/**
	 * Filters and returns the suffix to use for installing the correct version
	 * of PyTorch.
	 * <p>
	 * This method checks the operating system and CUDA availability to
	 * determine the appropriate suffix for installing PyTorch. If you are on a
	 * Mac or do not have CUDA available, it returns "cpu". Otherwise, it
	 * returns the specified torchVersion.
	 * 
	 * @param torchVersion
	 *            the version of PyTorch to install if CUDA is available.
	 * @return the suffix to use for installing the correct version of PyTorch.
	 */
	private static String getTorchInstallSuffix( final String torchVersion )
	{
		// if MacOS, return "-cpu"
		if ( getOperatingSystem() == OperatingSystem.MACOS )
			return "cpu";

		if ( !hasCUDA() )
			return "cpu";

		return torchVersion;
	}

	/** Enum representing the main operating systems. */
	public enum OperatingSystem
	{
		WINDOWS, LINUX, MACOS, UNKNOWN
	}

	/**
	 * Returns the current operating system.
	 * 
	 * @return the current operating system.
	 */
	private static OperatingSystem getOperatingSystem()
	{
		final String os = System.getProperty( "os.name" ).toLowerCase();
		if ( os.contains( "mac" ) || os.contains( "darwin" ) )
			return OperatingSystem.MACOS;
		if ( os.contains( "win" ) )
			return OperatingSystem.WINDOWS;
		if ( os.contains( "nux" ) || os.contains( "nix" ) || os.contains( "aix" ) )
			return OperatingSystem.LINUX;
		return OperatingSystem.UNKNOWN;
	}

	/**
	 * Checks if CUDA is available on the system by trying to execute
	 * {@code nvidia-smi}. This method returns {@code false} on macOS, as CUDA
	 * is not supported on that platform.
	 * 
	 * @return {@code true} if CUDA is available, {@code false} otherwise.
	 */
	private static Boolean hasCUDA()
	{
		if ( getOperatingSystem() == OperatingSystem.MACOS )
			return false;
		try
		{
			// try to run nvidia-smi to check if it is available
			final ProcessBuilder pb = new ProcessBuilder( "nvidia-smi" );
			pb.redirectErrorStream( true );
			final Process process = pb.start();
			process.waitFor();
			return process.exitValue() == 0;
		}
		catch ( final IOException | InterruptedException e )
		{
			return false;
		}
	}

	/** Prevent instantiation of this utility class. */
	private Cellpose()
	{}
}
