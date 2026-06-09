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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.loops.LoopBuilder;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Incrementally merges overlapping label-image tiles into a single canvas.
 * <p>
 * Each added tile contains local instance labels, with {@code 0} as background
 * and positive values as object ids. Tiles are written into the provided output
 * canvas according to their world-space interval. Objects overlapping across
 * tile boundaries are unified using an overlap-based IoU criterion.
 * <p>
 * The canvas stores provisional global labels while tiles are being added.
 * Calling {@link #finish()} remaps the canvas to compact final labels
 * {@code 1..N}. After {@code finish()}, no more tiles may be added.
 * <p>
 * The canvas is provided as a {@link net.imglib2.RandomAccessible}, which may
 * be bounded or unbounded. If the canvas also implements {@link Interval}, tile
 * intervals are validated against its bounds. Otherwise, the merger assumes
 * that all accessed tile positions are valid in the canvas. Because a
 * {@code RandomAccessible} does not necessarily define a finite interval,
 * {@link #finish()} relabels only the union bounding box of the tile intervals
 * that were added, and {@link #snapshotInto(RandomAccessibleInterval)} writes
 * only within the interval of the provided target.
 * 
 * @param <R>
 *            canvas pixel type
 */
public class LabelTileMerger< R extends IntegerType< R > >
{

	private final RandomAccessible< R > canvas;

	private final double iouThreshold;

	private final UnionFind unionFind;

	/**
	 * The min of the interval that were iterated when adding tiles. This is
	 * used to determine the full interval to relabel.
	 */
	private final long[] minFullInterval;

	/**
	 * The max of the interval that were iterated when adding tiles. This is
	 * used to determine the full interval to relabel.
	 */
	private final long[] maxFullInterval;

	private int offset;

	private boolean finished;

	private boolean hasTiles;

	/**
	 * Creates a new incremental label-tile merger.
	 *
	 * @param canvas
	 *            output canvas, initialized to 0 and large enough to contain
	 *            all tile intervals. Will be modified in place as tiles are
	 *            added and when {@link #finish()} is called. Must be considered
	 *            invalid until {@code finish()} is called, as it may contain
	 *            provisional labels.
	 * @param iouThreshold
	 *            IoU threshold in [0, 1] used to merge labels across
	 *            overlapping tiles
	 */
	public LabelTileMerger(
			final RandomAccessible< R > canvas,
			final double iouThreshold )
	{
		if ( canvas == null )
			throw new IllegalArgumentException( "canvas must not be null" );
		if ( iouThreshold < 0.0 || iouThreshold > 1.0 )
			throw new IllegalArgumentException( "iouThreshold must be in [0, 1]" );

		this.canvas = canvas;
		this.iouThreshold = iouThreshold;
		this.unionFind = new UnionFind();
		this.offset = 0;
		this.finished = false;
		this.hasTiles = false;
		this.minFullInterval = new long[ canvas.numDimensions() ];
		this.maxFullInterval = new long[ canvas.numDimensions() ];
	}

	/**
	 * Adds one label tile into the merger.
	 * <p>
	 * The tile is assumed to contain local labels. It is translated into world
	 * / canvas coordinates according to {@code interval}, written into empty
	 * canvas pixels, and matched against existing canvas labels in overlap
	 * regions.
	 * <p>
	 * This method is <code>synchronized</code> to allow calls from multiple
	 * threads, but each call will be processed sequentially. This is required
	 * to maintain a consistent merger state.
	 *
	 * @param inputTile
	 *            label tile to merge. The input tile is not modified by this
	 *            method; its pixels are only read.
	 * @param interval
	 *            world-space interval specifying where the tile belongs in the
	 *            canvas.
	 * @param <T>
	 *            tile pixel type.
	 * 
	 * @throws IllegalStateException
	 *             if {@code finish()} has already been called.
	 */
	public synchronized < T extends IntegerType< T > > void addTile( final RandomAccessibleInterval< T > inputTile, final Interval interval )
	{
		if ( finished )
			throw new IllegalStateException( "Cannot add tile after finish() has been called." );

		// Detect issues.
		validateTile( inputTile, interval, canvas );

		// Only iterate over the interval size.
		final RandomAccessibleInterval< T > tile = cropTileToIntervalSize( inputTile, interval );

		// Is it empty?
		final int tileMax = maxLabel( tile );
		if ( tileMax == 0 )
			return;

		// Update the full interval bounds.
		if ( !hasTiles )
		{
			for ( int d = 0; d < canvas.numDimensions(); d++ )
			{
				minFullInterval[ d ] = interval.min( d );
				maxFullInterval[ d ] = interval.max( d );
			}
			hasTiles = true;
		}
		else
		{
			for ( int d = 0; d < canvas.numDimensions(); d++ )
			{
				minFullInterval[ d ] = Math.min( minFullInterval[ d ], interval.min( d ) );
				maxFullInterval[ d ] = Math.max( maxFullInterval[ d ], interval.max( d ) );
			}
		}

		for ( int lbl = 1; lbl <= tileMax; lbl++ )
			unionFind.add( lbl + offset );

		final RandomAccessibleInterval< T > tileInWorld = translateToInterval( tile, interval );

		final Map< Integer, Integer > oldOverlapAreas = new HashMap<>();
		final Map< Integer, Integer > newOverlapAreas = new HashMap<>();
		final Map< Long, Integer > pairIntersections = new HashMap<>();

		final Cursor< T > tileCursor = tileInWorld.localizingCursor();

		final RandomAccess< R > canvasRA = canvas.randomAccess( interval );
		while ( tileCursor.hasNext() )
		{
			tileCursor.fwd();

			final int tileLabel = tileCursor.get().getInteger();
			if ( tileLabel == 0 )
				continue;

			final int globalNew = tileLabel + offset;

			canvasRA.setPosition( tileCursor );
			final int canvasLabel = canvasRA.get().getInteger();

			if ( canvasLabel > 0 )
			{
				oldOverlapAreas.merge( canvasLabel, 1, Integer::sum );
				newOverlapAreas.merge( globalNew, 1, Integer::sum );
				pairIntersections.merge( packPair( canvasLabel, globalNew ), 1, Integer::sum );
			}
			else
			{
				canvasRA.get().setInteger( globalNew );
			}
		}

		for ( final Map.Entry< Long, Integer > e : pairIntersections.entrySet() )
		{
			final int oldLabel = unpackHigh( e.getKey() );
			final int newLabel = unpackLow( e.getKey() );
			final int intersection = e.getValue();
			final int union = oldOverlapAreas.get( oldLabel ) + newOverlapAreas.get( newLabel ) - intersection;

			if ( union > 0 && ( double ) intersection / union >= iouThreshold )
				unionFind.union( oldLabel, newLabel );
		}

		offset += tileMax;
	}

	/**
	 * Returns the subregion of {@code tile} whose dimensions match those of
	 * {@code interval}, starting at the minimum coordinates of {@code tile}.
	 * <p>
	 * This is intended for the case where {@code tile} is a reusable buffer
	 * that may be larger than the actual valid tile content. Only the leading
	 * region of the tile, with the same dimensions as {@code interval}, is
	 * considered valid and will be processed.
	 * <p>
	 * This method does not interpret {@code interval} in the coordinate system
	 * of {@code tile}; only {@code interval.dimension(d)} is used.
	 */
	private static < T > RandomAccessibleInterval< T > cropTileToIntervalSize( final RandomAccessibleInterval< T > tile, final Interval interval )
	{
		final int n = tile.numDimensions();
		final long[] min = new long[ n ];
		final long[] max = new long[ n ];

		for ( int d = 0; d < n; d++ )
		{
			min[ d ] = tile.min( d );
			max[ d ] = tile.min( d ) + interval.dimension( d ) - 1;
		}

		return Views.interval( tile, min, max );
	}

	/**
	 * Finalizes the merge by relabeling the canvas to compact labels
	 * {@code 1..N}. Relabeling is restricted to the union bounding box of the
	 * tile intervals that were added.
	 * <p>
	 * This operation is terminal. After it has been called, no more tiles may
	 * be added. Repeated calls are allowed and are no-ops.
	 */
	public void finish()
	{
		if ( finished )
			return;
		if ( !hasTiles )
		{
			finished = true;
			return;
		}

		final Interval fullInterval = new FinalInterval( minFullInterval, maxFullInterval );
		relabelInPlace( canvas, fullInterval, unionFind );
		finished = true;
	}

	/**
	 * Returns whether {@link #finish()} has already been called.
	 *
	 * @return true if the merger has been finalized
	 */
	public boolean isFinished()
	{
		return finished;
	}

	/**
	 * Writes a relabeled snapshot of the current merger state into
	 * {@code target}. Only the interval of {@code target} is written.
	 * <p>
	 * This method does not modify the internal canvas or the merger state. It
	 * can be used to inspect an intermediate finalized labeling before calling
	 * {@link #finish()}. Useful for debugging.
	 *
	 * @param target
	 *            output image receiving a relabeled snapshot
	 * @param <S>
	 *            target pixel type
	 */
	public < S extends IntegerType< S > > void snapshotInto( final RandomAccessibleInterval< S > target )
	{
		if ( isFinished() )
		{
			// Copy without relabeling.
			LoopBuilder.setImages( Views.interval( canvas, target ), target )
					.multiThreaded()
					.forEachPixel( ( i, o ) -> o.setInteger( i.getInteger() ) );
		}
		else
		{
			copyRelabeled( canvas, target, unionFind );
		}
	}

	private static < T extends IntegerType< T >, R extends IntegerType< R > > void validateTile(
			final RandomAccessibleInterval< T > tile,
			final Interval interval,
			final RandomAccessible< R > canvas )
	{
		if ( tile == null )
			throw new IllegalArgumentException( "tile must not be null" );
		if ( interval == null )
			throw new IllegalArgumentException( "interval must not be null" );

		if ( tile.numDimensions() != interval.numDimensions() )
			throw new IllegalArgumentException( "Tile and interval dimension mismatch" );
		if ( tile.numDimensions() != canvas.numDimensions() )
			throw new IllegalArgumentException( "Tile and canvas dimension mismatch" );
		for ( int d = 0; d < tile.numDimensions(); d++ )
		{
			if ( tile.dimension( d ) < interval.dimension( d ) )
				throw new IllegalArgumentException( "Tile is smaller than interval in dimension " + d );
		}
		// If we know that the canvas has bounds, test for it.
		if ( canvas instanceof Interval )
		{
			final Interval canvasInterval = ( Interval ) canvas;
			for ( int d = 0; d < canvas.numDimensions(); d++ )
			{
				if ( interval.min( d ) < canvasInterval.min( d ) || interval.max( d ) > canvasInterval.max( d ) )
					throw new IllegalArgumentException( "Tile interval lies outside the canvas" );
			}
		}
	}

	private static < T > RandomAccessibleInterval< T > translateToInterval(
			final RandomAccessibleInterval< T > tile,
			final Interval interval )
	{
		final int n = tile.numDimensions();
		final long[] shift = new long[ n ];
		for ( int d = 0; d < n; d++ )
			shift[ d ] = interval.min( d ) - tile.min( d );
		return Views.translate( tile, shift );
	}

	private static < T extends IntegerType< T > > int maxLabel(
			final RandomAccessibleInterval< T > tile )
	{
		int max = 0;
		final Cursor< T > c = tile.cursor();
		while ( c.hasNext() )
			max = Math.max( max, c.next().getInteger() );
		return max;
	}

	private static < R extends IntegerType< R > > void relabelInPlace(
			final RandomAccessible< R > canvas,
			final Interval interval,
			final UnionFind unionFind )
	{
		final IntervalView< R > rai = Views.interval( canvas, interval );

		final Set< Integer > roots = new LinkedHashSet<>();
		final Cursor< R > scan = rai.cursor();
		while ( scan.hasNext() )
		{
			final int v = scan.next().getInteger();
			if ( v > 0 )
				roots.add( unionFind.find( v ) );
		}

		final Map< Integer, Integer > rootToCompact = new HashMap<>();
		int id = 1;
		for ( final int root : roots )
			rootToCompact.put( root, id++ );

		final Cursor< R > write = rai.cursor();
		while ( write.hasNext() )
		{
			final R px = write.next();
			final int v = px.getInteger();
			if ( v > 0 )
				px.setInteger( rootToCompact.get( unionFind.find( v ) ) );
		}
	}

	private static < A extends IntegerType< A >, B extends IntegerType< B > > void copyRelabeled(
			final RandomAccessible< A > source,
			final RandomAccessibleInterval< B > target,
			final UnionFind unionFind )
	{
		final Set< Integer > roots = new LinkedHashSet<>();
		final Cursor< A > scan = Views.interval( source, target ).cursor();
		while ( scan.hasNext() )
		{
			final int v = scan.next().getInteger();
			if ( v > 0 )
				roots.add( unionFind.find( v ) );
		}

		final Map< Integer, Integer > rootToCompact = new HashMap<>();
		int id = 1;
		for ( final int root : roots )
			rootToCompact.put( root, id++ );

		final Cursor< B > dst = target.localizingCursor();
		final RandomAccess< A > src = source.randomAccess( target );
		while ( dst.hasNext() )
		{
			dst.fwd();
			src.setPosition( dst );

			final int v = src.get().getInteger();
			if ( v > 0 )
				dst.get().setInteger( rootToCompact.get( unionFind.find( v ) ) );
			else
				dst.get().setZero();
		}
	}

	private static long packPair( int a, int b )
	{
		if ( a > b )
		{
			final int tmp = a;
			a = b;
			b = tmp;
		}
		return ( ( long ) a << 32 ) | ( b & 0xFFFFFFFFL );
	}

	private static int unpackHigh( final long key )
	{
		return ( int ) ( key >>> 32 );
	}

	private static int unpackLow( final long key )
	{
		return ( int ) key;
	}

	private static class UnionFind
	{
		private final Map< Integer, Integer > parent = new HashMap<>();

		private final Map< Integer, Integer > rank = new HashMap<>();

		void add( final int x )
		{
			parent.putIfAbsent( x, x );
			rank.putIfAbsent( x, 0 );
		}

		int find( int x )
		{
			add( x );
			while ( parent.get( x ) != x )
			{
				final int grandparent = parent.get( parent.get( x ) );
				parent.put( x, grandparent );
				x = grandparent;
			}
			return x;
		}

		void union( final int a, final int b )
		{
			int ra = find( a );
			int rb = find( b );
			if ( ra == rb )
				return;

			final int rankA = rank.get( ra );
			final int rankB = rank.get( rb );

			if ( rankA < rankB )
			{
				final int tmp = ra;
				ra = rb;
				rb = tmp;
			}

			parent.put( rb, ra );
			if ( rankA == rankB )
				rank.put( ra, rankA + 1 );
		}
	}
}
