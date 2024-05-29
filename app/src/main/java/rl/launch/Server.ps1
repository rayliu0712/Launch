# https://github.com/rayliu0712/Launch
@"
start PowerShell 'Set-ExecutionPolicy RemoteSigned' -Verb RunAs
cmd /c 'ftype Microsoft.PowerShellScript.1="%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe" "%1"'
"@ > $null

function deshell([String]$command) {
    adb shell run-as rl.launch "$command"
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
        $launchList = $output.Trim().Split("`n")
        $size = $launchList.Length
        break
    }
}

$stopwatch = [System.Diagnostics.Stopwatch]::new()
$stopwatch.Start()

for ($i = 0; $i -lt $size; $i++) {
    $launch = $launchList[$i]
    adb pull "$launch" Pulled
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

echo " Completed. Pulled $size files, uses $( $stopwatch.Elapsed.TotalSeconds )s `n"
for ($i = 10; $i -ge 0; $i--) {
    Write-Host -NoNewline "`r Exit After $i`s "
    sleep 1
}