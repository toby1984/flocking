/**
 * Copyright 2012 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.flocking;

/**
 * Simulation interface.
 * 
 * <p>Implementations need to be <b>thread-safe</b>.</p>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public interface ISimulation
{
	/**
	 * Advances the simulation by one step and returns the new world state.
	 * 
	 * @return
	 */
    public World advance();

    /**
     * Sets the simulation parameters to be used.
     * 
     * <p>The simulation parameters will get applied on the next (and all subsequent) calls
     * to {@link #advance()}.</p>
     * 
     * @param parameters
     */
    public void setSimulationParameters(SimulationParameters parameters);
}
