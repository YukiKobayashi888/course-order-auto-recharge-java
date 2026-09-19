#!/usr/bin/env sh
set -eu
if [ "$#" -ne 1 ]; then
  echo "Usage: INFRAI_API_KEY=your-key scripts/run-example.sh learner-email" >&2
  exit 2
fi
repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
classes_dir="$repo_dir/target/classes"
mkdir -p "$classes_dir"
find "$repo_dir/src/main/java" -name '*.java' -print > "$classes_dir/sources.txt"
javac -d "$classes_dir" @"$classes_dir/sources.txt"
java -cp "$classes_dir" dev.lessonshop.orders.CourseOrderExample "$1"
