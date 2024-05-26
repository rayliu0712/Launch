# https://github.com/rayliu0712/Launch
@"
start PowerShell 'Set-ExecutionPolicy RemoteSigned' -Verb RunAs
cmd /c 'ftype Microsoft.PowerShellScript.1="%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe" "%1"'
kill -Name explorer -Force; explorer
"@ > $null

$host.UI.RawUI.WindowTitle = "Receiver"
mkdir Received *> $null

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

Write-Host " [ Waiting For Push Command ]"
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

adb pull /sdcard/Android/data/rl.launch/files/Push_List.txt > $null
[String]$PullList = Get-Content Push_List.txt -Raw
[String[]]$PullList = $PullList.Trim().Split("`n")
$size = $PullList.Length

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$stopwatch.Start()
for ($i = 0; $i -lt $size; $i++) {
    [String]$Item = $PullList[$i].Trim()
    adb pull "$Item" Received
    $host.UI.RawUI.WindowTitle = "$( $i + 1 ) / $size"
}
adb shell "rm /sdcard/Android/data/rl.launch/files/Push_List.txt"
Remove-Item Push_List.txt
$stopwatch.Stop()

Write-Host " Done. Received $size files, uses $( $stopwatch.Elapsed.TotalSeconds )s"
adb shell "touch /sdcard/Android/data/rl.launch/files/PUSH_DONE"