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

import java.util.HashMap;
import java.util.Map;

import net.imglib2.appose.ShmImg;
import net.imglib2.appose.util.AxisInfo;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public abstract class CellposeParameters
{

	public final String customModel;

	// Core parameters

	public final double diameter;

	// Thresholds

	public final double flowThreshold;

	public final double cellProbThreshold;

	// Normalization & pre-processing

	public final boolean normalize;

	public final boolean resample;

	// 3D processing

	public final boolean do3D;

	public final double anisotropy;

	public final double stitchThreshold;

	public final int flow3dSmooth;

	// Advanced parameters

	public final boolean useGpu;

	public final double minSize;

	// Advanced processing

	public final double tileOverlap;

	public final boolean computeFlows;

	public final int nIter;

	public final String torchVersion;

	protected CellposeParameters(
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
			final String torchVersion )
	{
		this.customModel = customModel;
		this.diameter = diameter;
		this.do3D = do3D;
		this.normalize = normalize;
		this.flowThreshold = flowThreshold;
		this.cellProbThreshold = cellProbThreshold;
		this.useGpu = useGpu;
		this.minSize = minSize;
		this.anisotropy = anisotropy;
		this.stitchThreshold = stitchThreshold;
		this.resample = resample;
		this.tileOverlap = tileOverlap;
		this.computeFlows = computeFlows;
		this.flow3dSmooth = flow3dSmooth;
		this.nIter = nIter;
		this.torchVersion = torchVersion;
	}

	/**
	 * Creates a parameters map suitable for passing to Appose, using the
	 * specified image as input, and the parameter values stored in this object.
	 *
	 * @param <T>
	 *            the pixel type of the input image.
	 * @param input
	 *            the input image.
	 * @return a new map.
	 */
	public < T extends RealType< T > & NativeType< T >, R extends IntegerType< R > & NativeType< R > > Map< String, Object > toApposeMap(
			final ShmImg< T > input,
			final AxisInfo axisInfo,
			final ShmImg< R > outputLabels,
			final ShmImg< UnsignedByteType > outputFlows )
	{
		final Map< String, Object > inputs = new HashMap<>();

		// Inputs
		inputs.put( "input", input.ndArray() );
		final AxisInfo axisInfoPython = axisInfo.toPython();
		inputs.put( "t_axis", axisInfoPython.T() < 0 ? null : axisInfoPython.T() );
		inputs.put( "z_axis", axisInfoPython.Z() < 0 ? null : axisInfoPython.Z() );
		inputs.put( "channel_axis", axisInfoPython.C() < 0 ? null : axisInfoPython.C() );

		// Outputs
		inputs.put( "output_labels", outputLabels.ndArray() );
		inputs.put( "output_flows", outputFlows == null ? null : outputFlows.ndArray() );

		// Other params.
		inputs.put( "use_3D", do3D );
		// return null if custom model
		final boolean isBuiltInModel = customModel == null || customModel.equals( "" );
		inputs.put( "custom_model", isBuiltInModel ? null : customModel );
		inputs.put( "diameter", diameter );
		inputs.put( "stitch_threshold", stitchThreshold );
		inputs.put( "anisotropy", anisotropy );
		inputs.put( "compute_flows", computeFlows );
		inputs.put( "resample", resample );
		inputs.put( "normalize", normalize );
		inputs.put( "flow_threshold", flowThreshold );
		inputs.put( "cellprob_threshold", cellProbThreshold );
		inputs.put( "min_size", minSize );
		inputs.put( "tile_overlap", tileOverlap );
		inputs.put( "flow3D_smooth", flow3dSmooth );
		inputs.put( "niter", nIter <= 0 ? null : nIter );
		inputs.put( "use_gpu", useGpu );

		return inputs;
	}

	protected static abstract class Builder< B extends Builder< B > >
	{

		protected String customModel = null;

		// Core parameters

		protected double diameter = 30.0;

		// Thresholds

		protected double flowThreshold = 0.4;

		protected double cellProbThreshold = 0.0;

		// Normalization & pre-processing

		protected boolean normalize = true;

		protected boolean resample = false;

		// 3D processing

		protected boolean do3D = false;

		protected double anisotropy = 1.0;

		protected double stitchThreshold = 0.0;

		protected int flow3dSmooth = 0;

		// Advanced parameters

		protected boolean useGpu = true;

		protected double minSize = 15.0;

		// Advanced processing

		protected double tileOverlap = 0.1;

		protected boolean computeFlows = false;

		protected int nIter = 200;

		protected String torchVersion = "cpu";

		@SuppressWarnings( "unchecked" )
		public B customModel( final String customModel )
		{
			this.customModel = customModel;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B diameter( final double diameter )
		{
			this.diameter = diameter;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B do3D( final boolean do3D )
		{
			this.do3D = do3D;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B normalize( final boolean normalize )
		{
			this.normalize = normalize;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B flowThreshold( final double flowThreshold )
		{
			this.flowThreshold = flowThreshold;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B cellProbThreshold( final double cellProbThreshold )
		{
			this.cellProbThreshold = cellProbThreshold;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B useGpu( final boolean useGpu )
		{
			this.useGpu = useGpu;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B minSize( final double minSize )
		{
			this.minSize = minSize;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B anisotropy( final double anisotropy )
		{
			this.anisotropy = anisotropy;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B stitchThreshold( final double stitchThreshold )
		{
			this.stitchThreshold = stitchThreshold;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B resample( final boolean resample )
		{
			this.resample = resample;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B tileOverlap( final double tileOverlap )
		{
			this.tileOverlap = tileOverlap;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B computeFlows( final boolean computeFlows )
		{
			this.computeFlows = computeFlows;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B flow3dSmooth( final int flow3dSmooth )
		{
			this.flow3dSmooth = flow3dSmooth;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B nIter( final int nIter )
		{
			this.nIter = nIter;
			return ( B ) this;
		}

		@SuppressWarnings( "unchecked" )
		public B torchVersion( final String torchVersion )
		{
			this.torchVersion = torchVersion;
			return ( B ) this;
		}

		public abstract CellposeParameters build();
	}
}
