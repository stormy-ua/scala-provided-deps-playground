package com.void.project_a

import org.apache.avro.generic.GenericRecord
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData

object SerializerA {

    def serialize(schemaStr: String): Array[Byte] = {
        val schema = new Schema.Parser().parse(schemaStr)
        val record = new GenericData.Record(schema)

        record.put("intField", 12)
        record.put("strField", "test")

        write(Seq(record), schema)
    }

     def write(records: Seq[org.apache.avro.generic.GenericData.Record],
            schema: org.apache.avro.Schema): Array[Byte] = {
    import java.io.ByteArrayOutputStream
    import org.apache.avro.file.DataFileWriter
    import org.apache.avro.generic.{GenericDatumWriter, GenericRecord}

    val outputStream = new ByteArrayOutputStream()
    val datumWriter = new GenericDatumWriter[GenericRecord](schema)
    val dataFileWriter = new DataFileWriter[GenericRecord](datumWriter)
    dataFileWriter.create(schema, outputStream)

    for (record <- records)
      dataFileWriter.append(record)

    dataFileWriter.flush()
    dataFileWriter.close()

    outputStream.toByteArray
  }

  def read(bytes: Array[Byte],
           schema: org.apache.avro.Schema): List[org.apache.avro.generic.GenericRecord] = {
    import org.apache.avro.file.{DataFileReader, SeekableByteArrayInput}
    import org.apache.avro.generic.{GenericDatumReader, GenericRecord}

    val datumReader = new GenericDatumReader[GenericRecord](schema)
    val inputStream = new SeekableByteArrayInput(bytes)
    val dataFileReader = new DataFileReader[GenericRecord](inputStream, datumReader)

    import scala.collection.JavaConverters._
    val list = dataFileReader.iterator().asScala.toList

    dataFileReader.close()

    list
  }

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

    println(serialize(schemaStr).map(_.toHexString).toList.mkString)

  }

}