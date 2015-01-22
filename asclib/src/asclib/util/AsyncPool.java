package asclib.util;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.List;
import java.util.ListIterator;
import java.lang.Runnable;
import java.io.*;

/**
 * Thread Pool for #AsyncTask
 *
 */
public class AsyncPool {
	
	private LinkedBlockingQueue<AsyncRunner> mQueueBack = new LinkedBlockingQueue<AsyncRunner>();
	
	private ExecutorService mExecutor = null;
	private int mMaxUpdateTask = -1;
	private boolean mShutdown = false;
	private boolean mNoUpdate = false;
	private AtomicInteger mTaskCount = new AtomicInteger(0);
	private AtomicInteger mTaskRunning = new AtomicInteger(0);
	private PrintStream stderr = null;	
	
	private class AsyncRunner implements Runnable {
		public AsyncPool mPool = null;		
		public AsyncTask mTask = null;
		public Object mResult = null;
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			mTaskRunning.getAndIncrement();
			try {
				mResult = mTask.doInBackground();
			} catch (java.lang.Throwable e) {
				if (mPool.stderr != null) {
					e.printStackTrace(mPool.stderr);
				}	else {
					e.printStackTrace();
				}
			}
			try {
				mPool.mQueueBack.put(this);
			} catch (InterruptedException e) {
				if (mPool.stderr != null) {
					e.printStackTrace(mPool.stderr);
				}	else {
					e.printStackTrace();
				}
			}
			mTaskRunning.getAndDecrement();
		}
	}

	/**
	 * Create AsyncPool with given threads 
	 * @param nThreads how many threads
	 */
	public AsyncPool(int nThreads) {
		mExecutor = Executors.newFixedThreadPool(nThreads);
	}
	
	/**
	 * commit an AsyncTask to execute
	 * @param task given task
	 * @return true for success, false for failed
	 */
	public boolean execute(AsyncTask task) {
		if (task == null) return false;
		if (mShutdown) return false;
		AsyncRunner runner = new AsyncRunner();
		runner.mPool = this;
		runner.mTask = task;
		runner.mResult = null;
		task.onPreExecute();
		mTaskCount.getAndIncrement();		
		mExecutor.execute(runner);
		return true;
	}
	
	/**
	 * invoke it from main thread to call onPostExecute after running in background 
	 */
	public void update() {
		if (mNoUpdate) return;
		for (int count = 0; ; count++) {
			if (count > mMaxUpdateTask && mMaxUpdateTask > 0) break;
			AsyncRunner runner = mQueueBack.poll();
			if (runner == null) break;
			mTaskCount.getAndDecrement();
			runner.mTask.onPostExecute(runner.mResult);
			runner.mPool = null;
			runner.mResult = null;
			runner.mTask = null;
		}
	}
	
	/**
	 * set how many task will be proceeded in one update
	 * @param value -1 stands for no limit
	 */
	public void setMaxUpdateTask(int value) {
		mMaxUpdateTask = value;
	}
	
	/**
	 * no new task can be execute
	 */
	public void shutdown() {
		mShutdown = true;		
	}
	
	/**
	 * shutdown and wait until all tasks have been proceeded
	 */
	public void join() {
		mShutdown = true;
		mExecutor.shutdown();
		while (mTaskCount.get() > 0) {
			update();
			try {
				Thread.sleep(10);
			} catch (java.lang.Throwable e) {
			}
		}
	}
	
	/**
	 * stop immediately
	 */
	public void stop() {
		mShutdown = true;
		mNoUpdate = true;
		mExecutor.shutdown();
		List<Runnable> runners = mExecutor.shutdownNow();
		ListIterator<Runnable> it = runners.listIterator();
		while (it.hasNext()) {
			AsyncRunner runner = (AsyncRunner)it.next();
			runner.mPool = null;
			runner.mTask = null;
			runner.mResult = null;			
		}
		runners.clear();
	}
	
	/**
	 * set stderr to print stack trace
	 * @param stderr
	 */
	public void setErr(PrintStream stderr) {
		this.stderr = stderr;
	}
	
	/**
	 * get how many tasks is running or will be running
	 * @return task count
	 */
	public int taskCount() {
		return mTaskCount.get();
	}
	
	/**
	 * get how many tasks is running 
	 * @return task count
	 */
	public int taskRunning() {
		return mTaskRunning.get();
	}
	
	/**
	 * testing case
	 * @param args
	 */
	public static void main(String[] args) {
		class task extends AsyncTask {
			public void onPreExecute() {
				System.out.println("onPreExecute(): " + Thread.currentThread().getName());
			}
			public Object doInBackground() {
				for (int i = 0; i < 5; i++) {
					try {
						Thread.sleep(1000);
					}	
					catch (java.lang.Throwable e) {
					}
					System.out.println("doInBackground(" + i + "): " + Thread.currentThread().getName());					
				}
				//throw new InterruptedException();
				return Thread.currentThread().getName();
			}
			public void onPostExecute(Object result) {
				System.out.println("onPostExecute(" + result + "): " + Thread.currentThread().getName());
			}
		}
		AsyncPool pool = new AsyncPool(10);
		task t = new task();
		pool.execute(t);
		pool.execute(t);
		pool.execute(t);
		for (int i = 0; i < 30; i++) {
			pool.update();
			try {
				Thread.sleep(100);
			}
			catch (java.lang.Throwable e) {
				
			}
		}
		System.out.println("JOIN START");		
		pool.join();		
		System.out.println("JOIN END");
	}
}


