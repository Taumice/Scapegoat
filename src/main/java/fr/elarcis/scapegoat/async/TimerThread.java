/*
Copyright (C) 2014 Elarcis.fr <contact+dev@elarcis.fr>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package fr.elarcis.scapegoat.async;

import fr.elarcis.scapegoat.ScapegoatPlugin;

/**
 * Runs every tenth of a second and updates the plugin's timer.
 * @author Elarcis
 */
public class TimerThread extends Thread
{
	protected static ScapegoatPlugin plugin = ScapegoatPlugin.getPlugin(ScapegoatPlugin.class);
	protected int secondsLeft;

	public TimerThread()
	{
		this.setName("Scapegoat Timer Thread");
	}

	public synchronized boolean isDone() { return secondsLeft < 0; }

	public synchronized void setSecondsLeft(int seconds) { this.secondsLeft = seconds; }

	@Override
	public synchronized void run()
	{
		long lastTick = System.currentTimeMillis();

		while (plugin.isRunning())
		{
			long currentTick = System.currentTimeMillis();

			while (currentTick - lastTick > 1000)
			{
				if (!isDone())
					secondsLeft--;

				plugin.timerTick(secondsLeft);
				lastTick += 1000;
			}

			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
}
