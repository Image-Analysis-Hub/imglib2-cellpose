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

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 * Represents the output of Cellpose. Stores masks and flows possibly.
 * 
 * @param <T>
 *            the type of the labels output. Can be {@link UnsignedShortType} or
 *            {@link UnsignedIntType} if N labels > 65k.
 */
public class CellposeOutput< T extends IntegerType< T > & NativeType< T > >
{

	/**
	 * The labels output from Cellpose. Can be {@link UnsignedShortType} or
	 * {@link UnsignedIntType}.
	 */
	public final RandomAccessibleInterval< T > labels;

	/**
	 * The flows output from Cellpose. Always 3 channels. Can be null if the
	 * flows were not returned by Cellpose.
	 */
	public final RandomAccessibleInterval< UnsignedByteType > flows;

	/**
	 * The axes of the labels output.
	 */
	public final AxisInfo axesLabels;

	/**
	 * The axes of the flows output. Can be null if the flows were not returned
	 * by Cellpose.
	 */
	public final AxisInfo axesFlows;

	public CellposeOutput( final RandomAccessibleInterval< T > labels, final AxisInfo axesLabels )
	{
		this( labels, axesLabels, null, null );
	}

	public CellposeOutput( final RandomAccessibleInterval< T > labels, final AxisInfo axesLabels, final RandomAccessibleInterval< UnsignedByteType > flows, final AxisInfo axesFlows )
	{
		this.labels = labels;
		this.axesLabels = axesLabels;
		this.flows = flows;
		this.axesFlows = axesFlows;
	}
}
