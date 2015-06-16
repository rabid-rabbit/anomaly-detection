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
local|remote <number_of_days> <number_of_neighbours>
```

There are 2 modes for running the program: _remote_ and _local_. In _remote_ mode you'll need a [Spark](https://spark.apache.org/) cluster. For _local_ mode all you need is _SBT_:

```bash
$ ./run.sh local 10 50
...
Map(a1IU00000012DAcMAM -> Vector(68, 68, 68, 68, 68, 68, 68, 68, 68, 68, 68))
Map(a1IU000000126NDMAY -> Vector(58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32))
Map(a1IU000000121kuMAA -> Vector(79, 79, 79, 79, 79, 79, 79, 79, 79, 79, 79))
Map(a1IU000000121jYMAQ -> Vector(39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44, 44))
Map(a1IU000000121jiMAA -> Vector(57, 57, 57, 57, 57, 57, 57, 57, 57, 57, 57))
Map(a1IU00000012AWVMA2 -> Vector(27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40))
Map(a1IU0000001280TMAQ -> Vector(34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 34, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37))
Map(a1IU000000125b4MAA -> Vector(85, 85, 85, 85, 85, 85, 85, 85, 85, 85, 85))
Map(a1IU00000050WxXMAU -> Vector(48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48))
Map(a1IU000000128yxMAA -> Vector(74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 74, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16))
```


