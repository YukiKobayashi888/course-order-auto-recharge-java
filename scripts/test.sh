#!/usr/bin/env sh
set -eu
repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
classes_dir="$repo_dir/target/test-classes"
mkdir -p "$classes_dir"
find "$repo_dir/src/main/java" "$repo_dir/src/test/java" -name '*.java' -print > "$classes_dir/sources.txt"
javac -d "$classes_dir" @"$classes_dir/sources.txt"
java -ea -cp "$classes_dir" dev.lessonshop.orders.OrderFlowTest
