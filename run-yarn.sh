#!/bin/bash
$SPARK_HOME/bin/spark-submit \
	--master yarn \
	--deploy-mode client \
	--class com.zishanfu.geosparksim.GeoSparkSim \
	target/GeoSparkSim-1.0-SNAPSHOT-jar-with-dependencies.jar \
	-d \
	-m run-yarn.config
