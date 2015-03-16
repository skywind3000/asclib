//=====================================================================
//
// asclib.core.CoreTimer - Linux Timer
//
// NOTE:
// for more information, please see the readme file.
//
//=====================================================================
package asclib.core;

import asclib.core.CoreInt;

public class CoreTimer {	
	public static abstract class TimerTask {
		private CoreInt.TimeNode __node__ = null;
		private CoreTimer __timer__ = null;
		private long __slap__ = 0;
		private int __period__ = 0;
		private int __repeat__ = 0;
		private int __running__ = 0;
		
		public TimerTask() {
			Runnable runner = new Runnable() {
				@Override
				public void run() {
					CoreTimer timer = TimerTask.this.__timer__; 
					if (timer != null) {
						timer.update(TimerTask.this);
					}
				}
			};
			__node__ = new CoreInt.TimeNode(runner);
		}
		
		/**
		 * TO IMPLEMENTE THIS METHOD
		 */
		public abstract void run();
		
		/**
		 * stop task
		 */
		public final void stop() {
			if (__node__ != null) {
				__node__.remove();
			}
			__running__ = 0;
			__timer__ = null;
		}
		
		public void dispose() {
			if (__node__ != null) {
				__node__.dispose();
			}
			__node__ = null;
			__timer__ = null;
		}
		
		protected void finalize() throws java.lang.Throwable {
			TimerTask.this.dispose();
			super.finalize();
		}
	}
	
	private long interval = 10;
	private long current = 0;
	private long millisec = 0;
	private long jiffies = 0;
	
	private CoreInt core = null;
	
	public long TIME_SKIP_LIMIT = 300000;  // 300 seconds to skip
	
	public CoreTimer(long millisec, int interval) {
		this.interval = interval;
		this.millisec = millisec;
		this.current = 0;
		this.jiffies = 0;
		core = new CoreInt(0);
	}
	
	public void dispose() {
		core.dispose();
		core = null;
	}
	
	protected void finalize() throws java.lang.Throwable {
		dispose();
		super.finalize();
	}
	
	/**
	 * Run timmer
	 * @param millisec
	 */
	public void run(long millisec) {
		long limit = TIME_SKIP_LIMIT + interval * 4;
		long diff = millisec - this.millisec;
		if (diff >= limit) this.millisec = millisec;
		else if (diff <= -limit) this.millisec = millisec;
		while (millisec >= this.millisec) {
			core.run(this.jiffies);
			this.jiffies++;			
			this.current += interval;
			this.millisec += interval;
			if (interval == 0) break;
		}
	}
	
	/**
	 * schedule task
	 * @param task - task to schedule
	 * @param period - period in millisec
	 * @param repeat - how many times to invoke, <= 0 for infinite
	 * @return
	 */
	public boolean start(TimerTask task, int period, int repeat) {
		if (task.__node__ == null) return false;
		task.__period__ = period;
		task.__repeat__ = repeat;
		task.__slap__ = current + period;
		task.__timer__ = this;
		long expires = (period + interval - 1) / interval;
		if (expires > 0x70000000) expires = 0x70000000;
		core.add(task.__node__, jiffies + expires);
		task.__running__ = 0;
		return true;
	}
	
	/**
	 * schedule task and repeat forever
	 * @param task - task to schedule
	 * @param period - period in millisec
	 * @return
	 */
	public boolean start(TimerTask task, int period) {
		return start(task, period, -1);
	}
	
	/**
	 * stop task
	 * @param task task to stop
	 */
	public void stop(TimerTask task) {
		task.stop();
	}
	
	/**
	 * update runner
	 * @param task
	 */
	private void update(TimerTask task) {
		boolean stop = false;		
		int count = 0;
		while (current >= task.__slap__) {
			count++;
			task.__slap__ += task.__period__;
			if (task.__repeat__ == 1) {
				stop = true;
				break;
			}
			else if (task.__repeat__ > 1) {
				task.__repeat__--;
			}
		}
		if (stop == false) {
			long expires = (task.__period__ + interval - 1) / interval;
			if (expires > 0x70000000) expires = 0x70000000;
			core.add(task.__node__, jiffies + expires);
		}	else {
			task.stop();
		}
		task.__running__ = 1;
		for (int i = 0; i < count; i++) {
			if (task.__node__ != null && task.__running__ == 1) {
				task.run();
			}
		}
		task.__running__ = 0;
	}
	
	public long now() {
		return current;
	}
	
	/**
	 * invoke a runnable object in a given time
	 * @param runnable calling object
	 * @param millisec after how many millisec to invoke
	 * @return new TimerTask
	 */
	public TimerTask delayInvoke(Runnable runnable, int millisec) {
		final Runnable r = runnable;
		TimerTask task = new TimerTask() {
			private final Runnable runner = r;
			@Override
			public void run() { 
				runner.run();
			}
		};
		task.__node__ = new CoreInt.TimeNode(runnable);
		start(task, millisec, 1);
		return task;
	}
	
	/**
	 * Create a TimerTask from given Runnable
	 * @param runnable Runnable to create a TimerTask
	 * @return TimerTask created
	 */
	public TimerTask create(Runnable runnable) {
		final Runnable r = runnable;
		TimerTask task = new TimerTask() {
			private final Runnable runner = r;
			@Override
			public void run() { 
				runner.run();
			}
		};
		task.__node__ = new CoreInt.TimeNode(runnable);
		return task;
	}
	
	/**
	 * invoke a runnable object repeatly
	 * @param runnable
	 * @param period
	 * @return new TimerTask
	 */
	public TimerTask repeatInvoke(Runnable runnable, int period) {
		final Runnable r = runnable;
		TimerTask task = new TimerTask() {
			private final Runnable runner = r;
			@Override
			public void run() { 
				runner.run();
			}
		};
		task.__node__ = new CoreInt.TimeNode(runnable);
		start(task, period, -1);
		return task;
	}
	
	/**
	 * testing case
	 * @param argv parameters
	 */
	public static void main(String[] argv) {
		final CoreTimer timer = new CoreTimer(0, 10);
		
		CoreTimer.TimerTask task1 = new CoreTimer.TimerTask() {
			private final CoreTimer t = timer;
			private int count = 0;
			@Override
			public void run() {
				System.out.printf("%d: task(%d) %d\n", t.now(), 1, count);
				count++;
			}
		};
		
		timer.start(task1, 10000, 5);
		
		for (long current = 0; current < 0x100000; current++) {
			timer.run(current);
		}
		
		System.out.println("DONE");
	}
}


