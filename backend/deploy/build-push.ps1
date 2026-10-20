param(
    [Parameter(Mandatory=$true, HelpMessage="Image version, e.g. v1.0")]
    [string]$Version,
    [string]$ACR = "**:8800/anwei-remote-monitor",
    [string]$ACR_INFRA = "**:8800/library",
    [string]$JDK = "",
    [string]$BackendDir = "",
    [string]$FrontendDir = "",
    [string]$BackendBranch = "master",
    [string]$FrontendBranch = "master",
    [string]$DevDir = ""
)
$ErrorActionPreference = "Stop"

# ============ Dynamic Path Detection ============

if (-not $DevDir) {
    $DevDir = (Split-Path $PSScriptRoot -Parent)
}

# JDK
if (-not $JDK) {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
        $JDK = $env:JAVA_HOME
        Write-Host "[Auto] JDK from JAVA_HOME: $JDK" -ForegroundColor DarkYellow
    } else {
        $javaExe = (Get-Command java -ErrorAction SilentlyContinue).Source
        if ($javaExe -and $javaExe -notlike "*\system32\*" -and $javaExe -notlike "*\Windows\*") {
            $JDK = Split-Path (Split-Path $javaExe -Parent) -Parent
            Write-Host "[Auto] JDK from PATH: $JDK" -ForegroundColor DarkYellow
        } else {
            $searchPaths = @("C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java", "D:\Java")
            $found = $null
            foreach ($sp in $searchPaths) {
                if (Test-Path $sp) {
                    $found = Get-ChildItem $sp -Directory -ErrorAction SilentlyContinue |
                             Where-Object { $_.Name -match "21|jdk-21" -and (Test-Path "$($_.FullName)\bin\java.exe") } |
                             Select-Object -First 1
                    if ($found) { break }
                }
            }
            if ($found) {
                $JDK = $found.FullName
                Write-Host "[Auto] JDK from search: $JDK" -ForegroundColor DarkYellow
            } else {
                Write-Host "ERROR: Cannot auto-detect JDK 21, please specify via -JDK parameter" -ForegroundColor Red
                Write-Host "  Example: .\build-push.ps1 -Version v1.0 -JDK 'C:\Program Files\Eclipse Adoptium\jdk-21.0.3.9-hotspot'" -ForegroundColor Red
                exit 1
            }
        }
    }
}

if (-not $BackendDir) {
    $BackendDir = $DevDir
    Write-Host "[Auto] BackendDir = DevDir: $BackendDir" -ForegroundColor DarkYellow
}

if (-not $FrontendDir) {
    $frontendName = "anwei-fire-remote-monitor-vue"
    $devParent = Split-Path $DevDir -Parent
    $candidate = Join-Path $devParent $frontendName
    if (Test-Path $candidate) {
        $FrontendDir = $candidate
        Write-Host "[Auto] FrontendDir from sibling: $FrontendDir" -ForegroundColor DarkYellow
    } else {
        Write-Host "[Auto] FrontendDir not detected, will prompt later" -ForegroundColor DarkYellow
    }
}

Write-Host ""
Write-Host "--- Detected paths ---" -ForegroundColor Cyan
Write-Host "  DevDir:      $DevDir"
Write-Host "  JDK:         $JDK"
Write-Host "  BackendDir:  $BackendDir"
Write-Host "  FrontendDir: $FrontendDir"
Write-Host ""

# ============ Build Target Selection ============
Write-Host ""
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "  Please select build target" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host "  1. Build all  (frontend + backend)" -ForegroundColor White
Write-Host "  2. Backend only (anwei-remote-monitor)" -ForegroundColor White
Write-Host "  3. Frontend only (nginx-frontend)" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Yellow
$choice = Read-Host "Enter number [1-3]"

$buildAll       = ($choice -eq "1")
$buildBackend   = ($choice -eq "2")
$buildFrontend  = ($choice -eq "3")

if (-not ($buildAll -or $buildBackend -or $buildFrontend)) {
    Write-Host "ERROR: Invalid choice '$choice', please enter 1-3" -ForegroundColor Red
    exit 1
}

$needBackend = ($buildAll -or $buildBackend)
$needFrontend = ($buildAll -or $buildFrontend)

Write-Host ""
if ($needBackend)  { Write-Host "  [v] Backend build" -ForegroundColor Green }
else               { Write-Host "  [x] Backend build  (skip)" -ForegroundColor DarkGray }
if ($needFrontend) { Write-Host "  [v] Frontend build" -ForegroundColor Green }
else               { Write-Host "  [x] Frontend build  (skip)" -ForegroundColor DarkGray }
Write-Host ""

function Step($msg) {
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "  $msg" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
}

function Check($msg) {
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: $msg" -ForegroundColor Red
        exit 1
    }
}

Step "Environment Setup"
$env:JAVA_HOME = $JDK
$env:PATH = "$JDK\bin;" + $env:PATH
$env:DOCKER_BUILDKIT = 0
Write-Host "JDK: $JDK"
Write-Host "ACR: $ACR"
Write-Host "Version: $Version"
if ($needBackend)  { Write-Host "Build target: Backend" -ForegroundColor Yellow }
if ($needFrontend) { Write-Host "Build target: Frontend" -ForegroundColor Yellow }

Step "Docker login to Harbor"
$harborHost = ($ACR -split "/")[0]
docker login $harborHost -u admin -p AWHarbor
Check "docker login failed"

if ($needBackend) {
    Step "Check base image"
    $baseImg = docker images -q eclipse-temurin:21-jre
    if (-not $baseImg) {
        Write-Host "Base image not found locally, trying Harbor..." -ForegroundColor Yellow
        $harborBase = "$ACR_INFRA/eclipse-temurin:21-jre"
        docker pull $harborBase
        if ($LASTEXITCODE -eq 0) {
            docker tag $harborBase eclipse-temurin:21-jre
            Write-Host "Base image pulled from Harbor and tagged" -ForegroundColor Green
        } else {
            Write-Host "Harbor pull failed, trying Docker Hub mirror..." -ForegroundColor Yellow
            $mirrorBase = "hub.rat.dev/library/eclipse-temurin:21-jre"
            docker pull $mirrorBase
            Check "Pull base image failed (both Harbor and mirror)"
            docker tag $mirrorBase eclipse-temurin:21-jre
            Write-Host "Base image pulled from mirror and tagged" -ForegroundColor Green
        }
    } else {
        Write-Host "Base image already exists, skip"
    }
}

if ($needBackend) {
    Step "Check TDengine image"
    $tdImg = docker images -q "$ACR_INFRA/tdengine:3.0.7.1"
    if (-not $tdImg) {
        Write-Host "TDengine image not found locally, pulling from Docker Hub mirror..." -ForegroundColor Yellow
        $tdMirror = "hub.rat.dev/tdengine/tdengine:3.0.7.1"
        docker pull $tdMirror
        if ($LASTEXITCODE -eq 0) {
            docker tag $tdMirror "$ACR_INFRA/tdengine:3.0.7.1"
            docker push "$ACR_INFRA/tdengine:3.0.7.1"
            Write-Host "TDengine image pushed to Harbor" -ForegroundColor Green
        } else {
            Write-Host "WARNING: Failed to pull TDengine image, please push manually" -ForegroundColor Yellow
        }
    } else {
        Write-Host "TDengine image already exists, skip"
    }
}

if ($needBackend) {
    Step "Check Dockerfile"
    $Dockerfile = Join-Path $BackendDir "server\Dockerfile"
    if (-not (Test-Path $Dockerfile)) {
        Write-Host "ERROR: $Dockerfile not found" -ForegroundColor Red
        exit 1
    }
}

if ($needBackend) {
    Step "Update backend code"
    Set-Location $BackendDir
    git checkout $BackendBranch
    Check "git checkout backend $BackendBranch failed"
    git pull origin $BackendBranch
    Check "git pull backend failed"
}

if ($needBackend) {
    Step "Maven package anwei-remote-monitor"
    Set-Location $BackendDir
    mvn clean package -pl server -am -DskipTests
    Check "mvn package server failed"
}

if ($needBackend) {
    Step "Build and push anwei-remote-monitor"
    docker build -t "$ACR/anwei-remote-monitor:${Version}" "$BackendDir\server"
    Check "docker build anwei-remote-monitor failed"
    docker push "$ACR/anwei-remote-monitor:${Version}"
    Check "docker push anwei-remote-monitor failed"
}

if ($needFrontend) {
    Step "Update frontend code"
    Set-Location $FrontendDir
    git checkout $FrontendBranch
    Check "git checkout frontend $FrontendBranch failed"
    git pull origin $FrontendBranch
    Check "git pull frontend failed"
}

if ($needFrontend) {
    Step "Build and push nginx-frontend"

    $distDir = Join-Path $FrontendDir "dist-prod"

    # 0. Check if rebuild is needed
    Set-Location $FrontendDir
    $skipBuild = $false
    if (Test-Path $distDir) {
        Write-Host "dist-prod directory already exists." -ForegroundColor Yellow
        $rebuild = Read-Host "Rebuild frontend? (y/N)"
        if ($rebuild -notin @('y', 'Y', 'yes', 'YES')) {
            $skipBuild = $true
            Write-Host "Skipping npm build, using existing dist-prod" -ForegroundColor Green
        }
    }
    if (-not $skipBuild) {
        if (Test-Path $distDir) {
            Write-Host "Cleaning old dist..." -ForegroundColor Yellow
            Remove-Item -Recurse -Force $distDir
        }
        npm cache clean --force
        npm install --ignore-scripts --legacy-peer-deps --registry https://registry.npmjs.org/ --fetch-retries 5
        Check "npm install failed"
        npm run build:prod
        Check "npm run build failed"
    }

    if (-not (Test-Path $distDir)) {
        Write-Host "ERROR: dist directory not found: $distDir" -ForegroundColor Red
        exit 1
    }
    Write-Host "dist build completed" -ForegroundColor Green

    # 1. Start temporary nginx container
    $tempName = "nginx-build-temp"
    $ErrorActionPreference = 'SilentlyContinue'
    docker rm -f $tempName 2>&1 | Out-Null
    $ErrorActionPreference = 'Continue'
    # Ensure nginx:1.31.4-alpine is available locally
    $nginxLocal = docker images -q "nginx:1.31.4-alpine"
    if (-not $nginxLocal) {
        Write-Host "Pulling nginx:1.31.4-alpine from mirror..." -ForegroundColor Yellow
        $mirrors = @("hub.rat.dev/library/nginx:1.31.4-alpine", "docker.m.daocloud.io/library/nginx:1.31.4-alpine", "dockerpull.org/library/nginx:1.31.4-alpine")
        $pulled = $false
        foreach ($m in $mirrors) {
            Write-Host "Trying $m ..." -ForegroundColor Yellow
            docker pull $m
            if ($LASTEXITCODE -eq 0) {
                docker tag $m nginx:1.31.4-alpine
                $pulled = $true
                Write-Host "nginx:1.31.4-alpine pulled from $m" -ForegroundColor Green
                break
            }
        }
        if (-not $pulled) {
            Write-Host "ERROR: Failed to pull nginx:1.31.4-alpine from all mirrors" -ForegroundColor Red
            exit 1
        }
    }
    docker run -d --name $tempName nginx:1.31.4-alpine
    Check "docker run nginx-temp failed"
    docker exec $tempName sh -c "rm -rf /usr/share/nginx/html/*"

    # 2. Transfer files via tar pipe (reliable cross-platform binary transfer)
    Write-Host "Transferring dist files via tar..." -ForegroundColor Yellow
    $tarFile = Join-Path $distDir "..\dist-build.tar"
    tar -cf $tarFile -C $distDir .
    Check "tar create failed"
    docker cp $tarFile "${tempName}:/tmp/dist.tar"
    docker exec $tempName sh -c "tar -xf /tmp/dist.tar -C /usr/share/nginx/html/ && rm /tmp/dist.tar"
    Check "tar extract failed"
    Remove-Item $tarFile -Force -ErrorAction SilentlyContinue
    Write-Host "Dist files transferred successfully" -ForegroundColor Green

    # 2b. Transfer nginx.conf via tar (docker cp corrupts files on Docker Desktop Windows)
    $nginxConf = Join-Path $PSScriptRoot "config\frontend\nginx.conf"
    if (Test-Path $nginxConf) {
        $confDir = Split-Path $nginxConf
        $confName = Split-Path $nginxConf -Leaf
        $confTar = Join-Path $PSScriptRoot "nginx-conf.tar"
        tar -cf $confTar -C $confDir $confName
        docker cp $confTar "${tempName}:/tmp/conf.tar"
        docker exec $tempName sh -c "cd /etc/nginx/conf.d && tar -xf /tmp/conf.tar && mv $confName default.conf && rm /tmp/conf.tar"
        Check "nginx.conf transfer failed"
        Remove-Item $confTar -Force -ErrorAction SilentlyContinue
        Write-Host "nginx.conf transferred successfully" -ForegroundColor Green
    } else {
        Write-Host "WARNING: nginx.conf not found at $nginxConf" -ForegroundColor Yellow
    }

    # 3. Commit as image
    $imageTag = "$ACR/nginx-frontend:${Version}"
    docker commit $tempName $imageTag
    Check "docker commit nginx-frontend failed"

    # 4. Verify index.html
    $check = docker run --rm $imageTag sh -c "head -c 20 /usr/share/nginx/html/index.html"
    if ($check -notmatch "doctype|html|<") {
        Write-Host "ERROR: index.html appears corrupted!" -ForegroundColor Red
        docker rm -f $tempName 2>&1 | Out-Null
        exit 1
    }
    Write-Host "index.html verified OK" -ForegroundColor Green

    # 4b. Verify nginx.conf
    $confCheck = docker run --rm $imageTag sh -c "head -c 10 /etc/nginx/conf.d/default.conf"
    if ($confCheck -notmatch "server|listen|location") {
        Write-Host "ERROR: nginx.conf appears corrupted!" -ForegroundColor Red
        docker rm -f $tempName 2>&1 | Out-Null
        exit 1
    }
    Write-Host "nginx.conf verified OK" -ForegroundColor Green

    # 5. Cleanup temp container
    $ErrorActionPreference = 'SilentlyContinue'
    docker rm -f $tempName 2>&1 | Out-Null
    $ErrorActionPreference = 'Continue'

    # 6. Push
    docker push $imageTag
    Check "docker push nginx-frontend failed"
}

Step "Verify registry images"
Invoke-RestMethod -Uri "http://$harborHost/api/v2.0/repositories" -Headers @{Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:AWHarbor"))} | ConvertTo-Json

Step "ALL DONE"
if ($needBackend)  { Write-Host "  anwei-remote-monitor: $ACR/anwei-remote-monitor:$Version" -ForegroundColor Green }
if ($needFrontend) { Write-Host "  nginx-frontend:       $ACR/nginx-frontend:$Version" -ForegroundColor Green }
Write-Host ""
