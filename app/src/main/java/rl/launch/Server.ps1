# https://github.com/rayliu0712/Launch

$OutputEncoding = [console]::InputEncoding = [console]::OutputEncoding = [Text.UTF8Encoding]::UTF8

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
    adb pull "$launch"
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
for ($i = 666; $i -ge 0; $i--) {
    Write-Host -NoNewline "`r Exit After $i`s "
    sleep 1
}