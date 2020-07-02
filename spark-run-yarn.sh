#!/bin/bash
$SPARK_HOME/bin/spark-submit \
	--class com.zishanfu.geosparksim.GeoSparkSim \
	/home/hadoop/GeoSparkSim_ws/geosparksim_removepreprocess/target/GeoSparkSim-1.0-SNAPSHOT-jar-with-dependencies.jar \
	-m /home/hadoop/GeoSparkSim_ws/geosparksim_removepreprocess/geosparksim.config
#--master yarn \
#--deploy-mode cluster \
