# Build CoordsToggle with Java 21

$java21Path = "C:\Program Files\Java\jdk-21*"

# Find Java 21
$java21 = Get-ChildItem $java21Path -ErrorAction SilentlyContinue | Select-Object -First 1

if ($java21) {
    $env:JAVA_HOME = $java21.FullName
    $env:PATH = "$java21FullName\bin;$env:PATH"
    Write-Host "Using Java 21: $java21FullName"
} else {
    Write-Host "Java 21 not found at: $java21Path"
    Write-Host "Please update the path in this script"
    exit 1
}

# Run Maven build
mvn clean package

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBuild successful!"
    Write-Host "Output: target\CoordsToggle-1.1.0.jar"
} else {
    Write-Host "`nBuild failed!"
}