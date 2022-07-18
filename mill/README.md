# Build scala with Mill

This is a first “Hello, world” example of the Mill build tool that also
shows how to use ScalaTest unit tests with Mill.

For more details, visit [alvinalexander.com/scala/mill-build-tool/intro/](https://alvinalexander.com/scala/mill-build-tool/intro/)

To install Mill, `brew install mill`.

To generate the dependency tree:
```
mill --no-server --import ivy:io.chris-kipp::mill-github-dependency-graph::0.1.1 io.kipp.mill.github.dependency.graph.Graph/generate

jq '.value.HelloWorld.resolved | keys[]' out/io/kipp/mill/github/dependency/graph/Graph/generate.json | sort | uniq -u > direct.txt

jq '.value.HelloWorld.resolved[].dependencies[]' out/io/kipp/mill/github/dependency/graph/Graph/generate.json | sort | uniq -u > indirect.txt
```