# https://github.com/rayliu0712/PR
@"
start PowerShell 'Set-ExecutionPolicy RemoteSigned' -Verb RunAs
cmd /c 'ftype Microsoft.PowerShellScript.1="%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe" "%1"'
kill -Name explorer -Force; explorer
"@ > $null

$host.UI.RawUI.WindowTitle = "Receiver"
mkdir Received *> $null

while ($true) {
    $output = adb shell "getprop ro.product.model" 2>&1 | Out-String
    $output = $output.Trim()
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
    $ClientKey = adb shell "cat /sdcard/Android/data/rl.pusher/files/Client_Key.txt" 2> $null
    if ($null -eq $ClientKey) {
        $ClientKey = "0"
    }
    $ClientKey = [Int64]::Parse($ClientKey.Trim())

    $now = adb shell "date +%s%3N"
    $now = [Int64]::Parse($now.Trim())

    if ($now - $ClientKey -le 500) {
        Clear-Host
        break
    }
}

$PullList = adb shell "cat /sdcard/Android/data/rl.pusher/files/Push_List.txt"
$size = $PullList.Length

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$stopwatch.Start()
for ($i = 0; $i -lt $size; $i++) {
    $Item = $PullList[$i].Trim()
    adb pull "$Item" Received
    $host.UI.RawUI.WindowTitle = "$( $i + 1 ) / $size"
}
$stopwatch.Stop()

Write-Host " Done. Received $size files, uses $( $stopwatch.Elapsed.TotalSeconds )s"
adb shell "touch /sdcard/Android/data/rl.pusher/files/PUSH_DONE"
Start-Sleep 10