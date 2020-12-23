# Provided Dependencies in Scala

> This repository is intended to be a playground for experimenting with Scala provided dependencies. As we all know, experimentation is the best way to discover and learn something new.


## Project Structure

The repository contains three Scala projects with extremely sophisitcated names: `project-a`, `project-b`, `projects-c`. The idea is to keep these projects as simple as possible and limit number of dependencies.

`project-a` depends on `com.apache.avro` dependency version `1.8.1`:

```scala
libraryDependencies ++= Seq(
        "org.apache.avro" % "avro" % "1.8.1" % "provided"
    )
```

`project-b` depends on the same artifact but of version `1.10.1`:

```scala
    libraryDependencies ++= Seq(
        "org.apache.avro" % "avro" % "1.10.1" % "provided"
    )
```

Both `project-a` and `project-b` declare the dependency as [provided](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html). The main idea of using "provided" dependency scope is to exlcude the dependency from being on the class path by default. As a result, it isn't included into a fat jar artifact.

`project-a` has very simple code which actually uses Apache Avro dependency. It serializes a `GenericRecord` to an array of bytes:

```scala
    def serialize(schemaStr: String): Array[Byte] = {
        val schema = new Schema.Parser().parse(schemaStr)
        val record = new GenericData.Record(schema)

        record.put("intField", 12)
        record.put("strField", "test")

        write(Seq(record), schema)
    }
```

`project-c` doesn't explicitly declare the Apache Avro artifact as a dependency, but it depends on both `project-a` and `project-b`:

```scala
    libraryDependencies ++= Seq(
        "com.void" %% "project-a" % "1.0.0",
        "com.void" %% "project-b" % "1.0.0"
    )
```

## Lesson 1: Project A

Let's get back to `project-a` and see what is going on there. All three projects use [sbt dependency graph](https://github.com/sbt/sbt-dependency-graph) plugin to easily check for dependencies.

With the Avro dependency declared as provided `dependencyTree` is indicating that there is no dependency on the artifact:

```bash
sbt:project-a> dependencyTree
[info] com.void:project-a_2.12:1.0.0 [S]
```

But the project compiles:

```bash
sbt:project-a> compile
[info] compiling 1 Scala source to scala-provided-deps-playground/project-a/project-a/target/scala-2.12/classes ...
[success] Total time: 4 s, completed Dec 23, 2020 4:27:24 PM
```

but on attempt to execute the code it fails:

```bash
sbt:project-a> runMain com.void.project_a.SerializerA
[info] running com.void.project_a.SerializerA 
[error] (run-main-0) java.lang.NoClassDefFoundError: org/apache/avro/Schema
[error] java.lang.NoClassDefFoundError: org/apache/avro/Schema
[error]         at java.lang.Class.getDeclaredMethods0(Native Method)
[error]         at java.lang.Class.privateGetDeclaredMethods(Class.java:2701)
[error]         at java.lang.Class.privateGetMethodRecursive(Class.java:3048)
[error]         at java.lang.Class.getMethod0(Class.java:3018)
[error]         at java.lang.Class.getMethod(Class.java:1784)
[error] Caused by: java.lang.ClassNotFoundException: org.apache.avro.Schema
```

It fails because JVM can't find the Avro artifact on the class path and fails.

Obviously, removing `provided` fixes the issue. So the first takeaway is that dependency with provided scope lets a project depending on it to compile, but fails on execution.

## Lesson 2: Project C. Show me your deps!

`project-c` depends on both `project-a` and `project-b`. Let's use `dependencyTree` again to check what transitive dependencies we have for `project-c`.

```bash
sbt:project-c> dependencyTree
[info] com.void:project-c_2.12:0.1.0-SNAPSHOT [S]
[info]   +-com.void:project-a_2.12:1.0.0 [S]
[info]   +-com.void:project-b_2.12:1.0.0 [S]
```

Well, since both `project-a` and `project-b` reference the Avro artifact as provided it isn't getting as a transitive dependency to the `project-c`.

`project-c` has the following code executing the "serializer" from `project-a`:

```scala
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
```

`project-c` compiles, but again fails to execute due to missing dependencies:

```bash
sbt:project-c> runMain com.void.project_c.Main
[info] running com.void.project_c.Main 
[error] (run-main-0) java.lang.NoClassDefFoundError: org/apache/avro/io/DatumWriter
[error] java.lang.NoClassDefFoundError: org/apache/avro/io/DatumWriter
[error]         at com.void.project_c.Main$.main(Main.scala:20)
[error]         at com.void.project_c.Main.main(Main.scala)
[error]         at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
[error]         at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
[error]         at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
[error]         at java.lang.reflect.Method.invoke(Method.java:498)
[error] Caused by: java.lang.ClassNotFoundException: org.apache.avro.io.DatumWriter
[error]         at java.net.URLClassLoader.findClass(URLClassLoader.java:382)
```

## Lesson 3: Project C. Choosing a dependency.

To fix `project-c` we have to add a non-provided dependency on the Avro artifact to the `project-c` itself. As you might recall, `project-a` depends on version `1.8.1` and `project-b` on version `1.10.1` of the Avro artifact. Let's see if can pick whichever we want and whether it will work. Let's start from `1.10.1`:

```scala
    libraryDependencies ++= Seq(
        "com.void" %% "project-a" % "1.0.0",
        "com.void" %% "project-b" % "1.0.0",
        //"org.apache.avro" % "avro" % "1.8.1",
        "org.apache.avro" % "avro" % "1.10.1"
    )
```

After sbt for `project-c` is reloaded the `dependencyTree` is reporting that the `1.10.1` dep is available. Now is time to run the project.

```bash
sbt:project-c> runMain com.void.project_c.Main
[info] running com.void.project_c.Main 
4f626a12166176726f2e736368656d61ffffffc627b2274797065223a227265636f7264222c226e616d65223a22546573744176726f456e74697479222c226e616d657370616365223a22636f6d2e73706f746966792e7376616c626172642e736368656d61222c226669656c6473223a5b7b226e616d65223a22696e744669656c64222c2274797065223a22696e74227d2c7b226e616d65223a227374724669656c64222c2274797065223a22737472696e67227d5d7d0456974ffffffb3ffffffb5ffffffd03e5fffffffbc5959ffffffc753fffffff27affffffa62c18874657374456974ffffffb3ffffffb5ffffffd03e5fffffffbc5959ffffffc753fffffff27affffffa6
```

Works now with `1.10.1` version of the Avro.

Switching to `1.8.1` works as well:

```scala
    libraryDependencies ++= Seq(
        "com.void" %% "project-a" % "1.0.0",
        "com.void" %% "project-b" % "1.0.0",
        "org.apache.avro" % "avro" % "1.8.1",
    )
```

```bash
sbt:project-c> dependencyTree
[info] com.void:project-c_2.12:0.1.0-SNAPSHOT [S]
[info]   +-com.void:project-a_2.12:1.0.0 [S]
[info]   +-com.void:project-b_2.12:1.0.0 [S]
[info]   +-org.apache.avro:avro:1.8.1
[info]     +-com.thoughtworks.paranamer:paranamer:2.7
[info]     +-org.apache.commons:commons-compress:1.8.1
[info]     +-org.codehaus.jackson:jackson-core-asl:1.9.13
[info]     +-org.codehaus.jackson:jackson-mapper-asl:1.9.13
[info]     | +-org.codehaus.jackson:jackson-core-asl:1.9.13
[info]     | 
[info]     +-org.slf4j:slf4j-api:1.7.7
[info]     +-org.tukaani:xz:1.5
[info]     +-org.xerial.snappy:snappy-java:1.1.1.3
```

```bash
sbt:project-c> runMain com.void.project_c.Main
[info] running com.void.project_c.Main 
4f626a12166176726f2e736368656d61ffffffc627b2274797065223a227265636f7264222c226e616d65223a22546573744176726f456e74697479222c226e616d657370616365223a22636f6d2e73706f746966792e7376616c626172642e736368656d61222c226669656c6473223a5b7b226e616d65223a22696e744669656c64222c2274797065223a22696e74227d2c7b226e616d65223a227374724669656c64222c2274797065223a22737472696e67227d5d7d0fffffffa495e7effffffd0ffffffcc1ffffffe4ffffff89ffffffe92efffffff7ffffff8836f3d2c18874657374fffffffa495e7effffffd0ffffffcc1ffffffe4ffffff89ffffffe92efffffff7ffffff8836f3d
```

So the final takeaway is that a project depending on artifacts with provided transitive dependencies may put a dependency of its choice on the class pass to execute. Still, there might be some incompatibilities among different versions, so not always the approach works, but this is out of the scope for now.
