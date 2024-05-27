# https://github.com/rayliu0712/Launch
@"
start PowerShell 'Set-ExecutionPolicy RemoteSigned' -Verb RunAs
cmd /c 'ftype Microsoft.PowerShellScript.1="%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe" "%1"'
kill -Name explorer -Force; explorer
"@ > $null

$host.UI.RawUI.WindowTitle = "Commander"
mkdir "Received" *> $null
cd "Received"

while ($true) {
    [String]$output = adb shell "getprop ro.product.model" 2>&1 | Out-String
    [String]$output = $output.Trim()
    if ($output -notmatch "adb.exe: no devices/emulators found") {
        Clear-Host
        Write-Host "`r [ $output Connected ]"
        Write-Host
        break
    }

    Write-Host -NoNewline "`r Waiting For Device  /"
    Start-Sleep -Milliseconds 250
    Write-Host -NoNewline "`b-"
    Start-Sleep -Milliseconds 250
    Write-Host -NoNewline "`b\"
    Start-Sleep -Milliseconds 250
    Write-Host -NoNewline "`b|"
    Start-Sleep -Milliseconds 250
}

Write-Host " [ Waiting For Launch ]"
while ($true) {
    $ClientKey = adb shell "cat /sdcard/Android/data/rl.launch/files/Client_Key.txt" 2> $null | Out-String
    if ($ClientKey -eq "") {
        $ClientKey = "0"
    }
    $ClientKey = [Int64]::Parse($ClientKey.Trim())

    $now = adb shell "date +%s%3N"
    $now = [Int64]::Parse($now.Trim())

    $span = $now - $ClientKey

    if ($span -le 500) {
        Clear-Host
        break
    }
}

adb pull "/sdcard/Android/data/rl.launch/files/Launch_List.txt" > $null
[String]$LaunchList = Get-Content "Launch_List.txt" -Raw
[String[]]$LaunchList = $LaunchList.Trim().Split("`n")
$size = $LaunchList.Length

adb pull "/sdcard/Android/data/rl.launch/files/Move_List.txt" > $null
[String]$MoveList = Get-Content "Move_List.txt" -Raw -Encoding UTF8
[String[]]$MoveList = $MoveList.Trim().Split("`n")

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$stopwatch.Start()
for ($i = 0; $i -lt $size; $i++) {
    [String]$launch = $LaunchList[$i]
    adb pull "$launch"

    [String[]]$move = $MoveList[$i].Split("`t")
    [String]$cooked = $move[0]
    [String]$raw = $move[1]
    Move-Item "$cooked" "$raw"

    $host.UI.RawUI.WindowTitle = "$( $i + 1 ) / $size"
}
$stopwatch.Stop()
adb shell "touch /sdcard/Android/data/rl.launch/files/LAUNCH_COMPLETED"
Remove-Item "Launch_List.txt"
Remove-Item "Move_List.txt"

Write-Host " Done. Received $size files, uses $( $stopwatch.Elapsed.TotalSeconds )s"
Write-Host
for ($i = 10; $i -ge 0; $i--) {
    Write-Host -NoNewline "`r Exit After $i`s "
    Start-Sleep 1
}