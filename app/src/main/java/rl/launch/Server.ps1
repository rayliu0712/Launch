# Encoding : UTF-8 with BOM
# https://github.com/rayliu0712/Launch

# cmd (run as admin)
# ftype Microsoft.PowerShellScript.1="%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe" -File "%1" -ExecutionPolicy RemoteSigned

$OutputEncoding = [console]::InputEncoding = [console]::OutputEncoding = [Text.UTF8Encoding]::UTF8

$device = ""
$isFirst = $true
while ($true) {
    $output = adb $device shell exit 2>&1 | Out-String
    if ("" -eq $output) {
        break
    }

    if ($isFirst) {
        echo "----- 以參數指定裝置 -----`n"
        echo " [-d]                實體裝置`n"
        echo " [-e]                模擬器`n"
        echo " [-t<transport_id>]  範例：`"-t1`"`n"
        echo " [-s<serial>]        範例：`"-s90f73505`"`n"
        echo "----------------------------------------"
        $isFirst = $false
    }

    echo "`n [ ERROR ] $( $output.Trim().Split("`n")[0] )`n"
    $adbDevices = adb devices -l
    $adbDevices = $adbDevices.Trim().Split("`n")
    for($i = 1; $i -lt $adbDevices.Length - 1; $i++) {
        $line = [Regex]::Replace($adbDevices[$i], " +", " ")
        echo " [ DEVICE ] $line"
    }
    Write-Host -NoNewline "`n > "
    $device = Read-Host
}
echo ""

function deshell([String]$command) {
    adb $device shell run-as rl.launch "$command"
}

$launchList, $moveList, $size
while ($true) {
    $output = deshell "cat ./files/launch.txt" 2> $null
    if ($null -eq $output) {
        Write-Host -NoNewline "`r Waiting For Launch \"
        sleep -Milliseconds 250
        Write-Host -NoNewline "`b|"
        sleep -Milliseconds 250
        Write-Host -NoNewline "`b/"
        sleep -Milliseconds 250
        Write-Host -NoNewline "`b-"
        sleep -Milliseconds 250
    }
    else {
        deshell "touch ./files/key_a"
        mkdir Pulled *> $null
        cd Pulled
        $launchList = $output.Trim().Split("`n")
        $size = $launchList.Length
        break
    }
}

$stopwatch = [System.Diagnostics.Stopwatch]::new()
$stopwatch.Start()

for ($i = 0; $i -lt $size; $i++) {
    $launch = $launchList[$i]
    adb $device pull "$launch"
    $host.UI.RawUI.WindowTitle = "$( $i + 1 ) / $size"
}

$moveList = deshell "cat ./files/move.txt" 2> $null
if ($null -ne $moveList) {
    $moveList = $moveList.Trim().Split("`n")
    $size = $moveList.Length
    for($i = 0; $i -lt $size; $i++) {
        $move = $moveList[$i].Split("`t")
        $cooked = $move[0]
        $raw = $move[1]
        mv "$cooked" "$raw"
    }
}

$stopwatch.Stop()
deshell "touch ./files/key_b"

cd ..
echo " Completed. Pulled $size files, uses $($stopwatch.Elapsed.TotalSeconds.ToString("F2") )s `n"
for ($i = 10; $i -ge 0; $i--) {
    Write-Host -NoNewline "`r Exit After $i`s "
    sleep 1
}