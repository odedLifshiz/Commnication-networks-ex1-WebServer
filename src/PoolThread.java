
public class PoolThread extends Thread {

	private BlockingQueue m_taskQueue;

	public PoolThread(BlockingQueue taskQueue) {
		m_taskQueue = taskQueue;
	}

	public void run() {

		// run forever and try to get tasks from the task queue
		// if the queue is empty the dequeue method will cause the thread to
		// wait
		while (true) {
			try {
				Runnable runnable = (Runnable) m_taskQueue.dequeue();
				runnable.run();
			} catch (Exception e) {
			}
		}
	}
}