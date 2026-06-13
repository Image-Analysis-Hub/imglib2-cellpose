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

import java.util.function.Consumer;

import org.apposed.appose.Builder.ProgressConsumer;
import org.apposed.appose.TaskEvent;

/**
 * Interface for listeners that want to be notified about the progress and
 * results of Appose tasks. This can be used to update the user interface, log
 * progress, or perform other actions based on the Appose task's lifecycle
 * events.
 */
public interface ApposeTaskListener
{

	/**
	 * Implementation of ApposeTaskListener that writes messages to the standard
	 * output.
	 */
	public static final ApposeTaskListener STD = new StdApposeTaskListener();

	/**
	 * Implementation of ApposeTaskListener that does nothing.
	 */
	public static final ApposeTaskListener VOID = new VoidApposeTaskListener();

	/**
	 * Returns a consumer that will be called with task events related to the
	 * execution of an Appose task, and that writes messages in the outputs
	 * defined in this class.
	 * 
	 * @return a new task event consumer.
	 */
	Consumer< TaskEvent > taskListener();

	/**
	 * Returns a consumer that will be called with output messages related to
	 * the downloading, installation and deployment of an Appose environment,
	 * and that writes messages in the outputs defined in this class.
	 * 
	 * @return a new output message consumer.
	 */
	Consumer< String > outputListener();

	/**
	 * Returns a consumer that will be called with error messages related to the
	 * the downloading, installation and deployment of an Appose environment,
	 * and that writes messages in the outputs defined in this class.
	 * 
	 * @return a new error message consumer.
	 */
	Consumer< String > errorListener();

	/**
	 * Returns a consumer that will be called with progress updates related to
	 * the downloading, installation and deployment of an Appose environment,
	 * and that writes messages in the outputs defined in this class.
	 * 
	 * @return a new progress update consumer.
	 */
	ProgressConsumer progressListener();

	/**
	 * Writes a message to the outputs defined in this class.
	 * 
	 * @param msg
	 *            the message to write.
	 */
	void message( String msg );

	/**
	 * Implementation of ApposeTaskListener that does nothing.
	 */
	static class VoidApposeTaskListener implements ApposeTaskListener
	{

	@Override
        public Consumer< TaskEvent > taskListener()
        {
            return event -> {};
        }

        @Override
        public Consumer< String > outputListener()
        {
            return msg -> {};
        }

        @Override
        public Consumer< String > errorListener()
        {
            return msg -> {};
        }

        @Override
        public ProgressConsumer progressListener()
        {
            return ( t, c, m ) -> {};
        }

		@Override
		public void message( final String msg )
		{}
	}
	
	/**
	 * Implementation of ApposeTaskListener that writes messages to the standard
	 * output.
	 */
	static class StdApposeTaskListener implements ApposeTaskListener
	{
		@Override
		public Consumer< TaskEvent > taskListener()
		{
			return event -> {
				if ( event.message != null && !event.message.isEmpty() )
					message( event.responseType + " - " + event.message );
				if ( event.maximum > 0 )
					progress( ( double ) event.current / event.maximum );
			};
		}

		@Override
		public Consumer< String > outputListener()
		{
			return msg -> message( msg );
		}

		@Override
		public Consumer< String > errorListener()
		{
			return msg -> error( msg );
		}

		private void error( final String msg )
		{
			System.err.println( msg );
		}

		@Override
		public ProgressConsumer progressListener()
		{
			return ( t, c, m ) -> {
				message( t + ": " + String.format( "%.1f%%", ( double ) c / m ) );
			};
		}

		@Override
		public void message( final String msg )
		{
			System.out.println( msg );
		}

		private void progress( final double d )
		{
			System.out.println( String.format( "%.1f%%", 100. * d ) );
		}
	}
}
