import java.util.LinkedList;

public class ThreadPool {

	private BlockingQueue m_taskQueue;
	LinkedList<PoolThread> m_threads;

	public ThreadPool(int maxNoOfTasks) {
		m_threads = new LinkedList<PoolThread>();
		
		// create a blocking queue
		m_taskQueue = new BlockingQueue(maxNoOfTasks);

		// create max number of threads.
		// the threads will all have the same taskQueue which
		// is basically a synchronized queue
		for (int i = 0; i < maxNoOfTasks; i++) {
			m_threads.add(new PoolThread(m_taskQueue));
		}

		// start all the threads. they will initially sleep since
		// there are no tasks in the task queue
		for (PoolThread thread : m_threads) {
			thread.start();
		}

	}

	public synchronized void execute(Runnable task) {
		try {
			m_taskQueue.enqueue(task);
		} catch (Exception e) {
		}
	}
}