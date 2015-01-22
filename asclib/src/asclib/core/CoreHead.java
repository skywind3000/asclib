//=====================================================================
//
// CoreHead.java - Linux kernel list head
//
// NOTE:
// for more information, please see the readme file.
//
//=====================================================================
package asclib.core;


/**
 * Linux kernel list head
 * @author Linwei
 *
 */
public class CoreHead {
	private CoreHead next = null;
	private CoreHead prev = null;
	private Object element = null;	
	
	public CoreHead() {
	}
	
	public CoreHead(Object element) { 
		this.element = element; 
	}
	
	public Object get() { 
		return element; 
	}
	
	public void set(Object data) { 
		this.element = data;
	}
	
	public void init() {
		next = this;
		prev = this;
	}
	
	public void reset() {
		next = null;
		prev = null;
	}
	
	public void dispose() {
		next = null;
		prev = null;
		element = null;
	}
	
	protected void finalize() throws java.lang.Throwable {
		dispose();
		super.finalize();
	}
	
	public CoreHead getNext() { return next; }
	public CoreHead getPrev() { return prev; }
	public boolean empty() { return (next == this || next == null || prev == null); }	
	public boolean valid() { return (next != null && prev != null); }	
	public boolean invalid() { return (next == null || prev == null); }
	
	public void add(CoreHead node) {
		node.prev = this;
		node.next = this.next;
		this.next.prev = node;
		this.next = node;
	}
	
	public void addTail(CoreHead node) {
		node.prev = this.prev;
		node.next = this;
		this.prev.next = node;
		this.prev = node;
	}
	
	public void del() {
		if (next != null && prev != null) {
			this.next.prev = this.prev;
			this.prev.next = this.next;
			this.next = null;
			this.prev = null;
		}
	}
	
	public void delInit() {
		del();
		init();
	}
	
	public void splice(CoreHead node) {
		if (prev == null || next == null) {
			prev = this;
			next = this;
		}
		if (!node.empty()) {
			CoreHead first = node.next;
			CoreHead last = node.prev;
			CoreHead at = this.next;
			first.prev = this;
			this.next = first;
			last.next = at;
			at.prev = last;
			node.init();
		}
	}
}



