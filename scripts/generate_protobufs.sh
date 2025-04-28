#!/bin/bash

# Define directories
SRC="./protobufs"        # Directory where .proto files are located
OUT="./protobufoutput"              # Temp output directory for compiled .proto files
KOTLIN_DIR="./app/src/main/java/com/example/meshtasticapp/protobufs"  # Directory to store Kotlin compiled files

PACKAGE="com/example/meshtasticapp/protobufs"  # Package name for Kotlin files

# Create necessary directories if they do not exist
if [ ! -d "$OUT" ]; then
    mkdir -p "$OUT"
fi
if [ ! -d "$KOTLIN_DIR" ]; then
    mkdir -p "$KOTLIN_DIR"
fi

# Compile .proto files for Kotlin
echo "Source directory: $SRC"
for protoFile in "$SRC"/*.proto; do
    if [ -f "$protoFile" ]; then
        echo "Compiling $(basename "$protoFile") for Kotlin..."
        protoc --proto_path="$SRC" --java_out="$OUT" --kotlin_out="$OUT" "$protoFile"
    fi
done

# Copy compiled files to Kotlin directory
echo "Copying compiled Kotlin files..."
cp -R "$OUT/$PACKAGE/"* "$KOTLIN_DIR"

# Optionally remove temporary output directory
rm -rf "$OUT"

echo "Done!"
