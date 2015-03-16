//=====================================================================
//
// asclib.core.CoreInt - Linux kernel Timer Interval
//
// NOTE:
// for more information, please see the readme file.
//
//=====================================================================
package asclib.core;

import asclib.core.CoreHead;
import asclib.core.CoreInt;


/**
 * Linux kernel timer implementation 
 *
 */
public class CoreInt {
	private static final int TVR_SHIFT = 8;
	private static final int TVN_SHIFT = 6;
	private static final int TVR_SIZE = (1 << TVR_SHIFT);
	private static final int TVN_SIZE = (1 << TVN_SHIFT);
	private static final int TVR_MASK = (TVR_SIZE - 1);
	private static final int TVN_MASK = (TVN_SIZE - 1);
	
	private static final int TVN_STATE_OK = 0x1981;
	
	/**
	 * TimeNode is the implementation of Linux timer_list 
	 *
	 */
	public static class TimeNode {
		private CoreHead head = new CoreHead(this);
		private CoreInt core = null;
		private long expires = 0;
		private int state = TVN_STATE_OK;
		private Runnable runner = null;
		
		public TimeNode(Runnable runner) {
			this.runner = runner;
		}
		
		public void dispose() {
			if (head != null) {
				head.del();
				head.dispose();
				head = null;
			}
			state = -1;
			core = null;
			runner = null;
		}
		
		public boolean pending() {
			return head.empty() == false;
		}
		
		protected void finalize() throws java.lang.Throwable {
			dispose();
			super.finalize();
		}
		
		public void remove() {
			head.del();
			core = null;
		}
		
		public void invoke() {
			runner.run();
		}
	}
	
	protected static class TimeVector {
		private CoreHead[] vector = null;
		public TimeVector(int size) {
			vector = new CoreHead[size];
			for (int i = 0; i < size; i++) {
				vector[i] = new CoreHead();
				vector[i].init();
			}
		}
		
		public void reset() {
			if (vector != null) {
				for (int i = 0; i < vector.length; i++) {
					CoreHead head = vector[i];
					while (head.empty() == false) {
						CoreHead iter = head.getNext();
						TimeNode node = (TimeNode)iter.get();
						node.remove();
						node.head.reset();
						node = null;
						iter = null;
					}
				}
			}
		}
		
		public void dispose() {
			reset();
			if (vector != null) {
				for (int i = 0; i < vector.length; i++) {
					vector[i].dispose();
					vector[i] = null;
				}
				vector = null;
			}
		}
		
		protected void finalize() throws java.lang.Throwable {
			dispose();
			super.finalize();
		}		
	}
	
	protected TimeVector tv1 = new TimeVector(TVR_SIZE);
	protected TimeVector tv2 = new TimeVector(TVN_SIZE);
	protected TimeVector tv3 = new TimeVector(TVN_SIZE);
	protected TimeVector tv4 = new TimeVector(TVN_SIZE);
	protected TimeVector tv5 = new TimeVector(TVN_SIZE);
	
	protected TimeVector[] tvs = { tv1, tv2, tv3, tv4, tv5 };
	
	protected long timer_jiffies = 0;
	
	/**
	 * Clear All the TimeNode in the CoreTimer
	 */
	public void reset() {
		if (tvs != null) {
			for (int n = 0; n < 5; n++) {
				tvs[n].reset();
			}
		}
	}
	
	public void dispose() {
		if (tvs != null) {
			reset();
			for (int n = 0; n < 5; n++) {
				tvs[n].dispose();
			}
			tvs = null;
		}
		tv1 = null;
		tv2 = null;
		tv3 = null;
		tv4 = null;
		tv5 = null;
	}
	
	protected void finalize() throws java.lang.Throwable {
		dispose();
		super.finalize();
	}
	
	private static final int SHIFT_1 = TVR_SHIFT;
	private static final int SHIFT_2 = TVR_SHIFT + TVN_SHIFT;
	private static final int SHIFT_3 = TVR_SHIFT + TVN_SHIFT * 2;
	private static final int SHIFT_4 = TVR_SHIFT + TVN_SHIFT * 3;
	private static final int SHIFT_5 = TVR_SHIFT + TVN_SHIFT * 4;
	
	private static final long LIMIT_1 = ((long)1) << (TVR_SHIFT);
	private static final long LIMIT_2 = ((long)1) << (TVR_SHIFT + TVN_SHIFT);
	private static final long LIMIT_3 = ((long)1) << (TVR_SHIFT + TVN_SHIFT * 2);
	private static final long LIMIT_4 = ((long)1) << (TVR_SHIFT + TVN_SHIFT * 3);
	private static final long LIMIT_5 = ((long)1) << (TVR_SHIFT + TVN_SHIFT * 4);
	
	private static final int[] SHIFTS = new int[] { 
			SHIFT_1, SHIFT_2, SHIFT_3, SHIFT_4, SHIFT_5
	};
	
	protected boolean internalAddTimer(TimeNode node) {
		long expires = node.expires & 0xffffffff;
		long idx = expires - timer_jiffies;
		CoreHead head = null;
		if (idx >= 0) {
			if (idx < LIMIT_1) {
				int i = (int)(expires & TVR_MASK);
				head = tv1.vector[i];
			}
			else if (idx < LIMIT_2) {
				int i = (int)((expires >>> SHIFT_1) & TVN_MASK);
				head = tv2.vector[i];		
			}
			else if (idx < LIMIT_3) {
				int i = (int)((expires >>> SHIFT_2) & TVN_MASK);
				head = tv3.vector[i];					
			}
			else if (idx < LIMIT_4) {
				int i = (int)((expires >>> SHIFT_3) & TVN_MASK);
				head = tv4.vector[i];
			}
			else if (idx < LIMIT_5) {
				int i = (int)((expires >>> SHIFT_4) & TVN_MASK);
				head = tv5.vector[i];
			}
			else {
				return false;
			}
		}
		else {
			int i = (int)(timer_jiffies & TVR_MASK);
			head = tv1.vector[i];
		}
		head.addTail(node.head);
		node.core = this;
		return true;
	}
	
	/**
	 * Queue a TimeNode into CoreTimer
	 * @param node TimeNode to be queued
	 * @param expires jiffies in the feature when it would be expired.
	 * @return true for success, false for error
	 */
	public boolean add(TimeNode node, long expires) {
		node.expires = expires;
		if (node.head == null || node.state != TVN_STATE_OK) {
			return false;
		}
		if (node.pending()) {
			node.remove();
		}
		return internalAddTimer(node);
	}
	
	/**
	 * Remove TimeNode
	 * @param node TimeNode to be removed
	 * @return
	 */
	public boolean del(TimeNode node) {
		boolean hr = (node.core == this && node.pending());
		node.remove();
		return hr;
	}
	
	private void cascade(TimeVector vec, int index) {
		CoreHead head = new CoreHead();
		head.init();
		head.splice(vec.vector[index]);
		while (!head.empty()) {
			TimeNode node = (TimeNode)head.getNext().get();
			node.remove();
			internalAddTimer(node);
		}
		head.dispose();
		head = null;
	}
	
	private int shift(long jiffies, int index) {
		return (int)((jiffies >>> SHIFTS[index]) & TVN_MASK);
	}
	
	private long update(long jiffies) {
		CoreHead queued = new CoreHead();
		long count = 0;
		queued.init();
		while (timer_jiffies <= jiffies) {
			int index = (int)(timer_jiffies & TVR_MASK);
			queued.init();
			if (index == 0) {
				int i = shift(timer_jiffies, 0);
				cascade(tv2, i);
				if (i == 0) {
					i = shift(timer_jiffies, 1);
					cascade(tv3, i);
					if (i == 0) {
						i = shift(timer_jiffies, 2);
						cascade(tv4, i);
						if (i == 0) {							
							i = shift(timer_jiffies, 3);
							cascade(tv5, i);
						}
					}
				}
			}
			timer_jiffies++;
			queued.splice(tv1.vector[index]);
			while (!queued.empty()) {
				TimeNode node = (TimeNode)queued.getNext().get();
				Runnable runner = node.runner;
				node.remove();
				node.core = null;
				runner.run();
				count++;
			}
		}
		queued.dispose();
		queued = null;
		return count;
	}
	
	/**
	 * CoreTimer Constructor
	 * @param jiffies start time jiffies
	 */
	public CoreInt(long jiffies) {
		timer_jiffies = jiffies;
	}
	
	/**
	 * Run timers
	 * @param jiffies current time jiffies
	 * @return how many timer invoked
	 */
	public long run(long jiffies) {
		return update(jiffies);
	}
	
	/**
	 * testing case
	 * @param argv
	 */
	public static void main(String[] argv) {
		CoreInt core = new CoreInt(0);
		TimeNode node = new TimeNode(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				System.out.println("TimeNode.run()");
			}
		});
		//node.runner.run();
		long DELAY = 20000;
		core.add(node, DELAY);
		for (long jiffies = 0; jiffies < 100000; jiffies++) {
			long hr = core.run(jiffies);
			if (hr > 0) {
				System.out.println("    jiffies=" + jiffies); 
				core.add(node, jiffies + DELAY);
			}
		}
		System.out.println("END");
	}
}


