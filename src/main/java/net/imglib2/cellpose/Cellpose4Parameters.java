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

import java.util.Map;

import net.imglib2.appose.ShmImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class Cellpose4Parameters extends CellposeParameters
{

	public final Integer chan0; // as Integer so that it can be null

	public final Integer chan1;

	public final Integer chan2;

	private Cellpose4Parameters(
			final Integer chan0,
			final Integer chan1,
			final Integer chan2,
			final String customModel,
			final double diameter,
			final boolean do3D,
			final boolean normalize,
			final double flowThreshold,
			final double cellProbThreshold,
			final boolean useGpu,
			final double minSize,
			final double anisotropy,
			final double stitchThreshold,
			final boolean resample,
			final double tileOverlap,
			final boolean computeFlows,
			final int flow3dSmooth,
			final int nIter,
			final String torchVersion)
	{
		super(
				customModel, diameter, do3D, normalize, flowThreshold,
				cellProbThreshold, useGpu, minSize, anisotropy,
				stitchThreshold, resample, tileOverlap, computeFlows,
				flow3dSmooth, nIter, torchVersion );
		this.chan0 = chan0;
		this.chan1 = chan1;
		this.chan2 = chan2;
	}

	@Override
	public < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > Map< String, Object > toApposeMap(
			final ShmImg< T > input,
			final AxisInfo axisInfo,
			final ShmImg< R > outputLabels,
			final ShmImg< UnsignedByteType > outputFlows )
	{
		final Map< String, Object > inputs = super.toApposeMap( input, axisInfo, outputLabels, outputFlows );

		final long nChannels = axisInfo.nChannels( input );
		inputs.put( "n_channels", nChannels );
		inputs.put( "chan0", chan0 );
		inputs.put( "chan1", chan1 );
		inputs.put( "chan2", chan2 );
		return inputs;
	}

	// Static builder method for convenience
	public static Builder builder()
	{
		return new Builder();
	}

	// Builder class for fluent construction
	public static class Builder extends CellposeParameters.Builder< Builder >
	{

		private Integer chan0 = 0;  // to have one selected by default

		private Integer chan1 = null;

		private Integer chan2 = null;

		public Builder chan0( final Integer chan0 )
		{
			this.chan0 = chan0;
			return this;
		}

		public Builder chan1( final Integer chan1 )
		{
			this.chan1 = chan1;
			return this;
		}

		public Builder chan2( final Integer chan2 )
		{
			this.chan2 = chan2;
			return this;
		}

		@Override
		public Cellpose4Parameters build()
		{
			return new Cellpose4Parameters(
					chan0, chan1, chan2, customModel, diameter, do3D, normalize,
					flowThreshold, cellProbThreshold, useGpu, minSize,
					anisotropy, stitchThreshold, resample, tileOverlap,
					computeFlows, flow3dSmooth, nIter, torchVersion );
		}
	}

	public static Cellpose4Parameters defaultParameters()
	{
		return builder().build();
	}
}
