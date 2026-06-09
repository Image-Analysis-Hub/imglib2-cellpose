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
package net.imglib2.cellpose.tiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.IntegerType;

public class LabelShuffleUtil
{
	/**
	 * Shuffles non-zero labels in-place in the given label image.
	 * <p>
	 * Background label 0 is preserved. All other distinct labels are randomly
	 * permuted, so that each original label is mapped to exactly one shuffled
	 * label and vice versa.
	 *
	 * @param labels
	 *            label image to shuffle in-place
	 * @param rng
	 *            random generator controlling the permutation
	 * @param <T>
	 *            integer pixel type
	 */
	public static < T extends IntegerType< T > > void shuffleLabelsInPlace( final RandomAccessibleInterval< T > labels, final Random rng )
	{
		final Set< Integer > labelSet = new LinkedHashSet<>();
		final Cursor< T > scan = labels.cursor();

		while ( scan.hasNext() )
		{
			final int v = scan.next().getInteger();
			if ( v > 0 )
				labelSet.add( v );
		}

		if ( labelSet.isEmpty() )
			return;

		final List< Integer > originalLabels = new ArrayList<>( labelSet );
		final List< Integer > shuffledLabels = new ArrayList<>( labelSet );
		Collections.shuffle( shuffledLabels, rng );

		final Map< Integer, Integer > relabelMap = new HashMap<>();
		for ( int i = 0; i < originalLabels.size(); i++ )
			relabelMap.put( originalLabels.get( i ), shuffledLabels.get( i ) );

		final Cursor< T > write = labels.cursor();
		while ( write.hasNext() )
		{
			final T px = write.next();
			final int v = px.getInteger();
			if ( v > 0 )
				px.setInteger( relabelMap.get( v ) );
		}
	}

	/**
	 * Convenience overload using a new Random().
	 */
	public static < T extends IntegerType< T > > void shuffleLabelsInPlace( final RandomAccessibleInterval< T > labels )
	{
		shuffleLabelsInPlace( labels, new Random() );
	}
}
