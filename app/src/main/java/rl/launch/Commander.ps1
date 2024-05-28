# https://github.com/rayliu0712/Launch
@"
start PowerShell 'Set-ExecutionPolicy RemoteSigned' -Verb RunAs
cmd /c 'ftype Microsoft.PowerShellScript.1="%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe" "%1"'
kill -Name explorer -Force; explorer
"@ > $null

$host.UI.RawUI.WindowTitle = "Commander"
mkdir Received *> $null
cd Received

while ($true) {
    $output = adb shell "getprop ro.product.model" 2>&1 | Out-String
    $output = $output.Trim()
    if ($output -notmatch "adb.exe: no devices/emulators found") {
        clear
        echo "`r [ $output Connected ]`n"
        break
    }

    Write-Host -NoNewline "`r Waiting For Device  /"
    sleep 0.25
    Write-Host -NoNewline "`b-"
    sleep 0.25
    Write-Host -NoNewline "`b\"
    sleep 0.25
    Write-Host -NoNewline "`b|"
    sleep 0.25
}
echo " [ Waiting For Launch ]"


while ($true) {
    adb shell "touch /sdcard/Android/data/rl.launch/files/Key"
    sleep 0.5
    $output = adb shell "[[ -f /sdcard/Android/data/rl.launch/files/Launch_List.txt ]] && echo E || echo N"
    $output = $output.Trim()
    if ($output -eq "E") {
        clear
        break
    }
}

$stopwatch = [System.Diagnostics.Stopwatch]::new()
$stopwatch.Start()

adb pull "/sdcard/Android/data/rl.launch/files/Launch_List.txt" > $null
[String]$LaunchList = cat Launch_List.txt -Raw -Encoding utf8
[String[]]$LaunchList = $LaunchList.Trim().Split("`n")
$size = $LaunchList.Length
rm Launch_List.txt

for ($i = 0; $i -lt $size; $i++) {
    [String]$launch = $LaunchList[$i]
    adb pull "$launch"
    $host.UI.RawUI.WindowTitle = "$( $i + 1 ) / $size"
}

adb pull "/sdcard/Android/data/rl.launch/files/Move_List.txt" > $null
[String]$MoveList = cat Move_List.txt -Raw -Encoding utf8
[String[]]$MoveList = $MoveList.Trim().Split("`n")
$size = $MoveList.Length
rm Move_List.txt

for ($i = 0; $i -lt $size; $i++) {
    [String[]]$move = $MoveList[$i].Split("`t")
    [String]$cooked = $move[0]
    [String]$raw = $move[1]
    mv "$cooked" "$raw"
}

$stopwatch.Stop()
adb shell "touch /sdcard/Android/data/rl.launch/files/LAUNCH_COMPLETED"

echo " Completed. Received $size files, uses $( $stopwatch.Elapsed.TotalSeconds )s `n"
for ($i = 10; $i -ge 0; $i--) {
    Write-Host -NoNewline "`r Exit After $i`s "
    sleep 1
}


#while ($true) {
#    $ClientKey = adb shell "cat /sdcard/Android/data/rl.launch/files/Client_Key.txt" 2> $null | Out-String
#    if ($ClientKey -eq "") {
#        $ClientKey = "0"
#    }
#    $ClientKey = [Int64]::Parse($ClientKey.Trim())
#
#    $now = adb shell "date +%s%3N"
#    $now = [Int64]::Parse($now.Trim())
#
#    $span = $now - $ClientKey
#
#    if ($span -le 500) {
#        clear
#        break
#    }
#}