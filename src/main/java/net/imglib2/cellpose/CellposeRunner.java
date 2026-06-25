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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apposed.appose.Appose;
import org.apposed.appose.BuildException;
import org.apposed.appose.Environment;
import org.apposed.appose.Service;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;
import org.apposed.appose.TaskException;

import net.imglib2.appose.ShmImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

/**
 * Specialized class that runs Cellpose. This class exists so that we can write
 * results in a pre-allocated output data structure.
 * <p>
 * Why this class? Normally we can run Cellpose on up to 3D or 2D+T images, with
 * or without C. As soon as we have 3D images over time as input, Cellpose will
 * crash. One solution is to have Cellpose process one time-point after another,
 * writing the results of processing one time-point in a pre-allocated output
 * data structure. This class is made to support this approach.
 * 
 */
public class CellposeRunner< T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > implements AutoCloseable
{

	private final String envName;

	private final String pythonScriptPath;
	
	private final String pythonInitScriptPath;

	private final ApposeTaskListener listener;

	private final Map< String, Object > inputsParams;

	private Service python;

	private String cellposeScript;
	

	/**
	 * Instantiates a Cellpose runner.
	 * 
	 * @param params
	 *            the Cellpose parameters to use for running Cellpose.
	 * @param pythonScriptPath
	 *            the path to the Cellpose Python script.
	 * @param envName
	 *            the name of the Cellpose Python environment to use.
	 * @param listener
	 *            the listener to use for receiving updates from the Python
	 *            script and environment deployment.
	 * @param input
	 *            a placeholder for the input image. Every <code>run()</code>
	 *            call use the image data written in this placeholder.
	 * @param inputAxisInfo
	 *            the axis info of the input image. Will be used for every call
	 *            to <code>run()</code>.
	 * @param outputLabels
	 *            a placeholder for the output labels image. Every
	 *            <code>run()</code> call writes the labels results in this
	 *            placeholder. It is the caller responsibility to ensure that
	 *            this placeholder has the right dimensions and pixel type.
	 * @param outputFlows
	 *            a placeholder for the output flows image. Every
	 *            <code>run()</code> call writes the flows results in this
	 *            placeholder. It is the caller responsibility to ensure that
	 *            this placeholder has the right dimensions. Can be
	 *            <code>null</code> if flows are not computed.
	 */
	CellposeRunner( final CellposeParameters params,
			final String pythonInitScriptPath,
			final String pythonScriptPath,
			final String envName,
			final ApposeTaskListener listener,
			final ShmImg< T > input,
			final AxisInfo inputAxisInfo,
			final ShmImg< R > outputLabels,
			final ShmImg< UnsignedByteType > outputFlows )
	{
		this.pythonScriptPath = pythonScriptPath;
		this.pythonInitScriptPath = pythonInitScriptPath;
		this.envName = envName;
		this.listener = listener;
		this.inputsParams = params.toApposeMap( input, inputAxisInfo, outputLabels, outputFlows );
	}

	/**
	 * Runs Cellpose on the input image currently written in the input
	 * placeholder, and writes the results in the output placeholders, with the
	 * parameters specified at construction time.
	 * 
	 * @throws InterruptedException
	 *             if the thread is interrupted while waiting for the Python
	 *             script to finish.
	 * @throws TaskException
	 *             if executing the Python script fails.
	 */
	public void run() throws InterruptedException, TaskException
	{
		// The Python task.
		final Task task = python.task( cellposeScript, inputsParams );

		final long start = System.currentTimeMillis();
		// To catch update message from the python script
		task.listen( listener.taskListener() );
		task.start();
		// Wait for task completion.
		task.waitFor();

		// Verify that it worked.
		if ( task.status != TaskStatus.COMPLETE )
			throw new RuntimeException( "Python script failed with error: " + task.error );

		// Benchmark.
		final long end = System.currentTimeMillis();
		listener.message( "Model inference done in " + ( end - start ) / 1000. + " s" );
	}

	/**
	 * Initializes the Cellpose runner. Must be called before calling run.
	 * 
	 * @throws IOException
	 *             if there is an error reading the pixi.toml file.
	 * @throws BuildException
	 *             if there is an error building the environment.
	 * @throws TaskException 
	 * @throws InterruptedException 
	 */
	public void init() throws IOException, BuildException, InterruptedException, TaskException
	{
		// Python env. specifications.
		final String cellposeEnv = pixiEnv();

		// Create Python env.
		final Environment env = Appose
				.pixi()
				.content( cellposeEnv )
				.subscribeProgress( listener.progressListener() )
				.subscribeOutput( listener.outputListener() )
				.subscribeError( listener.errorListener() )
				.build();
		final String utilsScript = IOUtils.toString( Cellpose.class.getResource( "/cp_utils.py" ), StandardCharsets.UTF_8 );
		this.python = env.activate(envName).python().init( utilsScript );

		// The Python initialization task.
		final String cellposeInitScript =  IOUtils.toString( Cellpose.class.getResource( pythonInitScriptPath ), StandardCharsets.UTF_8 );
		final Task task = python.task( cellposeInitScript, inputsParams );

		final long start = System.currentTimeMillis();
		// To catch update message from the python script
		task.listen( listener.taskListener() );
		task.start();
		// Wait for task completion.
		task.waitFor();

		// Verify that it worked.
		if ( task.status != TaskStatus.COMPLETE )
			throw new RuntimeException( "Python script failed with error: " + task.error );

		// Benchmark.
		final long end = System.currentTimeMillis();
		listener.message( "Cellpose initialization done in " + ( end - start ) / 1000. + " s" );
		
		// The runner script
		this.cellposeScript = IOUtils.toString( Cellpose.class.getResource( pythonScriptPath ), StandardCharsets.UTF_8 );
	}

	@Override
	public void close()
	{
		python.close();
	}

	/**
	 * Returns the content of the pixi.toml file to build the environment return
	 * throws IOException
	 */
	public static String pixiEnv() throws IOException
	{
		final URL pixiFile = CellposeRunner.class.getResource( "/pixi.toml" );
		final String env = IOUtils.toString( pixiFile, StandardCharsets.UTF_8 );
		return env;
	}
}
