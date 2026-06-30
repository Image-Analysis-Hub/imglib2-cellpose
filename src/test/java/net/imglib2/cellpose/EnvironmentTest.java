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
	public void createEnvironment()
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
			Assert.fail("Got an exception when installing environment: "+e);
			e.printStackTrace();
		}
	
	}

}
