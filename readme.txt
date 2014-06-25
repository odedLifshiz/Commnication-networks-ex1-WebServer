Classes:

WebServer- Reads the config file, creates a thread pool and listens for connections on the specified port.
Creates httpRequest to handle each connection.

ConfigFileParser- Parses the variables in the config file.

BadConfigFileException-An exception thrown when the config file has errors.

HttpRequest- Reads and handles each connection. 

PoolThread- A single thread in the thread pool. Runs endlessly trying to get items from the synchronized queue and
handles each Runnable item.

ThreadPool- A thread pool initialized with the number of threads from the config file.

BlockingQueue- A synchronized queue of Runnable objects. the Web server puts items in it(connections) and 
PoolThreads extract items from it and create HttpRequests to handle each connection.


