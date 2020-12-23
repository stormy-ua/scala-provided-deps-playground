package com.void.project_c

import com.void.project_a.SerializerA

object Main {

    def main(args: Array[String]): Unit = {
        val schemaStr = """
            {
                "type": "record",
                "name": "TestAvroEntity",
                "namespace": "com.spotify.svalbard.schema",
                "fields" : [
                    {"name": "intField", "type": "int"},
                    {"name": "strField", "type": "string"}
                ]
            }
            """.stripMargin

        println(SerializerA.serialize(schemaStr).map(_.toHexString).toList.mkString)
    }

}