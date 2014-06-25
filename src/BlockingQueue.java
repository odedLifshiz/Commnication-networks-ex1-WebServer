import java.util.LinkedList;

public class BlockingQueue {

	private LinkedList<Runnable> m_queue;
	private int m_limit;

	public BlockingQueue(int limit) {
		m_queue = new LinkedList<Runnable>();
		m_limit = limit;
	}

	public synchronized void enqueue(Runnable item) throws Exception {
		while (m_queue.size() == m_limit) {
			wait();
		}
		if (this.m_queue.size() == 0) {
			notifyAll();
		}
		m_queue.add((Runnable) item);
	}

	public synchronized Runnable dequeue() throws Exception {
		while (m_queue.size() == 0) {
			wait();
		}
		if (m_queue.size() == m_limit) {
			notifyAll();
		}

		return m_queue.remove(0);
	}

}