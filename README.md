# anomaly-detection
Anomaly Detection

##To Complile the Project

1. Get the latest version of [SBT](http://www.scala-sbt.org/)
1. Go to the project root and run
```bash
sbt clean compile
```

##To Run It

To run the tool use _run.sh_ script that can be found at the root directory of the project:

```bash
$ ./run.sh
local <number_of_days> <number_of_neighbours> <output_file>|remote <spark-master-url> <number_of_days> <number_of_neighbours> <output_file>
```

There are 2 modes for running the program: _remote_ and _local_. In _remote_ mode you'll need a [Spark](https://spark.apache.org/) cluster. For _local_ mode all you need is _SBT_:

```bash
$ ./run.sh local 10 50 /tmp/n-day final out.csv
...
15/06/23 16:05:00 INFO MemoryStore: MemoryStore cleared
15/06/23 16:05:00 INFO BlockManager: BlockManager stopped
15/06/23 16:05:00 INFO BlockManagerMaster: BlockManagerMaster stopped
15/06/23 16:05:00 INFO OutputCommitCoordinator$OutputCommitCoordinatorEndpoint: OutputCommitCoordinator stopped!
15/06/23 16:05:00 INFO SparkContext: Successfully stopped SparkContext
15/06/23 16:05:00 INFO Utils: Shutdown hook called
15/06/23 16:05:00 INFO Utils: Deleting directory /private/var/folders/b8/yf4pgvkd5z76gj36n7n1x4z80000gp/T/spark-23da851b-fe61-4381-b67d-238f8ddaadbe
15/06/23 16:05:00 INFO RemoteActorRefProvider$RemotingTerminator: Shutting down remote daemon.
15/06/23 16:05:00 INFO RemoteActorRefProvider$RemotingTerminator: Remote daemon shut down; proceeding with flushing remote transports.
```


