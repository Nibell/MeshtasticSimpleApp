# Define directories
$SRC = ".\protobufs"        # Directory where .proto files are located
$OUT = ".\protobufoutput"    # Temp output directory for compiled .proto files
$KOTLIN_DIR = ".\app\src\main\java\se\neriodefense\swetak\protobufs"     # Directory to store Kotlin compiled files

$PACKAGE = "se\neriodefense\swetak\protobufs"  # Package name for Kotlin files

# Create necessary directories if they do not exist
if (-Not (Test-Path $OUT)) {
    New-Item -ItemType Directory -Force -Path $OUT
}
if (-Not (Test-Path $KOTLIN_DIR)) {
    New-Item -ItemType Directory -Force -Path $KOTLIN_DIR
}

$fullPath = Get-Item -Path $SRC

# Compile .proto files for Kotlin
Write-Host "Fullpath=$fullPath"
Get-ChildItem -Path $SRC -Filter *.proto | ForEach-Object {
    Write-Host $_.Name
    $protoFile = $_.Name
    Write-Host "Compiling $protoFile for Kotlin..."
    protoc --proto_path=$SRC --java_out=$OUT --kotlin_out=$OUT "$SRC\$protoFile"
}

# Copy compiled files to Kotlin directory
Write-Host "Copying Go compiled files..."
Copy-Item -Path "$OUT\$PACKAGE\*" -Destination $KOTLIN_DIR -Force

# Remove temporary output directory
Remove-Item -Path $OUT -Force -Recurse

Write-Host "Done!"