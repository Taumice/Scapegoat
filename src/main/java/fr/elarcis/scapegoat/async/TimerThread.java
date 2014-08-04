package fr.elarcis.scapegoat.async;

import fr.elarcis.scapegoat.ScapegoatPlugin;


public class TimerThread extends Thread {
	ScapegoatPlugin plugin;
	int secondsLeft;
	
	public TimerThread(ScapegoatPlugin plugin) {
		this.plugin = plugin;
		this.setName("Scapegoat Timer Thread");
	}

	public synchronized void setSecondsLeft(int seconds) {
		this.secondsLeft = seconds;
	}
	
	public synchronized void addSecondsLeft(int seconds) {
		this.secondsLeft += seconds;
	}
	
	public synchronized boolean isDone() {
		return secondsLeft < 0;
	}
	
	@Override
	public synchronized void run() {
		long lastTick = System.currentTimeMillis();
		
		while(plugin.isRunning()) {
			long currentTick = System.currentTimeMillis();
			
			while (currentTick - lastTick > 1000) {
				if (!isDone()) {
					secondsLeft--;
				}
				
				plugin.timerTick(secondsLeft);
				lastTick += 1000;
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
