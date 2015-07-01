package com.sungevity.analytics.utils

import com.datastax.spark.connector.rdd.ValidRDDType
import com.datastax.spark.connector.rdd.reader.RowReaderFactory
import com.datastax.spark.connector.writer.RowWriterFactory
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

import com.datastax.spark.connector._

object Cassandra {

  implicit class CassandraUtils(keyspaceName: String) {

    def getOrElse[K, V](keys: RDD[K], table: String, primaryKey: (K) => String = (k: K) => k.toString)(f: (K) => Option[V])
                       (implicit newType: ClassTag[(String, V)],
                        rrf: RowReaderFactory[(String, V)],
                        ev: ValidRDDType[(String, V)],
                        currentType: ClassTag[(String, K)],
                        rwf: RowWriterFactory[(String, K)],
                        leftClassTag: ClassTag[K],
                        rightClassTag: ClassTag[V],
                        w: RowWriterFactory[(String, V)]): RDD[(K, V)] = {

      val primaryKeys = keys.map(v => primaryKey(v) -> v)

      val cachedEstimates = primaryKeys.joinWithCassandraTable[(String, V)](keyspaceName, table) map (v => v._1._2 -> v._2._2)

      val newKeys = keys.subtract(cachedEstimates.map(_._1))

      val newEstimates = newKeys.flatMap(v => f(v).map(v -> _))

      newEstimates.map(v => primaryKey(v._1) -> v._2).saveToCassandra(keyspaceName, table)

      newEstimates union cachedEstimates

    }

  }

}
