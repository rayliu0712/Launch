mkdir Pulled  *>$null

$state = "no devices/emulators found"

while($state.Contains("no devices/emulators found")) {
    $state = (adb pull /sdcard/Android/data/rl.puller/files/pull_list.txt).Trim()
    Write-Host -NoNewline "Disconnected"  #BUG
}

foreach ($line in cat pull_list.txt) {
    adb pull "$line" Pulled >> pull_result.txt
}

adb push pull_result.txt /sdcard/Android/data/rl.puller/files

rm pull_list.txt
rm pull_result.txt
