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
import java.net.http.WebSocket.Listener;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;
import org.junit.Assert;
import org.junit.Test;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.appose.ShmImg;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * JUnit tests that check that the appose environment are correctly installed/activated.
 */
public class EnvironmentTest 
{
	
	@Test 
	public void createEnvironmentCP3()
	{
		int[] dims = new int[] { 300, 300 };
		try {
			ShmImg<UnsignedByteType> shimg = new ShmImg<>( new UnsignedByteType(), dims );
			ShmImg<UnsignedByteType> shout = new ShmImg<>( new UnsignedByteType(), dims );
			
		
			final Cellpose3Parameters params = Cellpose3Parameters.builder()
				.model( Cellpose3BuiltinModels.CYTO2 )
				.computeFlows( true )
				.channels( 0, 0 )
				.build();
			final String envName = "cp3-cpu";
			final String pythonScriptPath = "/cp3.py";
			final String pythonInitScriptPath = "/cp3_init.py";
		
			CellposeRunner<UnsignedByteType, UnsignedByteType> cprun = new CellposeRunner<>(
					params,
					pythonInitScriptPath,
					pythonScriptPath,
					envName,
					ApposeTaskListener.STD,
					shimg,
					AxisInfo.XY,
					shout,
					null );
			
				cprun.init();
				cprun.close();
			
		}
		catch ( BuildException | IOException | InterruptedException | TaskException e )
		{
			Assert.fail("Got an exception when installing environment CP3: "+e);
			e.printStackTrace();
		}
	
	}

	@Test 
	public void createEnvironmentCP4()
	{
		int[] dims = new int[] { 300, 300 };
		try {
			ShmImg<UnsignedByteType> shimg = new ShmImg<>( new UnsignedByteType(), dims );
			ShmImg<UnsignedByteType> shout = new ShmImg<>( new UnsignedByteType(), dims );
			
		
			final Cellpose4Parameters params = Cellpose4Parameters.builder()
				.model( Cellpose4BuiltinModels.CPSAM )
				.computeFlows( false )
				.build();
			final String envName = "cp4-cpu";
			final String pythonScriptPath = "/cp4.py";
			final String pythonInitScriptPath = "/cp4_init.py";
		
			CellposeRunner<UnsignedByteType, UnsignedByteType> cprun = new CellposeRunner<>(
					params,
					pythonInitScriptPath,
					pythonScriptPath,
					envName,
					ApposeTaskListener.STD,
					shimg,
					AxisInfo.XY,
					shout,
					null );
			
				cprun.init();
				cprun.close();
			
		}
		catch ( BuildException | IOException | InterruptedException | TaskException e )
		{
			Assert.fail("Got an exception when installing environment CP4: "+e);
			e.printStackTrace();
		}
	
	}

}
