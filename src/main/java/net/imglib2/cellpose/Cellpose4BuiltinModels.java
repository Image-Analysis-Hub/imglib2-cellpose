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

public enum Cellpose4BuiltinModels
{
	// General  models
	CPSAM( "cpsam", "Cellpose-SAM model. General model, works well on most data." ),
	CPSAMV2( "cpsam_v2", "Cellpose-SAM model released in June 2026. It handles better low contrast regions" ),
	CPDINO( "cpdino", "CellposeDINO model, based on DINO backbone. For RGB images."),
	CPDINOVITB( "cpdino-vitb", "CellposeDINO smaller model, based on DINO backbone. For RGB images.");
	
	private final String name;

	private final String description;

	Cellpose4BuiltinModels( final String name, final String description )
	{
		this.name = name;
		this.description = description;
	}

	public String modelName()
	{
		return name;
	}

	public String description()
	{
		return description;
	}

	@Override
	public String toString()
	{
		return name;
	}

	/**
	 * Gets a tooltip string combining the model name and description
	 * 
	 * @return Formatted string for tooltip display
	 */
	public String getTooltip()
	{
		return String.format( "%s: %s", name, description );
	}
}
