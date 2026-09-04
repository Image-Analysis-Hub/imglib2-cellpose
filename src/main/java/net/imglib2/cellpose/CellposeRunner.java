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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.apposed.appose.TaskException;

import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.appose.ShmImg;
import net.imglib2.appose.util.AbstractPixiRunner;
import net.imglib2.appose.util.ApposeTaskListener;
import net.imglib2.appose.util.AxisInfo;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.ImgUtil;
import net.imglib2.util.Util;

/**
 * Specialized class that runs Cellpose. This class exists so that we can write
 * results in a pre-allocated output data structure.
 */
public class CellposeRunner< T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > extends AbstractPixiRunner
{

	private final AxisInfo axisInfo;

	private final ShmImg< T > inputShm;

	private final ShmImg< R > outputLabelsShm;

	private final ShmImg< UnsignedByteType > outputFlowsShm;

	private boolean processed = false;

	private CellposeRunner(
			final Map< String, Object > apposeMap,
			final URL cellposeInitScriptPath,
			final URL cellposeRunScriptPath,
			final String envName,
			final ShmImg< T > inputShm,
			final ShmImg< R > outputLabelsShm,
			final ShmImg< UnsignedByteType > outputFlowsShm,
			final ApposeTaskListener listener,
			final Dimensions input,
			final AxisInfo axisInfo )
	{
		super(
				apposeMap,
				CellposeRunner.class.getResource( "/pixi.toml" ),
				CellposeRunner.class.getResource( "/cp_utils.py" ),
				cellposeInitScriptPath,
				cellposeRunScriptPath,
				envName,
				listener );
		this.inputShm = inputShm;
		this.outputLabelsShm = outputLabelsShm;
		this.outputFlowsShm = outputFlowsShm;
		this.axisInfo = axisInfo;
	}

	public void setInput( final RandomAccessibleInterval< T > input )
	{
		processed = false;
		ImgUtil.copy( input, inputShm );
	}

	@Override
	public void run() throws InterruptedException, TaskException, IOException
	{
		super.run();
		processed = true;
	}

	public void getOutputLabels( final RandomAccessibleInterval< R > outputLabels )
	{
		if ( !processed )
			throw new IllegalStateException( "The input image has been set but the task has not been run yet. Please execute run() first." );
		ImgUtil.copy( outputLabelsShm, outputLabels );
	}

	public Img< R > getOutputLabels()
	{
		final Img< R > outputLabels = Util.getArrayOrCellImgFactory( outputLabelsShm, outputLabelsShm.getType() ).create( outputLabelsShm );
		getOutputLabels( outputLabels );
		return outputLabels;
	}

	public void getOutputFlows( final RandomAccessibleInterval< UnsignedByteType > outputFlows )
	{
		if ( !processed )
			throw new IllegalStateException( "The input image has been set but the task has not been run yet. Please execute run() first." );
		if ( outputFlowsShm == null )
			throw new IllegalStateException( "Output flows were not computed." );
		ImgUtil.copy( outputFlowsShm, outputFlows );
	}

	public Img< UnsignedByteType > getOutputFlows()
	{
		if ( outputFlowsShm == null )
			throw new IllegalStateException( "Output flows were not computed." );
		final Img< UnsignedByteType > outputFlows = Util.getArrayOrCellImgFactory( outputFlowsShm, outputFlowsShm.getType() ).create( outputFlowsShm );
		getOutputFlows( outputFlows );
		return outputFlows;
	}

	public CellposeOutput< R > getOutput()
	{
		final Img< R > outputLabels = getOutputLabels();
		final Img< UnsignedByteType > outputFlows = outputFlowsShm != null ? getOutputFlows() : null;
		final AxisInfo axesLabels = axisInfo.removeChannelDim();
		final AxisInfo axesFlows = axesLabels.insertChannelDim( 2 );
		return new CellposeOutput<>( outputLabels, axesLabels, outputFlows, axesFlows );
	}

	@Override
	public void close()
	{
		super.close();
		inputShm.close();
		outputLabelsShm.close();
		if ( outputFlowsShm != null )
			outputFlowsShm.close();
	}

	public static < T extends RealType< T > & NativeType< T > > CellposeRunner< T, UnsignedShortType > create(
			final Cellpose3Parameters params,
			final Dimensions dimension,
			final AxisInfo axisInfo,
			final T inputType,
			final ApposeTaskListener listener )
	{
		return create( params, dimension, axisInfo, inputType, new UnsignedShortType(), listener );
	}

	public static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeRunner< T, R > create(
			final Cellpose3Parameters params,
			final Dimensions dimension,
			final AxisInfo axisInfo,
			final T inputType,
			final R outputType,
			final ApposeTaskListener listener )
	{
		final ShmImg< T > inputShm = createInputShmImg( dimension, inputType );
		final ShmImg< R > outputLabelsShm = createOutputLabelsShmImg( dimension, axisInfo, outputType );
		final ShmImg< UnsignedByteType > outputFlowsShm;
		if ( params.computeFlows )
			outputFlowsShm = createOutputFlowsShmImg( dimension, axisInfo );
		else
			outputFlowsShm = null;
		final Map< String, Object > apposeMap = params.toApposeMap( inputShm, axisInfo, outputLabelsShm, outputFlowsShm );
		final String envName = "cp3-" + getTorchInstallSuffix( params.torchVersion );
		return new CellposeRunner< T, R >(
				apposeMap,
				CellposeRunner.class.getResource( "/cp3_init.py" ),
				CellposeRunner.class.getResource( "/cp3.py" ),
				envName,
				inputShm,
				outputLabelsShm,
				outputFlowsShm,
				listener,
				dimension,
				axisInfo );
	}

	public static < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > CellposeRunner< T, R > create(
			final Cellpose4Parameters params,
			final Dimensions dimension,
			final AxisInfo axisInfo,
			final T inputType,
			final R outputType,
			final ApposeTaskListener listener )
	{
		final ShmImg< T > inputShm = createInputShmImg( dimension, inputType );
		final ShmImg< R > outputLabelsShm = createOutputLabelsShmImg( dimension, axisInfo, outputType );
		final ShmImg< UnsignedByteType > outputFlowsShm;
		if ( params.computeFlows )
			outputFlowsShm = createOutputFlowsShmImg( dimension, axisInfo );
		else
			outputFlowsShm = null;
		final Map< String, Object > apposeMap = params.toApposeMap( inputShm, axisInfo, outputLabelsShm, outputFlowsShm );
		final String envName = "cp4-" + getTorchInstallSuffix( params.torchVersion );
		return new CellposeRunner< T, R >(
				apposeMap,
				CellposeRunner.class.getResource( "/cp4_init.py" ),
				CellposeRunner.class.getResource( "/cp4.py" ),
				envName,
				inputShm,
				outputLabelsShm,
				outputFlowsShm,
				listener,
				dimension,
				axisInfo );
	}

	public static < T extends RealType< T > & NativeType< T > > CellposeRunner< T, UnsignedShortType > create(
			final Cellpose4Parameters params,
			final Dimensions dimension,
			final AxisInfo axisInfo,
			final T inputType,
			final ApposeTaskListener listener )
	{
		return create( params, dimension, axisInfo, inputType, new UnsignedShortType(), listener );
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
	private static < T extends RealType< T > & NativeType< T > > ShmImg< T > createInputShmImg( final Dimensions input, final T type )
	{
		final long[] dims = input.dimensionsAsLongArray();
		final int[] dims2 = new int[ dims.length ];
		for ( int i = 0; i < dims.length; i++ )
			dims2[ i ] = ( int ) dims[ i ];
		return new ShmImg<>( type, dims2 );
	}

	/**
	 * Creates a shared memory image suitable to hold flows output, with the
	 * right dimensions for the specified image input.
	 *
	 * @param input
	 *            the input image.
	 * @param axisInfo
	 *            the AxisInfo of the input image.
	 * @return a new ShmImg.
	 */
	private static ShmImg< UnsignedByteType > createOutputFlowsShmImg( final Dimensions input, final AxisInfo axisInfo )
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
	 * Creates a shared memory image suitable to hold labels output, with the
	 * right dimensions for the specified image input.
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
	private static < R extends IntegerType< R > & NativeType< R > > ShmImg< R > createOutputLabelsShmImg( final Dimensions input, final AxisInfo axisInfo, final R outputType )
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
			// Only the exit code is used. The output must go somewhere the OS
			// drains, not into a pipe: nvidia-smi's default output includes a
			// table of every process holding a CUDA context, which can exceed
			// the ~4 KB pipe buffer and block the child forever in waitFor().
			pb.redirectOutput( new File( getOperatingSystem() == OperatingSystem.WINDOWS
					? "NUL" : "/dev/null" ) );
			final Process process = pb.start();
			return process.waitFor() == 0;
		}
		catch ( final IOException e )
		{
			return false;
		}
		catch ( final InterruptedException e )
		{
			Thread.currentThread().interrupt();
			return false;
		}
	}
}
