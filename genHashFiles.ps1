# Generate–HashesAndSignatures.ps1
param(
    [string]$BuildDir = "$PSScriptRoot\target\build"
)

# Verify the build directory exists
if (-not (Test-Path $BuildDir -PathType Container)) {
    Write-Error "Build directory not found: $BuildDir"
    exit 1
}

# Ensure GPG is available
if (-not (Get-Command gpg.exe -ErrorAction SilentlyContinue)) {
    Write-Error "gpg.exe not found in PATH. Please install GPG and retry."
    exit 1
}

# Process each .jar and .pom
Get-ChildItem -Path $BuildDir -File -Include *.jar, *.pom -Recurse |
  Sort-Object FullName |
  ForEach-Object {
    $file = $_.FullName

    # 1. Generate MD5 and SHA1 digests
    foreach ($alg in 'MD5','SHA1') {
      $raw = certutil -hashfile $file $alg
      $digest = ($raw | Select-Object -Skip 1 -First 1).Trim()
      $out   = "$file." + $alg.ToLower()
      Set-Content -Path $out -Value $digest
      Write-Host "→ $alg for '$file' written to '$out'"
    }

    # 2. Create a detached ASCII-armored GPG signature (.asc)
    $sigOut = "$file.asc"
    Write-Host "Signing '$file' → '$sigOut'"
    gpg.exe --batch --yes --armor --detach-sign --output $sigOut $file
  }
